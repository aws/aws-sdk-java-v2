**Design:** New Feature, **Status:** Design

## Problem

DynamoDB is a NoSQL database that stores data in the form of [items](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithItems.html), which are collections of attributes. While the 
previous version of the AWS SDK for Java (1.x) provided a Document API to access these items, this feature was not 
included in the current version (2.x). This document proposes a mechanism to enable users to access DynamoDB items as 
documents using the enhanced DynamoDB client.

### Requested features
Aws-sdk-java 2.x should provide Document API similar to that of aws-sdk-java 1.x with following APIs 
1. APIs to access DynamoDB for complex data models without having to use the DynamoDB Mapper. 
 This could include APIs to convert between JSON and DynamoDB items, and vice versa.
2. APIs to manipulate semi-structured data for each attribute value, such as APIs to access AttributeValue as 
 string sets, number sets, string lists, number lists, etc. 
3. Direct read and write of DynamoDB elements as documents.

Example Github issue: https://github.com/aws/aws-sdk-java-v2/issues/36

## Current functionality
The current version of the AWS SDK for Java (2.x) provides mid-level DynamoDB mapper/abstraction for Java by providing 
Mapper Clients. However, when using these mappers, the user must define the complete table schema at the time of mapped 
table creation.

## Naming conventions
Please note that the names of classes and APIs mentioned in this design document are not final and are subject 
to change based on future reviews.

## Proposed Solution

The proposed solution is to add a new DocumentSchema to the existing enhanced client. 
This schema only requires the primary key and sort key to be defined at the time of mapped table creation. 
This will allow the user to retrieve DynamoDB table items as Documents. 
The user can then access the attribute values from these documents using getters. 
Similarly, the user can create a new Document and insert it into the mapped table using the builder APIs of Document.

### Enhanced Client Table Schema creation API
The DocumentSchema is created by providing the partition key, sort key, and optional attribute converter providers in the builder. 
If no AttributeConverterProviders are supplied in the TableSchema, the Document Table schema will use the default converter providers.
providers.

~~~java
 // Existing way of creating enhanced client
 DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().build();

// New API in TableSchema to create a DocumentTableSchema 
DocumentTableSchema documentTableSchema =
    TableSchema.documentSchemaBuilder()
               .addIndexPartitionKey(primaryIndexName(), "sample_hash_name", AttributeValueType.S)
               .addIndexSortKey(primaryIndexName(), "sample_sort_name", AttributeValueType.N)
               .addAttributeConverterProviders(cutomAttributeConverters, AttributeConverterProvider.defaultProvider())
               .build();
                       
 // Existing API to access DynamoDB table.    
 DynamoDbTable<EnhancedDocument> documentTable = enhancedClient.table("table-name", documentTableSchema);
~~~
*addAttributeConverterProviders = Appends custom attribute converter providers to the defaults provided by sdk*<br>

### Accessing Document  from DDB table
#### Reading a document to DDB Table
The DocumentSchema mapped table returns items as EnhancedDocument,
The EnhancedDocument can then be used to retrieve attribute values.

~~~java
// Creating a document which defined primary key of the item needs to be retrieved
EnhancedDocument hashKeyDocument = EnhancedDocument.builder()
                                                    .putString("sample_hash_name", "sample_value")
                                                    .build();
// Retrieving from existing Get operation.
EnhancedDocument retrievedDocument = documentTable.getItem(hashKeyDocument);
    
// Retrieving from existing Get operation
EnhancedDocument documentTableItem = documentTable.getItem(
                                        EnhancedDocument.builder()
                                                        .putString("sample_hash_name", "sample_value")
                                                        .build());

// Accessing an attribute from document using generic getter.
Integer sampleSortvalue = documentTableItem.get("sample_sort_name", EnhancedType.of(Integer.class));

// Accessing an attribute from document using specific getters.
SdkNumber sampleSortvalue = documentTableItem.getNumber("sample_sort_name"); 

// Accessing an attribute of custom class using custom converters.
CustomClass customClass = documentTableItem.get("custom_nested_map", EnhancedType.of(CustomClass.class));

// Accessing Simple List 
List<String> simpleLists = documentTableItem.getList("string_set", EnhancedType.of(String));

// Accessing Nested List 
List<List<String>> nestedLists = documentTableItem.get("string_set", new EnhancedType<List<List<<String>>>(){}));

// Accessing Nested List 
List<List<String>> nestedLists = documentTableItem.getList("string_set", new EnhancedType<List<<String>>>(){}));

// Accessing Simple Map 
Map<String, Integer> simpleMap = documentTableItem.getMapOfType("map_key", EnhancedType.of(String.class), EnhancedType.of(Integer.class));

// Accessing a unknown type attribute value list .
List<AttributeValue> unknownAttribute = enhancedDocument.getUnknownTypeList("sampleAttribute");

// Accessing a unknown type attribute value list .
Map<String, AttributeValue> unknownAttribute = enhancedDocument.getUnknownTypeMap("sampleAttribute");

// Convert Document to Map .
Map<String, AttributeValue> unknownAttribute = enhancedDocument.tonknownTypeMap();
~~~


#### Writing a document to DDB Table
The EnhancedDocument provides builder method to create documents that can be put to DocumentSchema mapped table.

~~~java

// Creating a document from Json input.   
EnhancedDocument  documentFromJson = EnhancedDocument.fromJson(("{\"sample_hash_name\": \"sample_value_2\"}"));
// put to dynamo db table
documentTable.putItem(documentFromJson);

// Creating a document from EnhanceDocumentBuilders    
EnhancedDocument documentFromBuilder = EnhancedDocument.builder()
                                                       .putString("sample_hash_name", "sample_value_2")
                                                       .putNumber("sample_sort_name", 111)
                                                       .putOfType("customElement", EnhancedType.of(Custom.class))
                                                       .putNumberSet("sample_names", Stream.of(1 ,2 ,3, 4).collect(Collectors.toSet()))
                                                       .build();
    
// put to dynamo db table
documentTable.documentFromBuilder(documentFromBuilder);

// retrieving a document from dynamo db and updating some attributes
EnhancedDocument documentTableItem = documentTable.getItem(hashKeyDocument);
// using toBuilder to make a copy of the retrieved item and then modifying the key attribute
EnhancedDocument changedValue = documentTableItem.toBuilder().putString("key-to-change", "changedValue").build();
// put to dynamo db table
documentTable.putItem(changedValue);
~~~

#### Attribute converter providers for EnhancedDocument

A builder method will be provided to add Attribute converters for an EnhancedDocument. 
By default, the attribute converter field will be null for EnhancedDocument.
User should supply the Attribute converters for the EnhancedDocument.

Q: What converter providers will be used for EnhancedDocuments retrieved from DynamoDB using the SDK's get/scan/query operations?<br>
A: The DefaultAttributeConverterProviders will be assigned by default.

Q: What converter providers will be used for EnhancedDocuments created by the user?<br>
A: The converter providers supplied by the user in the EnhancedDocument builders. If no converter providers are provided,
the user will get an error while trying to access the attribute values. 
Therefore, the user should always supply defaultConverterProviders while creating EnhancedDocuments for which they want 
to access the attribute values later.


#### Getters and Setters for EnhancedDocuments

Q: What kind of getter and setter API would be available for EnhancedDocument?<br>
1. There are class-specific getters/setters, such as getString(), getNumber(), and getStringSet(), among others.
2. There is a generic getter API that can be used for any EnhancedType, where users will need to pass the EnhancedType in the API arguments.

Q: How can user access Attributes of Unknown Types?<br>
A: Unlike 1.x where we had APIs like [get](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/document/Item.html#get-java.lang.String-) ,
[getRawMap](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/dynamodbv2/document/Item.html#getRawMap-java.lang.String-)
will not be available in version 2.x. I
Instead, users can pass EnhancedType as AttributeValue.class to retrieve the values as AttributeValue.
```java
AttributeValue unknownTypeAttribute = document.get("sample_key", EnhancedType.of(AttributeValue.class));
Map<String,AttributeValue> unknownTypeAttributeMap = document.getMap("sample_key", EnhancedType.of(String.class), EnhancedType.of(AttributeValue.class))

```

## Appendix B: Alternative solutions

### Design alternative: Using existing Document APIs from Sdk-core
This design approach enhances the software.amazon.awssdk.core.document.Document with additional APIs enhancing it for 
better experience writing and reading open content.

#### Reading flat structures from DynamoDB
~~~java
Document document = table.getItem(Key.builder().partitionValue("0").build());
// Current Document API
String id = document.asMap().get("id").asString();
Instant time = Instant.parse(document.asMap().get("time").asString());

// Document Converters API
String id = document.get("id", String.class);
Instant time = document.get("time", Instant.class);

// Document JSON API
String json = document.toJsonString();

~~~

**Decision**

This alternative was discarded since it doesnot make use of existing converter providers.
The Document will be required to be converted from key-AttributeValue map to Documents , for which new jsonNode converters
needs to be implemented.