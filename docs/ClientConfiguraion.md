# Client Configuration

This page describes the structure and conventions used for client configuration objects. Client configuration objects are any objects used to configure an AWS client builder.

#### Example

This section walks through an example configuration class structure and describes each of its components at a high level.

```Java
/**
 * Configuration Description // (1)
 */
@Immutable
@ThreadSafe // (2)
public final class SdkConfiguration // (3)
        implements ToCopyableBuilder<SdkConfiguration.Builder, SdkConfiguration> { // (4)
    private final String option; // (5)

    /**
     * @see #builder() // (6)
     */
    private SdkClientConfiguration(DefaultSdkConfigurationBuilder builder) {
        this.option = builder.option;
    }

    public static Builder builder() {
        return new DefaultSdkConfigurationBuilder();
    }

    /**
     * @see #Builder#option(String) // (7)
     */
    public String option() {
        return this.option;
    }

    @Override
    public ClientHttpConfiguration.Builder toBuilder() {
        return builder().option(option);
    }

    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, SdkConfiguration> { // (8)
        /**
         * Configuration Option Description // (9)
         */
        Builder option(String option);
    }

    private static final class DefaultSdkConfigurationBuilder implements Builder { // (10)
        private String option;

        @Override
        public Builder option(String option) { // (11)
            this.option = option;
            return this;
        }

        public void setOption(String option) { // (12)
            this.option = option;
        }

        @Override
        public SdkConfiguration build() {
            return new SdkConfiguration(this);
        }
    }
}
```

1. A detailed description should be given of what types of options the user might find in this object.
2. Configuration objects should be `@Immutable`, and therefore `@ThreadSafe`.
3. Configuration classes should be defined as `final` to prevent extension.
4. Configuration classes should extend `ToCopyableBuilder` to ensure they can be converted back to a builder object.
5. All configuration fields should be defined as `private final` to prevent reassignment.
6. Configuration constructors should be `private` to enforce creation by the `Builder` and refer curious eyes to the `builder()` method for creating the object.
7. One "get" method should be created for each configuration field. This method's name should exactly match the name of the field it is retrieving.
8. Each builder should have its own interface. This allows hiding certain public methods from auto-complete (see below).
9. A detailed description of each option should be given, including what it does, and **why** a user could want to change its default value.
10. A `private static final` implementation of the `Builder` interface should be created that is not exposed outside of the scope of the configuration class.
11. One "set" method should be created for each option to mutate the value in this builder. This method's name should exactly match the  name of the field it is setting.
12. Each option should have a bean-style setter to allow configuring the object reflectively using `Inspector`-based frameworks, like spring XML.

#### Configuration Fields

This section details the semantics of configuration fields.

1. Configuration fields must be **immutable**.
    1. Fields must be marked as `final`.
    2. Mutable types, like `List` and `Set` should be wrapped in a type that prevents their modification (eg. `Collections.unmodifiableList`) when referenced through the "get" method.
    3. Mutable types, like `List` and `Set` should be copied in the "set" method to prevent their modification by mutating the original object.
2. Configuration fields must be **reference types**. Primitive types like `boolean` or `int` should not be used because they can not convey the absence of configuration.
3. Configuration field names should **not start with a verb** (eg. `config.enableRedirect()` should instead be `config.redirectEnabled()`). This is to avoid confusing their "get" methods for a mutating action.

Special notes for collection types, like `List`, `Set`, and `Map`:

1. Collection type field names must be plural.
2. Collection types should be accompanied by an `addX` method on the builder that permits adding one item to the collection. This `addX` method should be singular.

```Java
public interface Builder {
    Builder options(List<String> options);
    Builder addOption(String option);

    Builder headers(Map<String, String> headers);
    Builder addHeader(String key, String value);
}
