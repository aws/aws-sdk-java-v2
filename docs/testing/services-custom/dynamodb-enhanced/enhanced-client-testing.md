# DynamoDB Enhanced Client Test Checklist

Below is the checklist required for merging DynamoDB Enhanced Client test changes.
Please complete this checklist when making any changes to Enhanced DDB.

**Instructions:**
- Mark completed tests with [x] (e.g., [x] instead of [ ])
- For tests not completed, provide a reason in the "Comments" column
- If your code changes are covered by existing tests, reference the test class and method name in the Comments column (e.g., `TableSchemaTest.testFromBean()`)
- Please copy the markdown into your PR and update it in Testing section



```md
## Test Coverage Checklist

| Scenario | Done | Comments if Not Done |
|---------|:----:|---------------------|
| **1. Different TableSchema Creation Methods** | | |
| a. TableSchema.fromBean(Customer.class) | [ ] | |
| b. TableSchema.fromImmutableClass(Customer.class) for immutable classes | [ ] | |
| c. TableSchema.documentSchemaBuilder().build() | [ ] | |
| d. StaticTableSchema.builder(Customer.class) | [ ] | |
| **2. Nesting of Different TableSchema Types ** (Ref-1)| | |
| a. @DynamoDbBean with non-null nested @DynamoDbBean attribute | [ ] | |
| b. @DynamoDbBean with non-null nested @DynamoDbImmutable attribute | [ ] | |
| c. @DynamoDbImmutable with non-null nested @DynamoDbBean attribute | [ ] | |
| d. @DynamoDbBean with null nested @DynamoDbBean attribute | [ ] | |
| e. @DynamoDbBean with null nested @DynamoDbImmutable attribute | [ ] | |
| f. @DynamoDbImmutable with null nested @DynamoDbBean attribute | [ ] | |
| **3. CRUD Operations** (Ref-2) | | |
| a. scan() | [ ] | |
| b. query() | [ ] | |
| c. updateItem() | [ ] | |
| d. putItem() | [ ] | |
| e. getItem() | [ ] | |
| f. deleteItem() | [ ] | |
| g. batchGetItem() | [ ] | |
| h. batchWriteItem() | [ ] | |
| i. transactGetItems() | [ ] | |
| j. transactWriteItems()  | [ ] | |
| **4. Null Handling for Different Attribute types (Ref-3)** | |  |
| a. top-level null attributes | [ ] | |
| b. collections with null elements | [ ] | |
| c. maps with null values | [ ] | |
| d. full serialization/deserialization cycle with null values | [ ] | |
| **5. AsyncTable and SyncTable** | | |
| a. DynamoDbAsyncTable Testing | [ ] | |
| b. DynamoDbTable Testing | [ ] | |
| **6. New/Modification in Extensions** | | |
| a. Tables with Scenario in ScenarioSl No.1 (All table schemas are Must) | [ ] | |
| b. Test with Default Values in Annotations | [ ] | |
| c. Combination of Annotation and Builder passes extension | [ ] | |
| **7. New/Modification in Converters** | | |
| a. Tables with Scenario in ScenarioSl No.1 (All table schemas are Must) | [ ] | |
| b. Test with Default Values in Annotations | [ ] | |
| c. Test All Scenarios from 1 to 5 | [ ] | |
```

## Reference

[Ref-1] Issue were assigning Nested attribute as Null value resulted in NPE [#6037](https://github.com/aws/aws-sdk-java-v2/pull/6037)

[Ref-2] [Reference for CRUD Operations test](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/dynamodb-enhanced/src/test/java/software/amazon/awssdk/enhanced/dynamodb/functionaltests/BasicCrudTest.java)

[Ref-3] Example for Null Handling for Different Attribute types

| Case                                            | Bean Representation | Item Representation (DynamoDB JSON) |
|-------------------------------------------------|---------------------|-------------------------------------|
| **a. Top-level null attributes**                | <pre>Customer c = new Customer();<br>c.setAccountId("123");<br>c.setName(null);</pre> | <pre>{<br>  "accountId": {"S": "123"},<br>  "name": {"NULL": true}<br>}</pre> |
| **b. Collections with null elements**           | <pre>Customer c = new Customer();<br>c.setAccountId("123");<br>c.setEmails(Arrays.asList(<br>  "a@example.com", null));</pre> | <pre>{<br>  "accountId": {"S": "123"},<br>  "emails": {<br>    "L": [<br>      {"S": "a@example.com"},<br>      {"NULL": true}<br>    ]<br>  }<br>}</pre> |
| **c. Maps with null values**                    | <pre>Customer c = new Customer();<br>c.setAccountId("123");<br>Map<String, String> attrs = new HashMap<>();<br>attrs.put("tier", "gold");<br>attrs.put("status", null);<br>c.setAttributes(attrs);</pre> | <pre>{<br>  "accountId": {"S": "123"},<br>  "attributes": {<br>    "M": {<br>      "tier": {"S": "gold"},<br>      "status": {"NULL": true}<br>    }<br>  }<br>}</pre> |
| **d. Full serialization/deserialization cycle** | <pre>// Create with nulls<br>Customer c = new Customer();<br>c.setAccountId("123");<br>c.setName(null);<br>c.setEmails(Arrays.asList("a@example.com", null));<br><br>// Save to DynamoDB<br>table.putItem(c);<br><br>// Retrieve<br>Customer retrieved = table.getItem(key);<br><br>// Nulls preserved<br>assert retrieved.getName() == null;<br>assert retrieved.getEmails().get(1) == null;</pre> | <pre>{<br>  "accountId": {"S": "123"},<br>  "name": {"NULL": true},<br>  "emails": {<br>    "L": [<br>      {"S": "a@example.com"},<br>      {"NULL": true}<br>    ]<br>  }<br>}</pre> |
- [Reference for Null Handling for Different Attribute types](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/dynamodb-enhanced/src/test/java/software/amazon/awssdk/enhanced/dynamodb/document/EnhancedDocumentTestData.java)

