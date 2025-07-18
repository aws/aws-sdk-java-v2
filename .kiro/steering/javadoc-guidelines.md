---
title: Javadoc Guidelines for AWS SDK v2
inclusion: fileMatch
fileMatchPattern: "**/*.java"
---

# Javadoc Guidelines for AWS SDK v2

## API Classification

The AWS SDK for Java v2 uses annotations to classify APIs:

- **SDK Public APIs**: Classes/interfaces annotated with `@SdkPublicApi`
- **SDK Protected APIs**: Classes/interfaces annotated with `@SdkProtectedApi`
- **SDK Internal APIs**: Classes/interfaces annotated with `@SdkInternalApi`

## Documentation Requirements

### Required Documentation

- All SDK public APIs **MUST** have documentation
- All public methods in SDK public APIs **MUST** have documentation except:
  - Methods that implement or override a method in an interface
  - Methods that override a method in a superclass
- For SDK protected APIs and internal APIs, documentation is recommended but not required
- Javadocs for SDK public APIs in high-level libraries (e.g., DynamoDB Enhanced Client) **SHOULD** include code snippets

## Style Guidelines

### General Formatting

- Javadoc **MUST** be properly formatted following the [Javadoc standard](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- A single `<p>` **MUST** be used at the beginning of paragraphs (except the first paragraph), with no closing tag, as shown in this example:

  ```java
  /**
   * First paragraph with no <p> tag.
   *
   * <p>Second paragraph starts with a <p> tag.
   *
   * <p>Third paragraph also starts with a <p> tag.
   */
  ```

- First sentence should be a summary of the method/class purpose
- Use complete sentences with proper punctuation
- Use third person (e.g., "Returns the value" not "Return the value")

### Javadoc Tags

- Use `@param` to document all parameters with clear descriptions
- Use `@return` to document return values (except for void methods)
- Use `@throws` to specify exceptions that could be thrown from a method
- Use `@link` and `@see` tags to point to related methods/classes
- Use `@deprecated` tag for deprecated methods, including:
  - When the method was deprecated
  - Why it was deprecated
  - The replacement method, linked using `{@link}`
- Avoid using `@version` and `@since` tags

### Code Snippets

- Use `@snippet` to add code snippets
- [External code snippets](https://docs.oracle.com/en/java/javase/18/code-snippet/index.html#external-snippets) **SHOULD** be favored over inline code snippets
- Code snippets should be:
  - Concise and focused on demonstrating the API
  - Compilable and correct
  - Well-commented to explain key points
  - Following the same code style as the rest of the codebase

## Examples

### Class Documentation Example

```java
/**
 * A high-level library for uploading and downloading objects to and from Amazon S3.
 * This can be created using the static {@link #builder()} method.
 *
 * <p>S3TransferManager provides a simplified API for efficient transfers between a local environment
 * and S3. It handles multipart uploads/downloads, concurrent transfers, progress tracking, and
 * automatic retries.
 *
 * <p>See {@link S3TransferManagerBuilder} for information on configuring an S3TransferManager.
 *
 * <p>Example usage:
 * {@snippet :
 *   S3TransferManager transferManager = S3TransferManager.builder()
 *       .s3ClientConfiguration(b -> b.credentialsProvider(credentialsProvider)
 *                                    .region(Region.US_WEST_2))
 *       .build();
 *
 *   // Upload a file
 *   UploadFileRequest uploadRequest = UploadFileRequest.builder()
 *       .putObjectRequest(req -> req.bucket("bucket").key("key"))
 *       .source(Paths.get("file.txt"))
 *       .build();
 *
 *   FileUpload upload = transferManager.uploadFile(uploadRequest);
 *   CompletedFileUpload uploadResult = upload.completionFuture().join();
 * }
 *
 * @see S3TransferManagerBuilder
 */
@SdkPublicApi
public interface S3TransferManager extends SdkAutoCloseable {
    // ...
}
```

### Method Documentation Example

```java
/**
 * Uploads a file from a specified path to an S3 bucket.
 *
 * <p>This method handles large files efficiently by using multipart uploads when appropriate.
 * Progress can be tracked through the returned {@link FileUpload} object.
 *
 * <p>Example:
 * {@snippet :
 *   UploadFileRequest request = UploadFileRequest.builder()
 *       .putObjectRequest(r -> r.bucket("bucket-name").key("key"))
 *       .source(Paths.get("my-file.txt"))
 *       .build();
 *   
 *   FileUpload upload = transferManager.uploadFile(request);
 *   CompletedFileUpload completedUpload = upload.completionFuture().join();
 * }
 *
 * @param request Object containing the bucket, key, and file path for the upload
 * @return A {@link FileUpload} object to track the upload and access the result
 * @throws S3Exception If any errors occur during the S3 operation
 * @throws SdkClientException If any client-side errors occur
 * @throws IOException If the file cannot be read
 */
FileUpload uploadFile(UploadFileRequest request);
```

### Deprecated Method Example

```java
/**
 * Returns the value of the specified header.
 *
 * <p>This method provides direct access to the header value.
 *
 * @param name The name of the header
 * @return The value of the specified header
 * @deprecated Use {@link #firstMatchingHeader(String)} instead, as it properly handles
 *             headers with multiple values.
 */
@Deprecated
String header(String name);
```

## References

- [Oracle Javadoc Guide](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
- [AWS SDK for Java v2 API Reference Convention](https://github.com/aws/aws-sdk-java-v2/blob/master/docs/design/APIReference.md)