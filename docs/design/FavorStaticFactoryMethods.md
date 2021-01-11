**Design:** Convention, **Status:** [Accepted](README.md)

## Favor Static Factory Methods Over Constructors

This page describes the structures and conventions used to initialize a class.

### Static Factory Methods vs. Constructors
Static factory methods are preferable than constructors for the following reasons:
- Static factory methods provide meaningful names compared with constructors, which improves the readability of the codes by describing the objects being returned.
```java
// With static factory method, giving a hint that a foobar provider with default settings is being created.
FoobarProvider defaultProvider = FoobarProvider.defaultFoobarProvider(); 

// With constructor
FoobarProvider defaultProvider = new FoobarProvider(); 
```
- They are useful when work with immutable classes as we can reuse the same object instead of creating new object every time when it is invoked. 
- Static factory methods can return any subtype of that class and this gives us flexibility to write a separate factory class to return different subclasses if needed.

There are a few disadvantages of static factory methods:
- It might not be straightforward for the users to use static factory method to create new instances compared with constructors, but this can be improved by providing a set of common method names such as `create()` and smart IDEs such as IntelliJ and Eclipse can also give hints and help auto-completing.
- Classes without public or protected constructors cannot be subclassed, but this could encourage us to use composition instead of inheritance. 

In general, we should favor static factory methods over constructors.

### Example
```java
public class DefaultCredentialsProvider implements AwsCredentialsProvider, SdkAutoCloseable {

    private static final DefaultCredentialsProvider DEFAULT_CREDENTIALS_PROVIDER = new DefaultCredentialsProvider(builder());

    private DefaultCredentialsProvider(Builder builder) {
        this.providerChain = createChain(builder);
    }

    public static DefaultCredentialsProvider create() {
        return DEFAULT_CREDENTIALS_PROVIDER;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder { 
      // ...
    }
    // ...
}


// There are two ways to create new instance
DefaultCredentialsProvider defaultCredentialsProvider1 = DefaultCredentialsProvider.create();
DefaultCredentialsProvider defaultCredentialsProvider2 = DefaultCredentialsProvider.builder().build;
```
### Naming Conventions
The naming conventions for the static factory methods:
- `create()`, `create(params)` when creating a new instance
eg: `DynamoDBClient.create()`
- `defaultXXX()` when returning an instance with default settings.
eg: `BackoffStrategy.defaultStrategy()`
