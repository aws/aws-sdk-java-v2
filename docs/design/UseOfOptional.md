**Design:** Convention, **Status:** [Accepted](README.md)

## Use of Optional

This page describes general guidelines of how we use
`java.util.Optional`.

- `Optional` should be used when it isn't obvious to a caller whether a
  result will be null.
- `Optional` must not be used when the result will never be
  `Optional.empty()`.
- `Optional` must not be used for instance (in `@SdkPublicApi`s).
- `Optional` must not be used for parameters (in `@SdkPublicApi`s).
- `Optional` must not be used when the caller may know whether a value
  is present. As a consequence:
  - `Optional` must not be used in classes with "setters" and "getters",
    like Builders or POJOs.

References:

- http://blog.joda.org/2015/08/java-se-8-optional-pragmatic-approach.html
