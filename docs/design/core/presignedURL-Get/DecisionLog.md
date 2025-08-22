
# S3 Pre-signed URL GET - Decision Log

## Review Meeting: 06/17/2025
**Attendees**: Alban Gicquel, John Viegas, Zoe Wang, Dongie Agnir, Bole Yi, Ran Vaknin, Saranya Somepalli

### Closed Decisions

1. Create a new PresignedUrlGetObjectResponse specifically for pre-signed URLs, or use the existing GetObjectResponse? Decided to use the existing GetObjectResponse for pre-signed URL operations as the HTTP response from a pre-signed URL GET is same as a standard S3 GetObject response.

2. Use the existing SDK and S3 exceptions or implement specialized exceptions for validation errors like expired URLs? Decided to utilize existing SDK exceptions rather than creating specialized ones for pre-signed URL operations.

3. Provide additional client-side validation with server-side validation as fallback or just rely entirely on server-side validation from S3? No additional client-side validation will be implemented for pre-signed URLs.

### Discussions Addressed

1. Are there alternative methods to skip signing, such as using NoOpSigner(), instead of setting additional Execution attributes? Added the use of NoOpSigner() in the design doc.

2. Does the S3 response include a checksum? If so, should checksum-related support be implemented in this project, or deferred until after Transfer Manager support is added? S3 Response doesn't include checksum.

3. What should we name the Helper API? Options include PresignedURLManager or PresignedUrlExtension. Will be addressed in the Surface API Review.

## Review Meeting: 06/23/2025
**Attendees**: John Viegas, Zoe Wang, Dongie Agnir, Bole Yi, Ran Vaknin, Saranya Somepalli, David Ho

### Decisions Addressed

1. Should PresignedUrlGetObjectRequest extend S3Request/SdkRequest? Decided to use a standalone request class with minimal parameters (presignedUrl, rangeStart, rangeEnd) to avoid exposing incompatible configurations like credentials and signers. Internally convert to S3Request for ClientHandler compatibility.

2. Replace IS_DISCOVERED_ENDPOINT execution attribute with a more semantically appropriate solution. Decided to introduce new SKIP_ENDPOINT_RESOLUTION execution attribute specifically for presigned URL scenarios where endpoint resolution should be bypassed, as IS_DISCOVERED_ENDPOINT is tied to deprecated endpoint discovery feature.

3. Use separate rangeStart/rangeEnd fields vs single range string parameter. Decided to use separate rangeStart and rangeEnd Long fields for better user experience, as start/end is more intuitive than string parsing.

## Decision Poll Meeting: 06/30/2025
**Attendees**: John Viegas, Zoe Wang, Dongie Agnir, Bole Yi, Ran Vaknin, Saranya Somepalli, David Ho, Alex Woods

### Decision Addressed
Decided to use String range field for Request object to support all RFC 7233 formats including suffix ranges (bytes=-100) and future multi-range support, since S3 currently doesn't support multiple ranges but may in the future without requiring SDK changes.

## Post-standup Meeting: 07/14/2025 
**Attendees**: Alban Gicquel, John Viegas, Zoe Wang, Dongie Agnir, Bole Yi, Ran Vaknin, Saranya Somepalli, David Ho, Alex Woods

### Decision Addressed
The team has decided to implement functionality only for S3 async client and defer S3 sync client implementation for now. This decision was made because implementing S3 sync client would require supporting multipart download capabilities. 

## API Surface Area Review: 07/21/2025
**Attendees**: John Viegas, Zoe Wang, Dongie Agnir, Bole Yi, Ran Vaknin, Saranya Somepalli, David Ho, Alex Woods

### Decisions Addressed

1. Decided on the naming for the surface APIs - AsyncPresignedUrlExtension for the new core API, presignedUrlExtension() for the method to call from the AsyncS3Client and PresignedUrlDownloadRequest for the Get Object Request. Also decided to have PresignedUrlDownload as the Operation Name for metric collection.

2. Remove the consumer builder pattern from Get Request Model.

3. throw UnsupportedOperationException for Multipart S3 Client and S3 CRT Client for now.

## Design Review: 07/31/2025
**Source**: Design : Multipart Download Support for Pre-signed URLs  
**Attendees**: John Viegas, Zoe Wang, Dongie Agnir, Bole Yi, Ran Vaknin, Saranya Somepalli, David Ho, Olivier Lepage Applin

### Decision Addressed
Initially considered separate discovery request (bytes=0-0) followed by download, but decided to follow AWS Transfer Manager SEP specification using Range: bytes=0-{partSizeInBytes-1} to download first part AND discover total object size simultaneously from Content-Range header response.

## Transfer Manager Integration Review: 08/19/2025
**Attendees**: John Viegas, Zoe Wang, Dongie Agnir, Alex Woods, Bole Yi, Olivier Lepage Applin, Ran Vaknin, David Ho, Saranya Somepalli

### Decisions Addressed

1. **API Names**: Finalize `downloadWithPresignedUrl` and `downloadFileWithPresignedUrl` for Transfer Manager methods during Surface API Review later.

2. **Pause/Resume Support**: Decided not to support pause/resume capability for presigned URL downloads, maintaining consistency with AWS SDK for Java v1 which also lacks this feature for presigned URLs.
