# DynamoDB Enhanced Client Test Checklist

Below is the checklist required for merging DynamoDB Enhanced Client test changes.
Please complete this checklist when making any changes to Enhanced DDB.

**Instructions:**
- Mark completed tests with [x] (e.g., [x] instead of [ ])
- For tests not completed, provide a reason in the "Comments" column
- If your code changes are covered by existing tests, reference the test class and method name in the Comments column (e.g., `TableSchemaTest.testFromBean()`)
- Please copy the markdown into your PR and update it in Testing section
- [Reference for CRUD Operations test](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/dynamodb-enhanced/src/test/java/software/amazon/awssdk/enhanced/dynamodb/functionaltests/BasicCrudTest.java)
- [Reference for Data Types and Null Handling](https://github.com/aws/aws-sdk-java-v2/blob/master/services-custom/dynamodb-enhanced/src/test/java/software/amazon/awssdk/enhanced/dynamodb/document/EnhancedDocumentTestData.java) 

```md
## Test Coverage Checklist

| Scenario | Done | Comments if Not Done |
|---------|:----:|---------------------|
| **1. Different TableSchema Creation Methods** | | |
| a. TableSchema.fromBean(Customer.class) | [ ] | |
| b. TableSchema.fromImmutableClass(Customer.class) for immutable classes | [ ] | |
| c. TableSchema.documentSchemaBuilder().build() | [ ] | |
| d. StaticTableSchema.builder(Customer.class) | [ ] | |
| **2. Nesting of Different TableSchema Types** | | |
| a. @DynamoDbBean with nested @DynamoDbBean as NonNull | [ ] | |
| b. @DynamoDbBean with nested @DynamoDbImmutable as NonNull | [ ] | |
| c. @DynamoDbImmutable with nested @DynamoDbBean as NonNull | [ ] | |
| d. @DynamoDbBean with nested @DynamoDbBean as Null | [ ] | |
| e. @DynamoDbBean with nested @DynamoDbImmutable as Null| [ ] | |
| f. @DynamoDbImmutable with nested @DynamoDbBean as Null | [ ] | |
| **3. CRUD Operations** | | |
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
| **4. Data Types and Null Handling** | |  |
| a. top-level null attributes | [ ] | |
| b. collections with null elements | [ ] | |
| c. maps with null values | [ ] | |
| d. conversion between null Java values and AttributeValue | [ ] | |
| e. full serialization/deserialization cycle with null values | [ ] | |
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