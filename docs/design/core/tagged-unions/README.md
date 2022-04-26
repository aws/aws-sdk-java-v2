**Design:** New Feature, **Status:** [Proposed](../../README.md)

# Tagged Unions

## Motivation
By defining a structure that requires a single member to be set at any given time, service teams have been defining tagged unions in AWS services for years. However, the AWS SDK for Java has yet to provide first-class support for them.

The quintessential example of a tagged union is DynamoDB's `AttributeValue`:

```java
class AttributeValue {
    private String s;
    private String n;
    private SdkBytes b;
    private List<String> ss;
    private List<String> ns;
    private List<SdkBytes> bs;
    
    // ...
}
```

Currently, customers are left to perform ad-hoc matching on the value to first determine what kind of value is provided for a union, and then dispatch to branching code to interact with the value. Rather than ask customers to do this themselves, we should provide abstractions that make it easy for customers to use these values.

## Goal

The goal of this project is to provide a backwards-compatible customer-friendly abstraction for existing and new union types within the 2.x SDK.

## Prior Art

We currently implement friendly tagged unions in at least two external places within the SDK already:
1. The `software.amazon.awssdk.core.document.Document` type.
2. Event stream types, like Lex's `software.amazon.awssdk.services.lexruntimev2.model.StartConversationRequestEventStream` type.

And at least one internal place:
1. The `software.amazon.awssdk.protocols.jsoncore.JsonNode` type.

## Union Type Definition

We will follow the patterns set by `Document` and `JsonNode`, because the pattern used by `StartConversationRequestEventStream` would not be backwards-compatible with existing types.

Existing structures primarily contain the following methods:
1. A static `builder()` method for creating the structure and setting its mutually exclusive members.
2. "Get" methods for each member in the union.
3. "Has" methods for each collection or map member, to allow for differentiating between absent and missing values.

In addition to the above methods, new and existing union types will have the following additional methods:
1. A static `T fromN(...)` method for creating the structure with its single exclusive member initialized. e.g. `AttributeValue s = AttributeValue.fromS("string")`
2. A `X.Type type()` method for determining the type contained within the union. e.g. `AttributeValue.Type type = getItemResponse.item().type()`

For example, an `AttributeValue` union type containing `String s`, `String n` and `SdkBytes b` would have the following public methods:
```java
class AttributeValue {
    static AttributeValue.Builder builder();
    static fromS(String s);
    static fromN(String n);
    static fromB(SdkBytes b);
    
    String s();
    String n();
    SdkBytes b();

    AttributeValue.Type type();
    
    enum Type {
        S,
        N,
        B,
        UNKNOWN_TO_SDK_VERSION
    }
    
    class Builder {
        // ...
    }
    
    // ...
}
```

## Customer Experience

The following section outlines example customer code before and after the union types changes:

### Creating a union type

**Before Changes**
```java
AttributeValue.builder().s("foo").build()
```

**After Changes**
```java
AttributeValue.fromS("foo")
```

### Reading a field with an unknown type

**Before Changes**
```java
String result;
if (attributeValue.s() != null) { result = attributeValue.s(); }
else if (attributeValue.n() != null) { result = attributeValue.n(); }
else if (attributeValue.b() != null) { result = attributeValue.b().asUtf8String(); }
else { result = null; }
```

**After Changes**
```java
// (Java 17+)
String result = switch (attributeValue.type()) {
    case S -> attributeValue.s();
    case N -> attributeValue.n();
    case B -> attributeValue.b().asUtf8String();
    default -> null;
}

// Java (8-16)
String result = null;
switch (attributeValue.type()) {
    case S: result = attributeValue.s(); break;
    case N: result = attributeValue.n(); break;
    case B: result = attributeValue.b().asUtf8String(); break;
}
```

### Using a field with an unknown type

**Before Changes**
```java
if (attributeValue.s() != null) { System.out.println(attributeValue.s()); }
else if (attributeValue.n() != null) { System.out.println(attributeValue.n()); }
else if (attributeValue.b() != null) { System.out.println(attributeValue.b().asUtf8String()); }
```

**After Changes**
```java
attributeValue.visit(new AttributeValue.VoidVisitor() {
    void visitS(String s) { System.out.println(s); }
    void visitN(String n) { System.out.println(n); }
    void visitB(SdkBytes b) { System.out.println(b.asUtf8String()); }
)

// (Java 17+)
System.out.println(switch (attributeValue.type()) {
    case S -> attributeValue.s();
    case N -> attributeValue.n();
    case B -> attributeValue.b().asUtf8String();
    default -> null;
});

// Java (8-16)
switch (attributeValue.type()) {
    case S: System.out.println(attributeValue.s()); break;
    case N: System.out.println(attributeValue.n()); break;
    case B: System.out.println(attributeValue.b().asUtf8String()); break;
}
```