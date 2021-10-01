**Design:** Convention, **Status:** [Accepted](README.md)

## Use of Optional

This page describes a general guideline for the use of
`java.util.Optional` in the AWS SDK for Java 2.x.

- `Optional` SHOULD be used when it isn't obvious to a caller whether a
  result will be null, e.g., 
- `Optional` MUST not be used when the result will never be null.
- `Optional` SHOULD not be used for member variables, e.g., `private final Optional<String> field;`
- `Optional` MUST not be used for method parameters, e.g., `private void test(Optional<String> value)`
- `Optional` MUST not be used as a return type for "getters" in generated service model classes such as service Builders or POJOs.

References:

- http://blog.joda.org/2015/08/java-se-8-optional-pragmatic-approach.html
