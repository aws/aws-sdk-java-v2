# DynamoDB Enhanced Clients

The enhanced Amazon DynamoDB client replaces the generated DynamoDB
client with one that is easier for a Java customer to use. It does this
by converting Java types into DynamoDB types.

For example, the client makes it possible to persist a Book object in
DynamoDB, without needing to manually convert the fields (e.g.
List<Author>) into DynamoDB-specific types (e.g. List<Map<String,
AttributeValue>>).

# Current Features

1. Synchronous and asynchronous (nonblocking) clients.
2. Writing a single item to DynamoDB.
3. Reading a single item from DynamoDB.

# Installation

The enhanced client is currently in preview, and **is subject to change
in backward-incompatible ways**. For this reason, it's not currently
released to Maven.

You must manually install the client in your local Maven repository to
use it in your application.

**Step 1:** Check out the project from GitHub.

```bash
git clone git@github.com:aws/aws-sdk-java-v2.git -b dynamodb-enhanced
``` 

**Step 2:** Compile and install the project to your local Maven
repository. You must run this command from the directory to which you
checked out the project.

```bash
mvn clean install -pl :dynamodb-enhanced -Pquick
```

**Step 3:** Add a dependency on the `dynamodb-enhanced` project to your
pom.xml file:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb-enhanced</artifactId>
    <version>preview-SNAPSHOT</version>
</dependency>
```

# Getting Started

The enhanced DynamoDB client is still in preview, and is changing
constantly. Until the client is released, you can refer to the following
for usage examples:

* [The issue for this feature](https://github.com/aws/aws-sdk-java-v2/issues/35):
  Here you can see development updates and code samples.
* [The `DynamoDbEnhancedClient` source and JavaDocs](https://github.com/aws/aws-sdk-java-v2/blob/dynamodb-enhanced/services-custom/dynamodb-enhanced/src/main/java/software/amazon/awssdk/enhanced/dynamodb/DynamoDbEnhancedClient.java):
  Here you can see up-to-date code samples using the
  `DynamoDbEnhancedClient`.
* [The `DynamoDbEnhancedAsyncClient` source and JavaDocs](https://github.com/aws/aws-sdk-java-v2/blob/dynamodb-enhanced/services-custom/dynamodb-enhanced/src/main/java/software/amazon/awssdk/enhanced/dynamodb/DynamoDbEnhancedAsyncClient.java):
  Here you can see up-to-date code samples using the
  `DynamoDbEnhancedAsyncClient`.

# Examples

### Writing a Book to DynamoDB

**Enhanced API Code**

```java
DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create();
Table books = client.table("books");

books.putItem(r -> r.putAttribute("isbn", "0-330-25864-8")
                    .putAttribute("title", "The Hitchhiker's Guide to the Galaxy")
                    .putAttribute("publicationDate", 
                                  p -> p.putAttribute("UK", Instant.parse("1979-10-12T00:00:00Z"))
                                        .putAttribute("US", Instant.parse("1980-01-01T00:00:00Z")))
                    .putAttribute("authors", Collections.singletonList("Douglas Adams")));
```

**Equivalent Generated API Code**

```java
DynamoDbClient client = DynamoDbClient.create();

String ukPublicationDate = Long.toString(Instant.parse("1979-10-12T00:00:00Z").toEpochMilli());
String usPublicationDate = Long.toString(Instant.parse("1980-01-01T00:00:00Z").toEpochMilli());

Map<String, AttributeValue> publicationDate = new LinkedHashMap<>();
publicationDate.put("UK", AttributeValue.builder().n(ukPublicationDate).build());
publicationDate.put("US", AttributeValue.builder().n(usPublicationDate).build());

Map<String, AttributeValue> requestItem = new LinkedHashMap<>();
requestItem.put("isbn", AttributeValue.builder().s("0-330-25864-8").build());
requestItem.put("title", AttributeValue.builder().s("The Hitchhiker's Guide to the Galaxy").build());
requestItem.put("publicationDate", AttributeValue.builder().m(publicationDate).build());
requestItem.put("authors", AttributeValue.builder().ss("Douglas Adams").build());

client.putItem(r -> r.tableName(TABLE).item(requestItem));
```

### Reading a Book from DynamoDB

**Enhanced API Code**

```java
DynamoDbEnhancedClient client = DynamoDbEnhancedClient.create();
Table books = client.table("books");

ResponseItem book = books.getItem(k -> k.putAttribute("isbn", "0-330-25864-8"));

String isbn = book.attribute("isbn").asString();
String title = book.attribute("title").asString();
Map<String, Instant> publicationDate = book.attribute("publicationDate")
                                           .asMap(String.class, Instant.class);
List<String> authors = book.attribute("authors").asList(String.class);
```

**Equivalent Generated API Code**

```java
DynamoDbClient client = DynamoDbClient.create();

Map<String, AttributeValue> key = new HashMap<>();
key.put("isbn", AttributeValue.builder().s("0-330-25864-8").build());

Map<String, AttributeValue> book = client.getItem(r -> r.tableName(TABLE).key(key)).item();

String isbn = book.get("isbn").s();
String title = book.get("title").s();
Map<String, Instant> publicationDates = new LinkedHashMap<>();
book.get("publicationDate").m().forEach((k, v) -> {
    publicationDates.put(k, Instant.ofEpochMilli(Long.parseLong(v.n())));
});
List<String> authors = new ArrayList<>();
book.get("authors").l().forEach(a -> authors.add(a.s()));
```