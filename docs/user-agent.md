# User Agent

## Additional Metadata

The table below documents additional metadata the SDK may include in the `User-Agent` header of a request. These take the form of `md/[name]#[value]` in the request.

|Name|Value(s)|Description|
|---|---|---|
|`rb`||The request body implementation. This includes `ContentStreamProvider` for sync clients, and `AsyncRequestBody` for async clients.|
||`f`|"File". The body implementation reads from a file.|
||`b`|"Bytes". The body implementation reads from a byte array.|
||`c`|"String" The body implementation reads from a string. |
||`s`|"Stream". The body implementation reads from an `InputStream`.|
||`p`|"Publisher". The body implementation reads from an `SdkPublisher`.|
||`u`|"unknown"|
|`rt`||The response transformer implementation. This includes `ResponseTransformer` for sync clients, and `AsyncResponseTransformer` for async clients.|
||`f`|"File". The response transformer writes the response body to a file.|
||`b`|"Bytes". The response transformer writes the response body to a byte array.|
||`s`|"Stream". The response transformer adapts the response body to an `InputStream`.|
||`p`|"Publisher". The response transformer adapts the response body to an `SdkPublisher`.|
||`u`|"unknown"|
