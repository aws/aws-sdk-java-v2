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
   database table. At a minimum you must annotate the class so that
   it can be used as a DynamoDb bean, and also the property that
   represents the primary partition key of the table. Here's an example:-
   ```java
   @DynamoDbBean
   public class Customer {
       private String accountId;
       private int subId;            // primitive types are supported
       private String name;
       private Instant createdDate;
       
       @DynamoDbPartitionKey
       public String getAccountId() { return this.accountId; }
       public void setAccountId(String accountId) { this.accountId = accountId; }
       
       @DynamoDbSortKey
       public int getSubId() { return this.subId; }
       public void setSubId(int subId) { this.subId = subId; }
       
       // Defines a GSI (customers_by_name) with a partition key of 'name'
       @DynamoDbSecondaryPartitionKey(indexNames = "customers_by_name")
       public String getName() { return this.name; }
       public void setName(String name) { this.name = name; }
       
       // Defines an LSI (customers_by_date) with a sort key of 'createdDate' and also declares the 
       // same attribute as a sort key for the GSI named 'customers_by_name'
       @DynamoDbSecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"})
       public Instant getCreatedDate() { return this.createdDate; }
       public void setCreatedDate(Instant createdDate) { this.createdDate = createdDate; }
   }
   ```
   
2. Create a TableSchema for your class. For this example we are using the 'BeanTableSchema' that will scan your bean
   class and use the annotations to infer the table structure and attributes :
   ```java
   static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA = BeanTableSchema.create(Customer.class);
   ```
   
   If you would prefer to skip the slightly costly bean inference for a faster solution, you can instead declare your 
   schema directly and let the compiler do the heavy lifting. If you do it this way, your class does not need to follow
   bean naming standards nor does it need to be annotated. This example is equivalent to the bean example : 
   ```java
   static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
     StaticTableSchema.builder(Customer.class)
       .newItemSupplier(Customer::new)
       .addAttribute(String.class, a -> a.name("account_id")
                                         .getter(Customer::getAccountId)
                                         .setter(Customer::setAccountId)
                                         .tags(primaryPartitionKey()))
       .addAttribute(Integer.class, a -> a.name("sub_id")
                                          .getter(Customer::getSubId)
                                          .setter(Customer::setSubId)
                                          .tags(primarySortKey()))
       .addAttribute(String.class, a -> a.name("name")
                                         .getter(Customer::getName)
                                         .setter(Customer::setName)
                                         .tags(secondaryPartitionKey("customers_by_name")))
       .addAttribute(Instant.class, a -> a.name("created_date")
                                          .getter(Customer::getCreatedDate)
                                          .setter(Customer::setCreatedDate)
                                          .tags(secondarySortKey("customers_by_date"),
                                                secondarySortKey("customers_by_name")))
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
named after. The examples below are the most simple variants of each
operation possible, using the the two styles available for constructing
requests with either builder or consumers. These commands can be 
customized by using the builders provided for each command and offer 
most of the features available in the low-level DynamoDB SDK client.

   ```java
   // CreateTable
   customerTable.createTable();
   customerTable.createTable(CreateTableEnhancedRequest.builder().build());
   
   // GetItem
   Customer customer = customerTable.getItem(r -> r.key(k -> k.partitionValue("a123")));
   Customer customer = customerTable.getItem(GetItemEnhancedRequest.builder()
                                                                   .key(Key.builder().partitionValue("a123").build())
                                                                   .build()); 
   // UpdateItem
   Customer updatedCustomer = customerTable.updateItem(Customer.class, r -> r.item(customer));
   Customer updatedCustomer = customerTable.updateItem(UpdateItemEnhancedRequest.builder(Customer.class)
                                                                                .item(customer)
                                                                                .build());
   
   // PutItem
   customerTable.putItem(Customer.class, r -> r.item(customer));
   customerTable.putItem(PutItemEnhancedRequest.builder(Customer.class)
                                               .item(customer)
                                               .build());
   
   // DeleteItem
   Customer deletedCustomer = customerTable.deleteItem(r -> r.key(k -> partitionValue("a123").sortValue(456)));
   Customer deletedCustomer = customerTable.deleteItem(
        DeleteItemEnhancedRequest.builder()
                                 .key(Key.builder().partitionValue("a123").sortValue(456).build())
                                 .build());
   
   // Query
   Iterable<Page<Customer>> customers = customerTable.query(r -> r.queryConditional(equalTo(k -> k.partitionValue("a123"))));
   Iterable<Page<Customer>> customers = 
        customerTable.query(QueryEnhancedRequest.builder()
                                                .queryConditional(equalTo(Key.builder().partitionValue("a123").build()))
                                                .build());
   // Scan
   Iterable<Page<Customer>> customers = customerTable.scan();
   Iterable<Page<Customer>> customers = customerTable.scan(ScanEnhancedRequest.builder().build());
   
   // BatchGetItem
   batchResults = enhancedClient.batchGetItem(r -> r.addReadBatch(ReadBatch.builder(Customer.class)
                                                                           .mappedTableResource(customerTable)
                                                                           .addGetItem(i -> i.key(key1))
                                                                           .addGetItem(i -> i.key(key2))
                                                                           .addGetItem(i -> i.key(key3))
                                                                           .build()));
   batchResults = enhancedClient.batchGetItem(
       BatchGetItemEnhancedRequest.builder()
                                  .readBatches(ReadBatch.builder(Customer.class)
                                                        .mappedTableResource(customerTable)
                                                        .addGetItem(GetItemEnhancedRequest.builder().key(key1).build())
                                                        .addGetItem(GetItemEnhancedRequest.builder().key(key2).build())
                                                        .addGetItem(GetItemEnhancedRequest.builder().key(key3).build())
                                                        .build())
                                  .build());
   
   // BatchWriteItem
   batchResults = enhancedClient.batchWriteItem(r -> r.addWriteBatch(WriteBatch.builder(Customer.class)
                                                                               .mappedTableResource(customerTable)
                                                                               .addPutItem(i -> i.item(customer))
                                                                               .addDeleteItem(i -> i.key(key1))
                                                                               .addDeleteItem(i -> i.key(key1))
                                                                               .build()));
   batchResults = enhancedClient.batchWriteItem(
       BatchWriteItemEnhancedRequest.builder()
                                    .addWriteBatch(WriteBatch.builder(Customer.class)
                                                             .mappedTableResource(customerTable)
                                                             .addPutItem(PutItemEnhancedRequest.builder(Customer.class).item(customer).build())
                                                             .addDeleteItem(DeleteItemEnhancedRequest.builder().key(key1).build())
                                                             .addDeleteItem(DeleteItemEnhancedRequest.builder().key(key2).build())
                                                             .build())
                                    .build());
   
   // TransactGetItems
   transactResults = enhancedClient.transactGetItems(r -> r.addGetItem(customerTable, r -> r.key(key1))
                                                           .addGetItem(customerTable, r -> r.key(key2));
   transactResults = enhancedClient.transactGetItems(
       TransactGetItemsEnhancedRequest.builder()
                                      .addGetItem(customerTable, GetItemEnhancedRequest.builder().key(key1).build())
                                      .addGetItem(customerTable, GetItemEnhancedRequest.builder().key(key2).build())
                                      .build());
   
   // TransactWriteItems
   enhancedClient.transactWriteItems(r -> r.addConditionCheck(customerTable, i -> i.key(orderKey).conditionExpression(conditionExpression))
                                           .addUpdateItem(customerTable, Customer.class, i -> i.item(customer))
                                           .addDeleteItem(customerTable, i -> i.key(key)));

   enhancedClient.transactWriteItems(
       TransactWriteItemsEnhancedRequest.builder()
                                        .addConditionCheck(customerTable, ConditionCheck.builder()
                                                                                        .key(orderKey)
                                                                                        .conditionExpression(conditionExpression)
                                                                                        .build())
                                        .addUpdateItem(customerTable, UpdateItemEnhancedRequest.builder(Customer.class)
                                                                                               .item(customer)
                                                                                               .build())
                                        .addDeleteItem(customerTable, DeleteItemEnhancedRequest.builder()
                                                                                               .key(key)
                                                                                               .build())
                                        .build());
```
   
### Using secondary indices
Certain operations (Query and Scan) may be executed against a secondary
index. Here's an example of how to do this:
   ```java
   DynamoDbIndex<Customer> customersByName = customerTable.index("customers_by_name");
       
   Iterable<Page<Customer>> customersWithName = 
       customersByName.query(r -> r.queryConditional(equalTo(k -> k.partitionValue("Smith"))));
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
   CompletableFuture<Customer> result = mappedTable.getItem(r -> r.key(customerKey));
   // Perform other work here
   return result.join();   // now block and wait for the result
   ```

3. Operations that return paginated lists of results will return an
   SdkPublisher of the results instead of an SdkIterable. Your
   application can then subscribe a handler to that publisher and deal
   with the results asynchronously without having to block:
   ```java
   SdkPublisher<Customer> results = mappedTable.query(r -> r.queryConditional(equalTo(k -> k.partitionValue("Smith"))));
   results.subscribe(myCustomerResultsProcessor);
   // Perform other work and let the processor handle the results asynchronously
   ```


### Using extensions
The mapper supports plugin extensions to provide enhanced functionality
beyond the simple primitive mapped operations. Extensions have two hooks, beforeWrite() and
afterRead(); the former can modify a write operation before it happens,
and the latter can modify the results of a read operation after it
happens. Some operations such as UpdateItem perform both a write and
then a read, so call both hooks.

Extensions are loaded in the order they are specified in the enhanced client builder. This load order can be important,
as one extension can be acting on values that have been transformed by a previous extension. By default, just the
VersionedRecordExtension will be loaded, however you can override this behavior on the client builder and load any
extensions you like or specify none if you do not want the default bundled VersionedRecordExtension.

In this example, a custom extension named 'verifyChecksumExtension' is being loaded after the VersionedRecordExtension
which is usually loaded by default by itself:
```java
DynamoDbEnhancedClientExtension versionedRecordExtension = VersionedRecordExtension.builder().build();

DynamoDbEnhancedClient enhancedClient = 
    DynamoDbEnhancedClient.builder()
                          .dynamoDbClient(dynamoDbClient)
                          .extensions(versionedRecordExtension, verifyChecksumExtension)
                          .build();
```

#### VersionedRecordExtension

This extension is loaded by default and will increment and track a record version number as
records are written to the database. A condition will be added to every
write that will cause the write to fail if the record version number of
the actual persisted record does not match the value that the
application last read. This effectively provides optimistic locking for
record updates, if another process updates a record between the time the
first process has read the record and is writing an update to it then
that write will fail. 

To tell the extension which attribute to use to track the record version
number tag a numeric attribute in the TableSchema:
```java
    @DynamoDbVersionAttribute
    public Integer getVersion() {...};
    public void setVersion(Integer version) {...};
```
Or using a StaticTableSchema:
```java
    .addAttribute(Integer.class, a -> a.name("version")
                                       .getter(Customer::getVersion)
                                       .setter(Customer::setVersion)
                                        // Apply the 'version' tag to the attribute
                                       .tags(versionAttribute())                         
```

## Advanced StaticTableSchema scenarios
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