## Overview

Mid-level DynamoDB mapper/abstraction for Java using the v2 AWS SDK.

Warning: This package is provided for preview and comment purposes only.
It is not asserted to be stable or correct, and is subject to frequent
breaking changes.

## Getting Started
All the examples below use a fictional Customer class. This class is
completely made up and not part of this library. Any search or key
values used are also completely arbitrary.

### Initialization
1. Create or use a java class for mapping records to and from the
   database table. The class does not need to conform to Java bean
   standards but you will need getters and setters to all the attributes
   you want to map. Here's an example :-
   ```java
   public class Customer {
       private String accountId;
       private int subId;            // you could also use Integer here
       private String name;
       private String createdDate;
       
       public String getAccountId() { return this.accountId; }
       public void setAccountId(String accountId) { this.accountId = accountId; }
       
       public int getSubId() { return this.subId; }
       public void setSubId(int subId) { this.subId = subId; }
       
       public String getName() { return this.name; }
       public void setName(String name) { this.name = name; }
       
       public String getCreatedDate() { return this.createdDate; }
       public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
   }
   ```
   
2. Create a static TableSchema for your class. You could put this in the
   class itself, or somewhere else :-
   ```java
   static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
     StaticTableSchema.builder(Customer.class)
       .newItemSupplier(Customer::new)       // Tells the mapper how to make new objects when reading items
       .attributes(
         stringAttribute("account_id", 
                         Customer::getAccountId, 
                         Customer::setAccountId)
            .as(primaryPartitionKey()),                                                  // primary partition key         
         integerNumberAttribute("sub_id", 
                                Customer::getSubId, 
                                Customer::setSubId)
            .as(primarySortKey()),                                                       // primary sort key
         stringAttribute("name", 
                         Customer::getName, 
                         Customer::setName)
            .as(secondaryPartitionKey("customers_by_name")),                             // GSI partition key
         stringAttribute("created_date", 
                         Customer::getCreatedDate, 
                         Customer::setCreatedDate)
            .as(secondarySortKey("customers_by_date"), 
                secondarySortKey("customers_by_name")))              // Sort key for both the LSI and the GSI
       .build();
   ```
   
3. Create a DynamoDbEnhancedClient object that you will use to repeatedly
   execute operations against all your tables :- 
   ```java
   DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                                                                 .dynamoDbClient(dynamoDbClient)
                                                                 .build();
   ```
4. Create a DynamoDbTable object that you will use to repeatedly execute
  operations against a specific table :-
   ```java
   // Maps a physical table with the name 'customers_20190205' to the schema
   DynamoDbTable<Customer> customerTable = enhancedClient.table("customers_20190205", CUSTOMER_TABLE_SCHEMA);
   ```
 
### Common primitive operations
These all strongly map to the primitive DynamoDB operations they are
named after. These examples are the most simple variants of each
operation possible. These commands can be customized by using the
builders provided for each command and offer most of the features
available in the low-level DynamoDB SDK client.

   ```java
   // CreateTable
   customerTable.createTable(CreateTableEnhancedRequest.create());
   
   // GetItem
   Customer customer = customerTable.getItem(GetItemEnhancedRequest.create(Key.create(stringValue("a123"))));
   
   // UpdateItem
   Customer updatedCustomer = customerTable.updateItem(UpdateItemEnhancedRequest.create(customer));
   
   // PutItem
   customerTable.putItem(PutItemEnhancedRequest.create(customer));
   
   // DeleteItem
   Customer deletedCustomer = customerTable.deleteItem(DeleteItemEnhancedRequest.create(Key.create(stringValue("a123"), numberValue(456))));
   
   // Query
   Iterable<Page<Customer>> customers = customerTable.query(QueryEnhancedRequest.create(equalTo(Key.create(stringValue("a123")))));
   
   // Scan
   Iterable<Page<Customer>> customers = customerTable.scan(ScanEnhancedRequest.create());
   
   // BatchGetItem
   batchResults = enhancedClient.batchGetItem(
       BatchGetItemEnhancedRequest.builder().addReadBatch(ReadBatch.builder(Customer.class)
                                                                   .mappedTableResource(customerTable)
                                                                   .addGetItem(GetItemEnhancedRequest.create(key1))
                                                                   .addGetItem(GetItemEnhancedRequest.create(key2))
                                                                   .addGetItem(GetItemEnhancedRequest.create(key3))
                                                                   .build())
                                  .build());
   
   // BatchWriteItem
   batchResults = enhancedClient.batchWriteItem(
       BatchWriteItemEnhancedRequest.builder().addWriteBatch(WriteBatch.builder(Customer.class)
                                                                       .mappedTableResource(customerTable)
                                                                       .putItem(PutItemEnhancedRequest.create(item))
                                                                       .deleteItem(DeleteItemEnhancedRequest.create(key1))
                                                                       .deleteItem(DeleteItemEnhancedRequest.create(key2))
                                                                       .build())
                                    .build());
   
   // TransactGetItems
   transactResults = enhancedClient.transactGetItems(
       TransactGetItemsEnhancedRequest.builder()
                                      .addGetItem(customerTable, GetItemEnhancedRequest.create(Key.create(key1)))
                                      .addGetItem(customerTable, GetItemEnhancedRequest.create(Key.create(key2)))
                                      .build());
   
   // TransactWriteItems
   enhancedClient.transactWriteItems(
       TransactWriteItemsEnhancedRequest.builder()
                                        .addConditionCheck(customerTable, ConditionCheck.create(orderKey, conditionExpression))
                                        .addUpdateItem(customerTable, UpdateItemEnhancedRequest.create(customer))
                                        .addDeleteItem(customerTable, DeleteItemEnhancedRequest.create(key))
                                        .build());
```
   
### Using secondary indices
Certain operations (Query and Scan) may be executed against a secondary
index. Here's an example of how to do this:
   ```
   DynamoDbIndex<Customer> customersByName = customerTable.index("customers_by_name");
       
   Iterable<Page<Customer>> customersWithName = customersByName.query(QueryEnhancedRequest.create(equalTo(Key.create(stringValue("Smith")))));
   ```

### Non-blocking asynchronous operations
If your application requires non-blocking asynchronous calls to
DynamoDb, then you can use the asynchronous implementation of the
mapper. It's very similar to the synchronous implementation with a few
key differences:

1. When instantiating the mapped database, use the asynchronous version
   of the library instead of the synchronous one (you will need to use
   an asynchronous DynamoDb client from the SDK as well):
   ```java
    DynamoDbEnhancedAsyncClient enhancedClient = DynamoDbEnhancedAsyncClient.builder()
                                                                            .dynamoDbClient(dynamoDbAsyncClient)
                                                                            .build();
   ```

2. Operations that return a single data item will return a
   CompletableFuture of the result instead of just the result. Your
   application can then do other work without having to block on the
   result:
   ```java
   CompletableFuture<Customer> result = mappedTable.getItem(GetItemEnhancedRequest.create(customerKey));
   // Perform other work here
   return result.join();   // now block and wait for the result
   ```

3. Operations that return paginated lists of results will return an
   SdkPublisher of the results instead of an SdkIterable. Your
   application can then subscribe a handler to that publisher and deal
   with the results asynchronously without having to block:
   ```java
   SdkPublisher<Customer> results = mappedTable.query(QueryEnhancedRequest.create(equalTo(Key.create(stringValue("a123")))));
   results.subscribe(myCustomerResultsProcessor);
   // Perform other work and let the processor handle the results asynchronously
   ```


### Using extensions
The mapper supports plugin extensions to provide enhanced functionality
beyond the simple primitive mapped operations. Only one extension can be
loaded into a DynamoDbEnhancedClient. Any number of extensions can be chained
together in a specific order into a single extension using a
ChainExtension. Extensions have two hooks, beforeWrite() and
afterRead(); the former can modify a write operation before it happens,
and the latter can modify the results of a read operation after it
happens. Some operations such as UpdateItem perform both a write and
then a read, so call both hooks.

#### VersionedRecordExtension

This extension will increment and track a record version number as
records are written to the database. A condition will be added to every
write that will cause the write to fail if the record version number of
the actual persisted record does not match the value that the
application last read. This effectively provides optimistic locking for
record updates, if another process updates a record between the time the
first process has read the record and is writing an update to it then
that write will fail. 

To load the extension:
```java
DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                        .dynamoDbClient(dynamoDbClient)
                        .extendWith(VersionedRecordExtension.builder().build())
                        .build();
```

To tell the extension which attribute to use to track the record version
number tag a numeric attribute in the TableSchema with the version()
AttributeTag:
```java
    integerNumberAttribute("version", 
                           Customer::getVersion, 
                           Customer::setVersion)
        .as(version())          // Apply the 'version' tag to the attribute                         
```

## Advanced scenarios
### Flat map attributes from another class
If the attributes for your table record are spread across several
different Java objects, either through inheritance or composition, the
static TableSchema implementation gives you a method of flat mapping
those attributes and rolling them up into a single schema.

To accomplish this using inheritance:-
```java
@Data
public class Customer extends GenericRecord {
  private String name;
}

@Data
public abstract class GenericRecord {
  private String id;
  private String createdDate;
}

private static final StaticTableSchema<GenericRecord> GENERIC_RECORD_SCHEMA =
  StaticTableSchema.builder(GenericRecord.class)
    .attributes(
          // The partition key will be inherited by the top level mapper
      stringAttribute("id", GenericRecord::getId, GenericRecord::setId).as(primaryPartitionKey()),
      stringAttribute("created_date", GenericRecord::getCreatedDate, GenericRecord::setCreatedDate))
    .build();
    
private static final StaticTableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
  StaticTableSchema.builder(Customer.class)
    .newItemSupplier(Customer::new)
    .attributes(
      stringAttribute("name", Customer::getName, Customer::setName))
    .extend(GENERIC_RECORD_SCHEMA)     // All the attributes of the GenericRecord schema are added to Customer
    .build();
```

Using composition:
```java
@Data
public class Customer{
  private String name;
  private GenericRecord recordMetadata;
}

@Data
public class GenericRecord {
  private String id;
  private String createdDate;
}

private static final StaticTableSchema<GenericRecord> GENERIC_RECORD_SCHEMA =
  StaticTableSchema.builder(GenericRecord.class)
    .newItemSupplier(GenericRecord::new)
    .attributes(
      stringAttribute("id", GenericRecord::getId, GenericRecord::setId).as(primaryPartitionKey()),
      stringAttribute("created_date", GenericRecord::getCreatedDate, GenericRecord::setCreatedDate))
    .build();
    
private static final StaticTableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
  StaticTableSchema.builder(Customer.class)
    .newItemSupplier(Customer::new)
    .attributes(stringAttribute("name", Customer::getName, Customer::setName))
    // Because we are flattening a component object, we supply a getter and setter so the
    // mapper knows how to access it
    .flatten(CUSTOMER_TABLE_SCHEMA, Customer::getRecordMetadata, Customer::setRecordMetadata)
    .build(); 
```
You can flatten as many different eligible classes as you like using the
builder pattern. The only constraints are that attributes must not have
the same name when they are being rolled together, and there must never
be more than one partition key, sort key or table name.