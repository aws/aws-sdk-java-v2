**Design:** Convention, **Status:** [Accepted](README.md)

## Use of Optional

This page describes a general guideline for the use of
`java.util.Optional` in the AWS SDK for Java 2.x.

- `Optional` MUST NOT be used under any circumstances when the result will never be null.
- For return types,
  - `Optional` SHOULD be used when it is not obvious to a caller whether a
  result will be null, e.g, `public <T> Optional<T> getValueForField(String fieldName, Class<T> clazz)`in [SdkResponse.java](https://github.com/aws/aws-sdk-java-v2/blob/aa161c564c580ced4a0381d3ed7d4d13120916fc/core/sdk-core/src/main/java/software/amazon/awssdk/core/SdkResponse.java#L59-L61)
  - `Optional` MUST NOT be used for "getters" in generated service model classes such as service Builders or POJOs.
- For member variables: `Optional` SHOULD NOT be used, e.g., `private final Optional<String> field;`
- For method parameters: `Optional` MUST NOT be used, e.g., `private void test(Optional<String> value)`


References:

- http://blog.joda.org/2015/08/java-se-8-optional-pragmatic-approach.html
