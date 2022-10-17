**Design:** Convention, **Status:** [Accepted](README.md)

## API Reference

This page describes a general guideline for API Reference.

### Definition
- SDK public APIs: all classes/interfaces annotated with `@SdkPublicApi`
- SDK protected APIs: all classes/interfaces annotated with `@SdkProtectedApi`
- SDK internal APIs: all classes/interfaces annotated with `@SdkInternalApi`

### General Guideline

- All SDK public APIs MUST have documentation.
- All public methods in SDK public APIs MUST have documentation except the ones that implement or override a method in an interface or superclass.
- For SDK protected APIs and internal APIs, documentation is recommended but not required.
- Javadocs for SDK public APIs in high level libraries such as DynamoDB Enhanced Client SHOULD include code snippets.

### Style guideline

- Javadoc MUST be properly formatted following the [Javadoc standard](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html).
- A single `<p>` MUST be used between paragraphs.
- Use `@link` and `@see` tags to point to related methods/classes.
- Use `@throws` to specify exceptions that could be thrown from a method.
- Use `@snippet` to add code snippets. [External code snippets](https://docs.oracle.com/en/java/javase/18/code-snippet/index.html#external-snippets) SHOULD be favored over inline code snippets.
- Use `@deprecated` tag for deprecated methods. The replacement MUST be documented and linked using `{@link}`.
- Avoid using `@version` and `@since` tags.

