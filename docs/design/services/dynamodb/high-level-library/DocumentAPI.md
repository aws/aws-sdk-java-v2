**Design:** New Feature, **Status:** Design

## Problem

In DynamoDB, an [item](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/WorkingWithItems.html) is a 
collection of attributes, each of which has a name and a value. Aws-sdk-java 1.x  provided Document API to access these 
items. The user could access these items without actually knowing the complete schema of the entire item. This feature 
did not exist in Aws-sdk-java 2.x. This document proposes mechanism by which user will be able to access the DDB items 
as documents using enhanced dynamodb client.

### Requested features
Aws-sdk-java 2.x should provide Document API similar to that of aws-sdk-java 1.x with following APIs

1. APIs to access DynamoDB for complicated data models without having to use DynamoDB Mapper. 
   For example, APIs for converting from JSON to DynamoDb items & vice versa.
2. APIs to manipulate semi structured data for each of the attribute values.  
   For example, APIs to access the AttributeValue as string sets, number sets,  string list, number list etc.
3. Allow direct read and write of dynamoDB elements as Documents.

Example Github issue: https://github.com/aws/aws-sdk-java-v2/issues/36

## Current functionality
Aws-sdk-java 2.x currently supports Mid-level DynamoDB mapper/abstraction for Java by providing Mapper Clients.
While using these mappers the user needs to define the complete Table schema at the time of mapped table creation.

## Naming conventions
Please note that the names of classes and api mentioned in this design document  are not final and might get changed
based on future reviews

## Proposed Solution

Add a new DocumentSchema in existing enhanced client. This schema just needs the primary key and sort key to be defined
at the time of mapped table creation. This will retrieve the dynamo db table items as Documents.
User can then access the attribute values from these documents by getters.
Similarly , user can create a new Document and insert it to the mapped table by using the builder apis of Document.

### Enhanced Client Table Schema creation API
The DocumentSchema is created by supplying partitionKey, sortKey and optional attributeConverterProviders in the builder.
If AttributeConverterProvider are not supplied in TableSchema the Document Table schema will use the default converter
providers.
~~~java
 // Existing way of creating enhanced client
 DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().build();

// New API in TableSchema to create a DocumentTableSchema 
DocumentTableSchema documentTableSchema =
    TableSchema.fromDocumentSchemaBuilder()
               .addIndexPartitionKey(primaryIndexName(), "sample_hash_name", AttributeValueType.S)
               .addIndexSortKey("gsi_index", "sample_sort_name", AttributeValueType.N)
               .addAttributeConverterProviders(cutomAttributeConverters)
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
                                                    .addString("sample_hash_name", "sample_value")
                                                    .build();
// Retrieving from existing Get operation.
EnhancedDocument retrievedDocument = documentTable.getItem(hashKeyDocument);
    
// Retrieving from existing Get operation
EnhancedDocument documentTableItem = documentTable.getItem(
                                        EnhancedDocument.builder()
                                                        .addString("sample_hash_name", "sample_value")
                                                        .build());

// Accessing an attribute from document using generic getter.
Number sampleSortvalue = documentTableItem.get("sample_sort_name", EnhancedType.of(Number.class));

// Accessing an attribute from document using specific getters.
sampleSortvalue = documentTableItem.getSdkNumber("sample_sort_name"); 

// Accessing an attribute of custom class using custom converters.
CustomClass customClass = documentTableItem.get("custom_nested_map", new CustomAttributeConverter()));

// Accessing Nested set 
Set<List<String>> stringSet = documentTableItem.get("string_set", new EnhancedType<Set<List<<String>>>(){}));
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
                                                       .addString("sample_hash_name", "sample_value_2")
                                                       .addNumber("sample_sort_name", 111)
                                                       .addNumberList("sample_names", 1 ,2 ,3, 4)
                                                       .build();
    
// put to dynamo db table
documentTable.documentFromBuilder(documentFromBuilder);

// retrieving a document from dynamo db and updating some attributes
EnhancedDocument documentTableItem = documentTable.getItem(hashKeyDocument);
// using toBuilder to make a copy of the retrieved item and then modifying the key attribute
EnhancedDocument changedValue = documentTableItem.toBuilder().addString("key-to-change", "changedValue").build();
// put to dynamo db table
documentTable.putItem(changedValue);
~~~


#### Attribute converter providers for EnhancedDocument

A builder method would be provided to add Attribute converters for an EnhancedDocument.
The default value of attribute converter field would be null for EnhancedDocument.

Q: What converter providers will be used for EnhancedDocument retrieved from Dynamo db?<br>
A: For the EnhancedDocuments retrieved from the SDK get/scan/query operations the DefaultAttributeConverterProviders would
be assigned by default. If the user has provided attribute converter providers at the time of table creation then these
converters will be used.

Q: What converter providers  would be used for EnhancedDocument created by the user ?<br>
A: The converter providers supplied by the user in the EnhancedDocument builders. If no converter providers are provided
then user will get error while trying to get the attribute values. Thus, user should always supply defaultConverterProviders 
while creating the EnhancedDocuments for which user wants to access the attriute values latter.


#### Getters and Setters for EnhancedDocuments

Q: What kind of getter API would be available for EnhancedDocument?<br>
A: Following getter API would be available in EnhancedDocument
 1. Class specific getters like getString(), getNumber, getMap() same as V1.
 2. Generic getter API for any EnhancedType.
 3. Getters with ConverterProviders in ares

Q: What kind of setter API would be available for EnhancedDocument?<br>
A: Following builder API would be available in EnhancedDocument
1. Class specific builder like addString(), addNumber, addMap() same as V1.
2. Generic builder API for any EnhancedType like add(value, EnhancedType).
3. Builders for custom classes with Custom converter providers/

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