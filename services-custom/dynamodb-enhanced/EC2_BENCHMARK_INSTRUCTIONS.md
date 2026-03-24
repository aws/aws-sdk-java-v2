# Enhanced Query Benchmark Instructions

This document covers two ways to run the Enhanced Query (join and aggregation) benchmarks: **local DynamoDB** (no AWS required) and **EC2 + real DynamoDB** (production-like numbers).

---

## Local DynamoDB benchmark (recommended for design doc)

No AWS account or credentials required. Uses in-process DynamoDB Local, creates and seeds 1000 customers × 1000 orders, then runs five scenarios and prints latency stats (avgMs, p50Ms, p95Ms, rows).

### How to run

From the **repository root**:

```bash
./services-custom/dynamodb-enhanced/run-enhanced-query-benchmark-local.sh
```

The script sets `USE_LOCAL_DYNAMODB=true` and invokes the benchmark runner. Results are printed to stdout. To save CSV results:

```bash
BENCHMARK_OUTPUT_FILE=benchmark_local.csv ./services-custom/dynamodb-enhanced/run-enhanced-query-benchmark-local.sh
```

Optional env vars (set before running the script): `BENCHMARK_ITERATIONS` (default 5), `BENCHMARK_WARMUP` (default 2), `BENCHMARK_OUTPUT_FILE` (optional path for CSV).

### Results

Example output (environment and scenario lines):

```
Using in-process DynamoDB Local.
Creating tables and seeding data (1000 customers x 1000 orders)...
...
Environment: DynamoDB Local (in-process) CUSTOMERS_TABLE=customers_large ORDERS_TABLE=orders_large
Warmup=2 Iterations=5
---
baseOnly_keyCondition: avgMs=... p50Ms=... p95Ms=... rows=1
joinInner_c1: avgMs=... p50Ms=... p95Ms=... rows=1000
...
```

Use this output (or the CSV file) in the design document. See [COMPLEX_QUERIES_DESIGN.md](COMPLEX_QUERIES_DESIGN.md#benchmarking) for where to reference the benchmark and link to results.

---

## EC2 + Real DynamoDB benchmark

Use this for production-like latency (e.g. external claims or SLA discussions). Requires AWS account and EC2.

### Prerequisites

- AWS account with permissions to create DynamoDB tables and launch EC2 instances (or use existing EC2).
- AWS CLI configured (`aws configure`) or IAM role for EC2 with DynamoDB access.
- Java 8+ and Maven 3.6+ (on your machine for building; on EC2 for running).

---

## Step 1: Create DynamoDB tables (AWS CLI)

Create two tables in your chosen region (e.g. `us-east-1`) with the same schema as the functional tests.

**Customers table** (partition key: `customerId` String):

```bash
export AWS_REGION=us-east-1
export CUSTOMERS_TABLE=customers_large
export ORDERS_TABLE=orders_large

aws dynamodb create-table \
  --table-name $CUSTOMERS_TABLE \
  --attribute-definitions AttributeName=customerId,AttributeType=S \
  --key-schema AttributeName=customerId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region $AWS_REGION
```

**Orders table** (partition key: `customerId` String, sort key: `orderId` String):

```bash
aws dynamodb create-table \
  --table-name $ORDERS_TABLE \
  --attribute-definitions \
    AttributeName=customerId,AttributeType=S \
    AttributeName=orderId,AttributeType=S \
  --key-schema \
    AttributeName=customerId,KeyType=HASH \
    AttributeName=orderId,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region $AWS_REGION
```

Wait until both tables are `ACTIVE`:

```bash
aws dynamodb describe-table --table-name $CUSTOMERS_TABLE --query 'Table.TableStatus'
aws dynamodb describe-table --table-name $ORDERS_TABLE --query 'Table.TableStatus'
```

---

## Step 2: Seed the tables (optional: use benchmark runner with CREATE_AND_SEED)

You can either seed from your **local machine** (or an EC2 instance) by running the benchmark runner once with `CREATE_AND_SEED=true`. This creates tables if they do not exist (skip if you already created them in Step 1) and seeds **1000 customers × 1000 orders** (1M orders). For tables you already created, use the same table names and set only the seed path.

**Option A – Seed from local (or EC2) with the runner**

From the **repo root**:

```bash
export AWS_REGION=us-east-1
export CUSTOMERS_TABLE=customers_large
export ORDERS_TABLE=orders_large
export CREATE_AND_SEED=true

mvn test-compile exec:java -pl services-custom/dynamodb-enhanced \
  -Dexec.mainClass="software.amazon.awssdk.enhanced.dynamodb.functionaltests.EnhancedQueryBenchmarkRunner" \
  -Dexec.classpathScope=test
```

If the tables already exist, the initializer will skip creation and only seed data (idempotent). If you use **pay-per-request** billing, no capacity settings are needed. Seeding 1M items may take several minutes and incur write cost.

**Option B – Create tables via runner (omit Step 1)**

If you omit Step 1 and set `CREATE_AND_SEED=true`, the runner will try to create the tables. The SDK’s `createTable` uses **provisioned** throughput by default (50 RCU/WCU). For pay-per-request, create tables in Step 1 and only seed via the runner (run with `CREATE_AND_SEED=true` once; the initializer skips create if tables exist).

---

## Step 3: Launch EC2 and install Java + Maven

1. Launch an EC2 instance (e.g. Amazon Linux 2 or Ubuntu) in the **same region** as your DynamoDB tables.
2. Attach an **IAM role** to the instance with at least:
   - `dynamodb:GetItem`, `dynamodb:PutItem`, `dynamodb:Query`, `dynamodb:Scan`, `dynamodb:BatchWriteItem`, `dynamodb:DescribeTable`, `dynamodb:CreateTable` (if you use CREATE_AND_SEED).
3. SSH into the instance and install Java and Maven:

**Amazon Linux 2:**

```bash
sudo yum install -y java-11-amazon-corretto maven
```

**Ubuntu:**

```bash
sudo apt-get update && sudo apt-get install -y openjdk-11-jdk maven
```

4. Verify:

```bash
java -version
mvn -version
```

---

## Step 4: Build and copy the project to EC2

**On your local machine** (from repo root):

```bash
cd /path/to/aws-sdk-java-v2
mvn clean package -pl services-custom/dynamodb-enhanced -DskipTests -q
```

Copy the module and its dependencies to EC2. Option A: copy the whole repo and build on EC2. Option B: copy the built JAR and dependency JARs.

**Option A – Copy repo and build on EC2**

```bash
scp -r . ec2-user@<EC2_PUBLIC_IP>:~/aws-sdk-java-v2
ssh ec2-user@<EC2_PUBLIC_IP> "cd ~/aws-sdk-java-v2 && mvn clean test-compile -pl services-custom/dynamodb-enhanced -DskipTests -q"
```

**Option B – Copy only the dynamodb-enhanced module and run with mvn exec:java on EC2**

Copy the entire `aws-sdk-java-v2` repo (or at least the parent POMs and `services-custom/dynamodb-enhanced`) so that `mvn exec:java -pl services-custom/dynamodb-enhanced` can resolve the parent and run the benchmark. Building on EC2 is usually simpler:

```bash
rsync -avz --exclude='.git' . ec2-user@<EC2_PUBLIC_IP>:~/aws-sdk-java-v2
```

Then on EC2:

```bash
cd ~/aws-sdk-java-v2
mvn test-compile -pl services-custom/dynamodb-enhanced -DskipTests -q
```

---

## Step 5: Run the benchmark on EC2

SSH to the EC2 instance and set environment variables, then run the benchmark.

```bash
cd ~/aws-sdk-java-v2

export AWS_REGION=us-east-1
export CUSTOMERS_TABLE=customers_large
export ORDERS_TABLE=orders_large
export BENCHMARK_ITERATIONS=5
export BENCHMARK_WARMUP=2
# Optional: append CSV results to a file
export BENCHMARK_OUTPUT_FILE=benchmark_results.csv

# Do NOT set CREATE_AND_SEED unless you want to create/seed from this instance (tables should already exist and be seeded).

mvn exec:java -pl services-custom/dynamodb-enhanced \
  -Dexec.mainClass="software.amazon.awssdk.enhanced.dynamodb.functionaltests.EnhancedQueryBenchmarkRunner" \
  -Dexec.classpathScope=test -q
```

Example output:

```
Environment: AWS_REGION=us-east-1 CUSTOMERS_TABLE=customers_large ORDERS_TABLE=orders_large
Warmup=2 Iterations=5
---
baseOnly_keyCondition: avgMs=45.20 p50Ms=42 p95Ms=58 rows=1
joinInner_c1: avgMs=320.40 p50Ms=310 p95Ms=380 rows=1000
aggregation_groupByCount_c1: avgMs=305.20 p50Ms=298 p95Ms=350 rows=1
aggregation_groupBySum_c1: avgMs=318.60 p50Ms=312 p95Ms=355 rows=1
joinLeft_c1_limit50: avgMs=89.40 p50Ms=85 p95Ms=102 rows=50
```

---

## Step 6: Collect results

- **Stdout**: Redirect to a file, e.g. `mvn exec:java ... > benchmark_stdout.txt 2>&1`.
- **CSV**: If `BENCHMARK_OUTPUT_FILE` is set, the runner appends one CSV line per scenario to the file. Copy the file from EC2:

  ```bash
  scp ec2-user@<EC2_PUBLIC_IP>:~/aws-sdk-java-v2/benchmark_results.csv .
  ```

Use the output (avgMs, p50Ms, p95Ms, rows) in your design doc. Document in the doc: **region**, **EC2 instance type**, **table names**, **dataset size** (e.g. 1000 customers × 1000 orders), and **billing mode** (pay-per-request or provisioned).

---

## Step 7: Cleanup (optional)

To avoid ongoing cost, delete the DynamoDB tables and terminate the EC2 instance when done:

```bash
aws dynamodb delete-table --table-name customers_large --region us-east-1
aws dynamodb delete-table --table-name orders_large --region us-east-1
# Terminate the EC2 instance from the AWS Console or CLI.
```

---

## Environment variable reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `AWS_REGION` | No | default SDK region | DynamoDB region (e.g. `us-east-1`). |
| `CUSTOMERS_TABLE` | No | `customers_large` | Customers table name. |
| `ORDERS_TABLE` | No | `orders_large` | Orders table name. |
| `CREATE_AND_SEED` | No | (unset) | Set to `true` to create tables (if missing) and seed 1000×1000 data. Requires DynamoDB create/put permissions. |
| `BENCHMARK_ITERATIONS` | No | `5` | Number of measured runs per scenario. |
| `BENCHMARK_WARMUP` | No | `2` | Warm-up runs per scenario before measuring. |
| `BENCHMARK_OUTPUT_FILE` | No | (none) | If set, CSV results are appended to this path. |

---

## Running locally against DynamoDB Local

To run the same benchmark against **DynamoDB Local** (e.g. for CI or no-AWS runs):

1. Start DynamoDB Local (e.g. `docker run -p 8000:8000 amazon/dynamodb-local` or the SDK’s embedded LocalDynamoDb).
2. Set `AWS_REGION` and point the SDK to the local endpoint (e.g. `DYNAMODB_ENDPOINT_OVERRIDE=http://localhost:8000` if your test setup supports it, or run the functional tests which use in-process LocalDynamoDb).

The benchmark runner does **not** set an endpoint override by default; it uses the default DynamoDB endpoint for the given region. To run against Local, you would need to configure the client with an endpoint override (e.g. in a variant of the runner or via a system property your client builder reads). The functional tests and `run-enhanced-query-tests-and-print-timing.sh` already run against Local and produce timing output for the design doc.
