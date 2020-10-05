## Overview

Mid-level DynamoDB mapper/abstraction for Java using the v2 AWS SDK.

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
   
2. Create a TableSchema for your class. For this example we are using a static constructor method on TableSchema that 
   will scan your annotated class and infer the table structure and attributes :
   ```java
   static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA = TableSchema.fromClass(Customer.class);
   ```
   
   If you would prefer to skip the slightly costly bean inference for a faster solution, you can instead declare your 
   schema directly and let the compiler do the heavy lifting. If you do it this way, your class does not need to follow
   bean naming standards nor does it need to be annotated. This example is equivalent to the bean example : 
   ```java
   static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
     TableSchema.builder(Customer.class)
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
operation possible. Each operation can be further customized by passing 
in an enhanced request object. These enhanced request objects offer most 
of the features available in the low-level DynamoDB SDK client and are
fully documented in the Javadoc of the interfaces referenced in these examples.

   ```java
   // CreateTable
   customerTable.createTable();
   
   // GetItem
   Customer customer = customerTable.getItem(Key.builder().partitionValue("a123").build());

   // UpdateItem
   Customer updatedCustomer = customerTable.updateItem(customer);
   
   // PutItem
   customerTable.putItem(customer);
   
   // DeleteItem
   Customer deletedCustomer = customerTable.deleteItem(Key.builder().partitionValue("a123").sortValue(456).build());
   
   // Query
   PageIterable<Customer> customers = customerTable.query(equalTo(k -> k.partitionValue("a123")));

   // Scan
   PageIterable<Customer> customers = customerTable.scan();
   
   // BatchGetItem
   BatchGetResultPageIterable batchResults = enhancedClient.batchGetItem(r -> r.addReadBatch(ReadBatch.builder(Customer.class)
                                                                               .mappedTableResource(customerTable)
                                                                               .addGetItem(key1)
                                                                               .addGetItem(key2)
                                                                               .addGetItem(key3)
                                                                               .build()));
   
   // BatchWriteItem
   batchResults = enhancedClient.batchWriteItem(r -> r.addWriteBatch(WriteBatch.builder(Customer.class)
                                                                               .mappedTableResource(customerTable)
                                                                               .addPutItem(customer)
                                                                               .addDeleteItem(key1)
                                                                               .addDeleteItem(key1)
                                                                               .build()));
   
   // TransactGetItems
   transactResults = enhancedClient.transactGetItems(r -> r.addGetItem(customerTable, key1)
                                                           .addGetItem(customerTable, key2));
   
   // TransactWriteItems
   enhancedClient.transactWriteItems(r -> r.addConditionCheck(customerTable, 
                                                              i -> i.key(orderKey)
                                                                    .conditionExpression(conditionExpression))
                                           .addUpdateItem(customerTable, customer)
                                           .addDeleteItem(customerTable, key));
```
   
### Using secondary indices
Certain operations (Query and Scan) may be executed against a secondary
index. Here's an example of how to do this:
   ```java
   DynamoDbIndex<Customer> customersByName = customerTable.index("customers_by_name");
       
   PageIterable<Customer> customersWithName = 
       customersByName.query(r -> r.queryConditional(equalTo(k -> k.partitionValue("Smith"))));
   ```

### Working with immutable data classes
It is possible to have the DynamoDB Enhanced Client map directly to and from immutable data classes in Java. An
immutable class is expected to only have getters and will also be associated with a separate builder class that
is used to construct instances of the immutable data class. The DynamoDB annotation style for immutable classes is
very similar to bean classes :

```java
@DynamoDbImmutable(builder = Customer.Builder.class)
public class Customer {
    private final String accountId;
    private final int subId;        
    private final String name;
    private final Instant createdDate;
    
    private Customer(Builder b) {
        this.accountId = b.accountId;
        this.subId = b.subId;
        this.name = b.name;
        this.createdDate = b.createdDate;
    }   

    // This method will be automatically discovered and used by the TableSchema
    public static Builder builder() { return new Builder(); }

    @DynamoDbPartitionKey
    public String accountId() { return this.accountId; }
    
    @DynamoDbSortKey
    public int subId() { return this.subId; }
    
    @DynamoDbSecondaryPartitionKey(indexNames = "customers_by_name")
    public String name() { return this.name; }
    
    @DynamoDbSecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"})
    public Instant createdDate() { return this.createdDate; }
    
    public static final class Builder {
        private String accountId;
        private int subId;        
        private String name;
        private Instant createdDate;

        private Builder() {}

        public Builder accountId(String accountId) { this.accountId = accountId; return this; }
        public Builder subId(int subId) { this.subId = subId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder createdDate(Instant createdDate) { this.createdDate = createdDate; return this; }

        // This method will be automatically discovered and used by the TableSchema
        public Customer build() { return new Customer(this); }
    }
}
```

The following requirements must be met for a class annotated with @DynamoDbImmutable:
1. Every method on the immutable class that is not an override of Object.class or annotated with @DynamoDbIgnore must
   be a getter for an attribute of the database record.
1. Every getter in the immutable class must have a corresponding setter on the builder class that has a case-sensitive
   matching name.
1. EITHER: the builder class must have a public default constructor; OR: there must be a public static method named
   'builder' on the immutable class that takes no parameters and returns an instance of the builder class.
1. The builder class must have a public method named 'build' that takes no parameters and returns an instance of the
   immutable class.
   
There are third-party library that help generate a lot of the boilerplate code associated with immutable objects.
The DynamoDb Enhanced client should work with these libraries as long as they follow the conventions detailed
in this section. Here's an example of the immutable Customer class using Lombok with DynamoDb annotations (note
how Lombok's 'onMethod' feature is leveraged to copy the attribute based DynamoDb annotations onto the generated code):

```java
    @Value
    @Builder
    @DynamoDbImmutable(builder = Customer.CustomerBuilder.class)
    public static class Customer {
        @Getter(onMethod = @__({@DynamoDbPartitionKey}))
        private String accountId;

        @Getter(onMethod = @__({@DynamoDbSortKey}))
        private int subId;  
      
        @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = "customers_by_name")}))
        private String name;

        @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = {"customers_by_date", "customers_by_name"})}))
        private Instant createdDate;
    }
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
    DynamoDbEnhancedAsyncClient enhancedClient = 
        DynamoDbEnhancedAsyncClient.builder()
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
   PagePublisher<Customer> results = mappedTable.query(r -> r.queryConditional(equalTo(k -> k.partitionValue("Smith"))));
   results.subscribe(myCustomerResultsProcessor);
   // Perform other work and let the processor handle the results asynchronously
   ```

## Using extensions
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

### VersionedRecordExtension

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

## Advanced table schema features
### Explicitly include/exclude attributes in DDB mapping
#### Excluding attributes
Ignore attributes that should not participate in mapping to DDB
Mark the attribute with the @DynamoDbIgnore annotation:
```java
private String internalKey;

@DynamoDbIgnore
public String getInternalKey() { return this.internalKey; }
public void setInternalKey(String internalKey) { return this.internalKey = internalKey;}
```
#### Including attributes
Change the name used to store an attribute in DBB by explicitly marking it with the
 @DynamoDbAttribute annotation and supplying a different name:
```java
private String internalKey;

@DynamoDbAttribute("renamedInternalKey")
public String getInternalKey() { return this.internalKey; }
public void setInternalKey(String internalKey) { return this.internalKey = internalKey;}
```

### Control attribute conversion
By default, the table schema provides converters for all primitive and many common Java types
through a default implementation of the AttributeConverterProvider interface. This behavior
can be changed both at the attribute converter provider level as well as for a single attribute.

#### Provide custom attribute converter providers
You can provide a single AttributeConverterProvider or a chain of ordered AttributeConverterProviders
through the @DynamoDbBean 'converterProviders' annotation. Any custom AttributeConverterProvider must extend the AttributeConverterProvider 
interface. 

Note that if you supply your own chain of attribute converter providers, you will override
the default converter provider (DefaultAttributeConverterProvider) and must therefore include it in the chain if you wish to
use its attribute converters. It's also possible to annotate the bean with an empty array `{}`, thus
disabling the usage of any attribute converter providers including the default, in which case
all attributes must have their own attribute converters (see below).

Single converter provider:
```java
@DynamoDbBean(converterProviders = ConverterProvider1.class)
public class Customer {

}
```

Chain of converter providers ending with the default (least priority):
```java
@DynamoDbBean(converterProviders = {
   ConverterProvider1.class, 
   ConverterProvider2.class,
   DefaultAttributeConverterProvider.class})
public class Customer {

}
```

In the same way, adding a chain of attribute converter providers directly to a StaticTableSchema:
```java
private static final StaticTableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
  StaticTableSchema.builder(Customer.class)
    .newItemSupplier(Customer::new)
    .addAttribute(String.class, a -> a.name("name")
                                     a.getter(Customer::getName)
                                     a.setter(Customer::setName))
    .attributeConverterProviders(converterProvider1, converterProvider2)
    .build();
```

#### Override the mapping of a single attribute
Supply an AttributeConverter when creating the attribute to directly override any
converters provided by the table schema AttributeConverterProviders. Note that you will 
only add a custom converter for that attribute; other attributes, even of the same
type, will not use that converter unless explicitly specified for those other attributes. 

Example:
```java
@DynamoDbBean
public class Customer {
    private String name;

    @DynamoDbConvertedBy(CustomAttributeConverter.class)
    public String getName() { return this.name; }
    public void setName(String name) { this.name = name;}
}
```
For StaticTableSchema:
```java
private static final StaticTableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
  StaticTableSchema.builder(Customer.class)
    .newItemSupplier(Customer::new)
    .addAttribute(String.class, a -> a.name("name")
                                     a.getter(Customer::getName)
                                     a.setter(Customer::setName)
                                     a.attributeConverter(customAttributeConverter))
    .build();
```

### Changing update behavior of attributes
It is possible to customize the update behavior as applicable to individual attributes when an 'update' operation is
performed (e.g. UpdateItem or an update within TransactWriteItems).

For example, say like you wanted to store a 'created on' timestamp on your record, but only wanted its value to be
written if there is no existing value for the attribute stored in the database then you would use the 
WRITE_IF_NOT_EXISTS update behavior. Here is an example using a bean:

```java
@DynamoDbBean
public class Customer extends GenericRecord {
    private String id;
    private Instant createdOn;

    @DynamoDbPartitionKey
    public String getId() { return this.id; }
    public void setId(String id) { this.name = id; }

    @DynamoDbUpdateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)
    public Instant getCreatedOn() { return this.createdOn; }    
    public void setCreatedOn(Instant createdOn) { this.createdOn = createdOn; }
}
```

Same example using a static table schema:

```java
static final TableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
     TableSchema.builder(Customer.class)
       .newItemSupplier(Customer::new)
       .addAttribute(String.class, a -> a.name("id")
                                         .getter(Customer::getId)
                                         .setter(Customer::setId)
                                         .tags(primaryPartitionKey()))
       .addAttribute(Instant.class, a -> a.name("createdOn")
                                          .getter(Customer::getCreatedOn)
                                          .setter(Customer::setCreatedOn)
                                          .tags(updateBehavior(UpdateBehavior.WRITE_IF_NOT_EXISTS)))
       .build();
```

### Flat map attributes from another class
If the attributes for your table record are spread across several
different Java objects, either through inheritance or composition, the
static TableSchema implementation gives you a method of flat mapping
those attributes and rolling them up into a single schema.

#### Using inheritance
To accomplish flat map using inheritance, the only requirement is that
both classes are annotated as a DynamoDb bean:

```java
@DynamoDbBean
public class Customer extends GenericRecord {
    private String name;
    private GenericRecord record;

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name;}

    public GenericRecord getRecord() { return this.record; }
    public void setRecord(GenericRecord record) { this.record = record;}
}

@DynamoDbBean
public abstract class GenericRecord {
    private String id;
    private String createdDate;

    public String getId() { return this.id; }
    public void setId(String id) { this.id = id;}

    public String getCreatedDate() { return this.createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate;}
}

```

For StaticTableSchema, use the 'extend' feature to achieve the same effect:
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
       // The partition key will be inherited by the top level mapper
      .addAttribute(String.class, a -> a.name("id")
                                        .getter(GenericRecord::getId)
                                        .setter(GenericRecord::setId)
                                        .tags(primaryPartitionKey()))
      .addAttribute(String.class, a -> a.name("created_date")
                                        .getter(GenericRecord::getCreatedDate)
                                        .setter(GenericRecord::setCreatedDate))
     .build();
    
private static final StaticTableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
  StaticTableSchema.builder(Customer.class)
    .newItemSupplier(Customer::new)
    .addAttribute(String.class, a -> a.name("name")
                                      .getter(Customer::getName)
                                      .setter(Customer::setName))
    .extend(GENERIC_RECORD_SCHEMA)     // All the attributes of the GenericRecord schema are added to Customer
    .build();
```
#### Using composition

Using composition, the @DynamoDbFlatten annotation flat maps the composite class:
```java
@DynamoDbBean
public class Customer {
    private String name;
    private GenericRecord record;

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name;}

    @DynamoDbFlatten
    public GenericRecord getRecord() { return this.record; }
    public void setRecord(GenericRecord record) { this.record = record;}
}

@DynamoDbBean
public class GenericRecord {
    private String id;
    private String createdDate;

    public String getId() { return this.id; }
    public void setId(String id) { this.id = id;}

    public String getCreatedDate() { return this.createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate;}
}
```
You can flatten as many different eligible classes as you like using the flatten annotation.
The only constraints are that attributes must not have the same name when they are being rolled
together, and there must never be more than one partition key, sort key or table name.

Flat map composite classes using StaticTableSchema:

```java
@Data
public class Customer{
  private String name;
  private GenericRecord recordMetadata;
  //getters and setters for all attributes
}

@Data
public class GenericRecord {
  private String id;
  private String createdDate;
  //getters and setters for all attributes
}

private static final StaticTableSchema<GenericRecord> GENERIC_RECORD_SCHEMA =
  StaticTableSchema.builder(GenericRecord.class)
      .addAttribute(String.class, a -> a.name("id")
                                        .getter(GenericRecord::getId)
                                        .setter(GenericRecord::setId)
                                        .tags(primaryPartitionKey()))
      .addAttribute(String.class, a -> a.name("created_date")
                                        .getter(GenericRecord::getCreatedDate)
                                        .setter(GenericRecord::setCreatedDate))
     .build();
    
private static final StaticTableSchema<Customer> CUSTOMER_TABLE_SCHEMA =
  StaticTableSchema.builder(Customer.class)
    .newItemSupplier(Customer::new)
    .addAttribute(String.class, a -> a.name("name")
                                      .getter(Customer::getName)
                                      .setter(Customer::setName))
    // Because we are flattening a component object, we supply a getter and setter so the
    // mapper knows how to access it
    .flatten(GENERIC_RECORD_SCHEMA, Customer::getRecordMetadata, Customer::setRecordMetadata)
    .build(); 
```
Just as for annotations, you can flatten as many different eligible classes as you like using the
builder pattern. 
