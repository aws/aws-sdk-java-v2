**Design:** Convention, **Status:** [Accepted](README.md)

## Use of Optional

This page describes a general guideline for the use of
`java.util.Optional` in the AWS SDK for Java 2.x.

- `Optional` SHOULD be used when it is not obvious to a caller whether a
  result will be null, e.g, `public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz)`in [SdkResponse.java](https://github.com/aws/aws-sdk-java-v2/blob/aa161c564c580ced4a0381d3ed7d4d13120916fc/core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkResponse.java#L59-L61)
- `Optional` MUST NOT be used when the result will never be null.
- `Optional` SHOULD NOT be used for member variables, e.g., `private final Optional<String> field;`
- `Optional` MUST NOT be used for method parameters, e.g., `private void test(Optional<String> value)`
- `Optional` MUST NOT be used as a return type for "getters" in generated service model classes such as service Builders or POJOs.

References:

- http://blog.joda.org/2015/08/java-se-8-optional-pragmatic-approach.html
