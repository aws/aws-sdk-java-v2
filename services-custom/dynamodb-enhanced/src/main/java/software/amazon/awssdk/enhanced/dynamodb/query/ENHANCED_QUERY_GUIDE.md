# Enhanced Query Playbook

A comprehensive reference for the DynamoDB Enhanced Client's **Enhanced Query** API: how to build queries, how execution
actually works, class-by-class scope, condition trees, output examples, benchmark workflows, and diagrams.

---

## Table of contents

1. [Setup](#1-setup)
2. [Core concepts](#2-core-concepts)
3. [Class reference](#3-class-reference)
4. [Single-table query](#4-single-table-query)
5. [Full-table scan](#5-full-table-scan)
6. [Filtering](#6-filtering)
7. [Nested attribute filtering](#7-nested-attribute-filtering)
8. [Joins](#8-joins)
9. [Pre-join filters](#9-pre-join-filters)
10. [ExecutionMode deep-dive](#10-executionmode-deep-dive)
11. [Aggregations](#11-aggregations)
12. [Join plus aggregation](#12-join-plus-aggregation)
13. [Ordering](#13-ordering)
14. [Projection](#14-projection)
15. [Limit](#15-limit)
16. [Latency report](#16-latency-report)
17. [Async API](#17-async-api)
18. [Condition system deep-dive](#18-condition-system-deep-dive)
19. [Execution flow diagrams](#19-execution-flow-diagrams)
20. [Build validation](#20-build-validation)
21. [Performance tips](#21-performance-tips)
22. [Complete example](#22-complete-example)
23. [Benchmark guide](#23-benchmark-guide)

---

## 1. Setup

```xml

<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb-enhanced</artifactId>
</dependency>
```

```java
DynamoDbClient lowLevel = DynamoDbClient.create();
DynamoDbEnhancedClient client = DynamoDbEnhancedClient.builder()
                                                      .dynamoDbClient(lowLevel)
                                                      .build();

DynamoDbTable<Customer> customers = client.table("Customers", CUSTOMER_SCHEMA);
DynamoDbTable<Order> orders = client.table("Orders", ORDER_SCHEMA);
```

---

## 2. Core concepts

| Concept                       | Description                        |
|-------------------------------|------------------------------------|
| `QueryExpressionBuilder`      | Fluent API for query specification |
| `QueryExpressionSpec`         | Immutable output of the builder    |
| `enhancedQuery(spec)`         | Sync execution API                 |
| `enhancedQuery(spec)` async   | Publisher-based async API          |
| `keyCondition`                | Only server-side filter pushdown   |
| `where`                       | Final in-memory filter             |
| `filterBase` / `filterJoined` | In-memory pre-join filters         |
| `ExecutionMode`               | Strict key only or scan allowed    |

Important: only `keyCondition` is pushed to DynamoDB query planning. Join logic, non-key filtering, grouping, ordering,
and limit are in-memory engine operations.

---

## 3. Class reference

### Public query model classes

| Class                        | Scope                | What it owns                                                                  |
|------------------------------|----------------------|-------------------------------------------------------------------------------|
| `QueryExpressionBuilder`     | Builder API          | Fluent query construction and validation                                      |
| `QueryExpressionSpec`        | Immutable spec       | Full query plan data (tables, join, conditions, grouping, order, mode, limit) |
| `AggregateSpec`              | Aggregation config   | Function, input attribute, output alias                                       |
| `OrderBySpec`                | Sort config          | Sort key and direction, attribute or aggregate                                |
| `EnhancedQueryRow`           | Result row           | `itemsByAlias` and `aggregates`                                               |
| `EnhancedQueryResult`        | Result stream (sync) | Iterable wrapper for rows                                                     |
| `EnhancedQueryLatencyReport` | Telemetry model      | base/joined/in-memory/total timing values                                     |

### Condition and evaluation classes

| Class                | Scope                                | Notes                                                                        |
|----------------------|--------------------------------------|------------------------------------------------------------------------------|
| `Condition`          | Filter AST factories and combinators | `eq`, `gt`, `between`, `contains`, `beginsWith`, `and`, `or`, `not`, `group` |
| `ConditionEvaluator` | Recursive condition execution        | Applies conditions over item maps, including dot-path lookups                |

### Enums

| Enum                  | Values                              | Purpose               |
|-----------------------|-------------------------------------|-----------------------|
| `ExecutionMode`       | `STRICT_KEY_ONLY`, `ALLOW_SCAN`     | Scan fallback policy  |
| `JoinType`            | `INNER`, `LEFT`, `RIGHT`, `FULL`    | Join output semantics |
| `AggregationFunction` | `COUNT`, `SUM`, `AVG`, `MIN`, `MAX` | Aggregate operator    |
| `SortDirection`       | `ASC`, `DESC`                       | Ordering direction    |

### Internal engine classes (implementation detail)

| Class                                                                  | Scope                                        |
|------------------------------------------------------------------------|----------------------------------------------|
| `QueryExpressionEngine`                                                | Sync execution interface                     |
| `QueryExpressionAsyncEngine`                                           | Async execution interface                    |
| `DefaultQueryExpressionEngine`                                         | Sync execution implementation                |
| `DefaultQueryExpressionAsyncEngine`                                    | Async execution implementation               |
| `JoinedTableObjectMapSyncFetcher` / `JoinedTableObjectMapAsyncFetcher` | Joined-side lookup strategy (PK/GSI/scan)    |
| `QueryEngineSupport`                                                   | Shared aggregation/sort/group helpers        |
| `JoinRowAliases`                                                       | Alias mapping for base/joined item maps      |
| `AttributeValueConversion`                                             | DynamoDB `AttributeValue` conversion helpers |

---

## 4. Single-table query

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .build();

for(
EnhancedQueryRow row :client.

enhancedQuery(spec)){
    System.out.

println(row.getItem("base").

get("customerId"));
    }
```

**Output (example):**

```text
c1
```

Execution summary: base table uses DynamoDB `Query`; rows are emitted with alias `base`.

---

## 5. Full-table scan

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .executionMode(ExecutionMode.ALLOW_SCAN)
                                                 .where(Condition.eq("region", "EU"))
                                                 .build();
```

**Output (example):**

```text
rows=500
first.customerId=c1
last.customerId=c999
```

On seeded test data, EU is odd customer IDs, so there are 500 matches out of 1000.

---

## 6. Filtering

All `Condition` filters run in memory:

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(orders)
                                                 .executionMode(ExecutionMode.ALLOW_SCAN)
                                                 .where(
                                                     Condition.eq("customerId", "c1")
                                                              .and(Condition.gte("amount", 50))
                                                              .and(Condition.beginsWith("orderId", "c1-o"))
                                                 )
                                                 .build();
```

**Output (example):**

```text
rows=997
first.orderId=c1-o4
first.amount=50
```

---

## 7. Nested attribute filtering

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .executionMode(ExecutionMode.ALLOW_SCAN)
                                                 .where(Condition.eq("address.city", "Seattle"))
                                                 .build();
```

**Output (example):**

```text
rows=2
customerIds=[c1, c3]
```

Dot-path (`address.city`) resolution traverses nested maps. Missing path components evaluate as non-match.

---

## 8. Joins

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .join(orders, JoinType.INNER, "customerId", "customerId")
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .build();
```

**Output (example):**

```text
rows=1000
sample: base.customerId=c1, joined.orderId=c1-o1
```

### Join type behavior

| Join type | Output semantics                              |
|-----------|-----------------------------------------------|
| `INNER`   | Matched pairs only                            |
| `LEFT`    | All base rows, empty joined map when no match |
| `RIGHT`   | All joined rows, empty base map when no match |
| `FULL`    | Union of LEFT and RIGHT behavior              |

**Output examples by type:**

```text
INNER  c1 -> rows=1000
LEFT   c1 -> rows=1000
RIGHT  c1 -> rows=1000
FULL   c1 -> rows=1000
```

---

## 9. Pre-join filters

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .join(orders, JoinType.INNER, "customerId", "customerId")
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .filterBase(Condition.eq("region", "EU"))
                                                 .filterJoined(Condition.gte("amount", 50))
                                                 .where(Condition.beginsWith("orderId", "c1-o"))
                                                 .build();
```

Execution order:

1. `filterBase`
2. `filterJoined`
3. `where`

---

## 10. ExecutionMode deep-dive

`ExecutionMode` controls scan fallback, not validation.

### 10.1 STRICT_KEY_ONLY plus key condition

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .build();
```

**Output:** `rows=1` (uses `Query`)

### 10.2 STRICT_KEY_ONLY without key condition

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                 .where(Condition.eq("region", "EU"))
                                                 .build();
```

**Output:** `rows=0` (no scan fallback)

### 10.3 ALLOW_SCAN without key condition

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .executionMode(ExecutionMode.ALLOW_SCAN)
                                                 .where(Condition.eq("region", "EU"))
                                                 .build();
```

**Output:** `rows=500` (scan fallback allowed)

### 10.4 ALLOW_SCAN with key condition

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .executionMode(ExecutionMode.ALLOW_SCAN)
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .build();
```

**Output:** `rows=1` (still uses `Query`; scan is fallback only)

### 10.5 Join with STRICT_KEY_ONLY and non-key joined path

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .join(orders, JoinType.LEFT, "region", "status")
                                                 .executionMode(ExecutionMode.STRICT_KEY_ONLY)
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .build();
```

**Output:** base rows preserved by LEFT semantics; joined side can be empty where no key/index path exists.

### 10.6 Join with ALLOW_SCAN and non-key joined path

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .join(orders, JoinType.LEFT, "region", "status")
                                                 .executionMode(ExecutionMode.ALLOW_SCAN)
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .build();
```

**Output:** joined lookups may use scan fallback.

---

## 11. Aggregations

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(orders)
                                                 .executionMode(ExecutionMode.ALLOW_SCAN)
                                                 .groupBy("customerId")
                                                 .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                 .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                 .aggregate(AggregationFunction.AVG, "amount", "avgAmount")
                                                 .build();
```

**Output (example for `customerId=c1`):**

```text
customerId=c1
orderCount=1000
totalAmount=510500
avgAmount=510.5
```

### Aggregation function types

| Function | Meaning         | Output type                          |
|----------|-----------------|--------------------------------------|
| `COUNT`  | Count values    | Long                                 |
| `SUM`    | Numeric sum     | BigDecimal-compatible numeric output |
| `AVG`    | Numeric average | BigDecimal-compatible numeric output |
| `MIN`    | Minimum         | Input-compatible comparable          |
| `MAX`    | Maximum         | Input-compatible comparable          |

---

## 12. Join plus aggregation

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .join(orders, JoinType.INNER, "customerId", "customerId")
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .filterJoined(Condition.gte("amount", 50))
                                                 .groupBy("customerId")
                                                 .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                 .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                 .build();
```

**Output (example):**

```text
customerId=c1
orderCount=997
totalAmount=510440
```

---

## 13. Ordering

```java
.orderBy("name",SortDirection.ASC)
.

orderByAggregate("totalAmount",SortDirection.DESC)
```

Ordering is in-memory and runs after filtering/aggregation.

---

## 14. Projection

```java
.project("customerId","name","address.city")
```

Projection is pushed to DynamoDB (`ProjectionExpression`) but does not replace in-memory filtering rules. If a filter
needs an attribute, ensure it is projected.

---

## 15. Limit

```java
.limit(100)
```

Limit is applied after ordering/aggregation and caps final emitted rows.

---

## 16. Latency report

```java
client.enhancedQuery(spec, report ->{
    System.out.

println("Base query: "+report.baseQueryMs() +" ms");
    System.out.

println("Joined lookups: "+report.joinedLookupsMs() +" ms");
    System.out.

println("In-memory: "+report.inMemoryProcessingMs() +" ms");
    System.out.

println("Total: "+report.totalMs() +" ms");
    });
```

---

## 17. Async API

```java
DynamoDbEnhancedAsyncClient asyncClient = DynamoDbEnhancedAsyncClient.builder()
                                                                     .dynamoDbClient(DynamoDbAsyncClient.create())
                                                                     .build();

QueryExpressionSpec spec = QueryExpressionBuilder.from(asyncCustomers)
                                                 .join(asyncOrders, JoinType.INNER, "customerId", "customerId")
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .build();

asyncClient.

enhancedQuery(spec).

subscribe(row ->
    System.out.

println(row.getItem("base").

get("customerId")));
```

---

## 18. Condition system deep-dive

Conditions are represented as a tree and evaluated recursively against item maps.

### 18.1 Leaf condition node types

| Node type  | Factory                        | Meaning               |
|------------|--------------------------------|-----------------------|
| Comparator | `eq`, `gt`, `gte`, `lt`, `lte` | Binary comparison     |
| Between    | `between(attr, from, to)`      | Inclusive range check |
| Function   | `contains`, `beginsWith`       | String/set helpers    |

### 18.2 Composite node types

| Node type | Created by             | Meaning             |
|-----------|------------------------|---------------------|
| `And`     | `.and(...)`            | Both children true  |
| `Or`      | `.or(...)`             | Either child true   |
| `Not`     | `.not()`               | Logical negation    |
| `Group`   | `Condition.group(...)` | Precedence grouping |

### 18.3 Condition tree example

```
             Or
            /  \
          And   Not
         /  \     \
eq(region, EU)  gt(age, 25)  eq(status, INACTIVE)
```

Expression that builds this tree:

```java
Condition.eq("region", "EU")
    .and(Condition.gt("age", 25))
    .or(Condition.eq("status", "INACTIVE").not());
```

Evaluation order: `And` evaluates left then right (short-circuits on false). `Or` evaluates left
then right (short-circuits on true). `Not` inverts its child. `Group` is transparent -- it only
affects precedence during tree construction, not evaluation.

### 18.4 Operator semantics

| Operator     | Rule                                            |
|--------------|-------------------------------------------------|
| `eq`         | true if comparison returns zero                 |
| `gt` / `gte` | standard numeric/comparable ordering            |
| `lt` / `lte` | standard numeric/comparable ordering            |
| `between`    | inclusive `[from, to]`                          |
| `contains`   | substring for `String`, element match for `Set` |
| `beginsWith` | prefix check for `String`                       |

Comparison behavior:

- Number values are compared as `BigDecimal`.
- Comparable values use `compareTo` when type-compatible.
- If comparable cast fails, fallback uses `toString()` lexical comparison.
- Null handling is deterministic (`null` compared with non-null is ordered lower).

### 18.5 Dot-path resolution

Paths like `address.city` or `metadata.score.value` are resolved by map traversal at each path segment. If any segment
is missing or non-map, the condition does not match.

### 18.6 Combined map behavior for join `where`

When evaluating `where` in join flows, base and joined maps are seen as a combined view. Base alias values are preferred
when present; joined map values fill missing attributes.

### 18.7 Input/output condition examples

Input item:

```text
{name=Alice, age=30, region=EU, address={city=Seattle}}
```

| Condition                                                                                                   | Result |
|-------------------------------------------------------------------------------------------------------------|--------|
| `Condition.eq("name", "Alice")`                                                                             | true   |
| `Condition.gt("age", 25)`                                                                                   | true   |
| `Condition.between("age", 20, 29)`                                                                          | false  |
| `Condition.contains("name", "li")`                                                                          | true   |
| `Condition.beginsWith("name", "Al")`                                                                        | true   |
| `Condition.eq("address.city", "Seattle")`                                                                   | true   |
| `Condition.eq("region", "EU").and(Condition.gt("age", 25))`                                                 | true   |
| `Condition.eq("region", "EU").or(Condition.eq("region", "US"))`                                             | true   |
| `Condition.eq("region", "NA").not()`                                                                        | true   |
| `Condition.group(Condition.eq("region","EU").or(Condition.eq("region","US"))).and(Condition.gt("age", 40))` | false  |

---

## 19. Execution flow diagrams

### 19.1 Single-table flow

```
                    ┌─────────────────────┐
                    │  keyCondition set?   │
                    └──────────┬──────────┘
                     yes ┌─────┴─────┐ no
                         │           │
                    ┌────▼───┐  ┌────▼──────────────┐
                    │ Query  │  │  ExecutionMode?    │
                    └────┬───┘  └────┬──────────┬───┘
                         │    ALLOW_SCAN    STRICT_KEY_ONLY
                         │       │              │
                         │  ┌────▼───┐   ┌──────▼──────┐
                         │  │  Scan  │   │ Empty result │
                         │  └────┬───┘   └─────────────┘
                         │       │
                    ┌────▼───────▼───┐
                    │ Apply where()  │
                    └───────┬───────┘
                    ┌───────▼───────┐
                    │ groupBy + agg │
                    └───────┬───────┘
                    ┌───────▼───────┐
                    │   orderBy     │
                    └───────┬───────┘
                    ┌───────▼───────┐
                    │    limit      │
                    └───────┬───────┘
                    ┌───────▼───────┐
                    │  Return rows  │
                    └───────────────┘
```

### 19.2 Join flow

```
  ┌──────────────────┐
  │ Fetch base rows  │
  └────────┬─────────┘
  ┌────────▼─────────┐
  │ Apply filterBase │
  └────────┬─────────┘
  ┌────────▼──────────────────┐
  │ Collect distinct join keys│
  └────────┬──────────────────┘
  ┌────────▼─────────────┐
  │ Lookup joined rows   │  (see 19.3 for strategy)
  └────────┬─────────────┘
  ┌────────▼───────────────┐
  │ Apply filterJoined     │
  └────────┬───────────────┘
  ┌────────▼───────────────┐
  │ Combine by JoinType    │
  └────────┬───────────────┘
  ┌────────▼───────────────┐
  │ Apply where on merged  │
  └────────┬───────────────┘
  ┌────────▼───────────────┐
  │ groupBy + aggregate    │
  └────────┬───────────────┘
  ┌────────▼───────────────┐
  │ orderBy + limit        │
  └────────┬───────────────┘
  ┌────────▼───────────────┐
  │ Return rows            │
  └────────────────────────┘
```

### 19.3 Joined lookup strategy

```
  ┌───────────────────────────────┐
  │ Join attr is joined table PK? │
  └──────────┬────────────────┬───┘
           yes               no
             │                │
   ┌─────────▼──────┐  ┌─────▼───────────────┐
   │  Query by PK   │  │ Join attr has GSI?   │
   └────────────────┘  └─────┬───────────┬────┘
                           yes           no
                             │            │
                   ┌─────────▼──────┐  ┌──▼──────────────┐
                   │ Query by GSI   │  │ ExecutionMode?   │
                   └────────────────┘  └──┬──────────┬───┘
                                   ALLOW_SCAN   STRICT_KEY_ONLY
                                       │              │
                                ┌──────▼──────┐ ┌─────▼──────────┐
                                │ Scan joined │ │ No rows (empty)│
                                └─────────────┘ └────────────────┘
```

### 19.4 Async flow

```
  ┌────────────────────────┐
  │ Async execute          │
  └───────────┬────────────┘
  ┌───────────▼────────────┐
  │ Fetch base publisher   │
  └───────────┬────────────┘
  ┌───────────▼────────────┐
  │ Simple base-only path? │
  └─────┬──────────────┬───┘
       yes             no
        │               │
  ┌─────▼───────────┐  ┌▼──────────────────────┐
  │ Stream rows via │  │ Drain publisher to list│
  │ publisher       │  └───────────┬────────────┘
  └─────────────────┘  ┌───────────▼────────────┐
                       │ Join + aggregation work │
                       └───────────┬─────────────┘
                       ┌───────────▼─────────────┐
                       │ Publish result rows     │
                       └─────────────────────────┘
```

---

## 20. Build validation

`build()` rejects invalid query shapes with `IllegalStateException`.

| Invalid shape                                | Typical validation message                                    |
|----------------------------------------------|---------------------------------------------------------------|
| `groupBy()` without `aggregate()`            | `groupBy() requires at least one aggregate()`                 |
| `filterBase()` without join                  | `filterBase() is only applicable when a join is configured`   |
| `filterJoined()` without join                | `filterJoined() is only applicable when a join is configured` |
| join table without join type (or vice-versa) | join configuration missing counterpart                        |
| missing join key names                       | left/right join key missing                                   |

---

## 21. Performance tips

1. Use `keyCondition` whenever possible.
2. Prefer `filterBase` and `filterJoined` to reduce join cardinality early.
3. Design joined-table access so join key maps to PK/GSI.
4. Treat `ALLOW_SCAN` as opt-in for known workloads.
5. Use `project(...)` to reduce transfer volume.
6. Use `limit(...)` and ordering for top-N style results.
7. Monitor timing with `EnhancedQueryLatencyReport`.

---

## 22. Complete example

```java
QueryExpressionSpec spec = QueryExpressionBuilder.from(customers)
                                                 .join(orders, JoinType.INNER, "customerId", "customerId")
                                                 .keyCondition(QueryConditional.keyEqualTo(k -> k.partitionValue("c1")))
                                                 .filterBase(Condition.eq("region", "EU"))
                                                 .filterJoined(Condition.gte("amount", 50))
                                                 .groupBy("customerId")
                                                 .aggregate(AggregationFunction.SUM, "amount", "totalAmount")
                                                 .aggregate(AggregationFunction.COUNT, "orderId", "orderCount")
                                                 .aggregate(AggregationFunction.AVG, "amount", "avgOrder")
                                                 .orderByAggregate("totalAmount", SortDirection.DESC)
                                                 .limit(10)
                                                 .build();

EnhancedQueryResult result = client.enhancedQuery(spec, report -> {
    System.out.printf("Timing: base=%dms joined=%dms memory=%dms total=%dms%n",
                      report.baseQueryMs(), report.joinedLookupsMs(),
                      report.inMemoryProcessingMs(), report.totalMs());
});

for(
EnhancedQueryRow row :result){
    System.out.

printf("%s total=%s count=%s avg=%s%n",
       row.getItem("base").

get("customerId"),
        row.

getAggregate("totalAmount"),
        row.

getAggregate("orderCount"),
        row.

getAggregate("avgOrder"));
    }
```

**Output (example):**

```text
Timing: base=xxms joined=yyms memory=zzms total=ttms
c1 total=510440 count=997 avg=512.979...
```

---

## 23. Benchmark guide

### 23.1 What is benchmarked

The benchmark runner covers these scenarios:

- `baseOnly_keyCondition`
- `joinInner_c1`
- `aggregation_groupByCount_c1`
- `aggregation_groupBySum_c1`
- `joinLeft_c1_limit50`

### 23.2 Local benchmark script

```bash
./services-custom/dynamodb-enhanced/run-enhanced-query-benchmark-local.sh
```

Optional CSV target:

```bash
BENCHMARK_OUTPUT_FILE=benchmark_local.csv ./services-custom/dynamodb-enhanced/run-enhanced-query-benchmark-local.sh
```

What it does:

1. Starts local/in-process benchmark environment.
2. Creates and seeds dataset (1000 customers x 1000 orders) when required.
3. Executes warmup iterations.
4. Executes measured iterations.
5. Prints formatted benchmark table.
6. Appends CSV lines if `BENCHMARK_OUTPUT_FILE` is configured.

### 23.3 Tests plus timing script

```bash
./services-custom/dynamodb-enhanced/run-enhanced-query-tests-and-print-timing.sh
```

Output includes:

- Functional test pass/fail summary.
- Timing lines from test execution.
- Useful quick regression signal for query behavior and performance.

### 23.4 Console output format

Benchmark output prints a formatted table with:

- `SCENARIO`
- `DDB OPERATION`
- `DESCRIPTION`
- `AVG(ms)`
- `P50(ms)`
- `P95(ms)`
- `ROWS`

### 23.5 CSV output format

Header:

```text
scenario,description,ddbOperation,avgMs,p50Ms,p95Ms,rows,region,iterations
```

Sample row:

```text
joinInner_c1,"INNER join for c1","Query base + Query joined",312.40,306,350,1000,local,5
```

Field meanings:

- `scenario`: scenario id
- `description`: human-readable description
- `ddbOperation`: underlying DynamoDB access pattern
- `avgMs`, `p50Ms`, `p95Ms`: latency distribution metrics
- `rows`: output row count
- `region`: `local` or AWS region name
- `iterations`: measured run count

### 23.6 Environment variables

| Variable                | Default           | Meaning                            |
|-------------------------|-------------------|------------------------------------|
| `BENCHMARK_ITERATIONS`  | `5`               | Measured iterations per scenario   |
| `BENCHMARK_WARMUP`      | `2`               | Warmup iterations before measuring |
| `BENCHMARK_OUTPUT_FILE` | none              | Optional CSV output path           |
| `AWS_REGION`            | SDK default       | Region for non-local run           |
| `CUSTOMERS_TABLE`       | `customers_large` | Customers table name               |
| `ORDERS_TABLE`          | `orders_large`    | Orders table name                  |
| `CREATE_AND_SEED`       | unset             | Create and seed dataset when true  |

### 23.7 EC2 benchmark flow

For real DynamoDB latency numbers:

1. Create target tables in AWS.
2. Seed dataset.
3. Run benchmark from EC2 in same region.
4. Capture console and CSV outputs.
5. Record region, table names, dataset size, and iteration count with results.

