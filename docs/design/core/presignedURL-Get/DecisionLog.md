
# S3 Pre-signed URL GET - Decision Log

## Review Meeting: 06/17/2024
**Attendees**: Alban, John, Zoe, Dongie, Bole, Ran, Saranya

### Closed Decisions

1. **Response Type:** Create a new PresignedUrlGetObjectResponse specifically for pre-signed URLs, or use the existing GetObjectResponse?
   - Decided to use the existing GetObjectResponse for pre-signed URL operations as the HTTP response from a pre-signed URL GET is same as a standard S3 GetObject response.

2. **Exception Handling:** Use the existing SDK and S3 exceptions or implement specialized exceptions for validation errors like expired URLs?
   - Decided to utilize existing SDK exceptions rather than creating specialized ones for pre-signed URL operations.

3. **Validation Strategy:** Provide additional client-side validation with server-side validation as fallback or just rely entirely on server-side validation from S3?
   - No additional client-side validation will be implemented for pre-signed URLs.

### Discussions Addressed

1. **Credential Signing Bypass:** Are there alternative methods to skip signing, such as using NoOpSigner(), instead of setting additional Execution attributes?
   - Added the use of NoOpSigner() in the design doc.

2. **Checksum Support:** Does the S3 response include a checksum? If so, should checksum-related support be implemented in this project, or deferred until after Transfer Manager support is added?
   - S3 Response doesn't include checksum.

3. **Helper API Naming:** What should we name the Helper API?
   - Options include PresignedURLManager or PresignedUrlExtension.
   - Will be addressed in the Surface API Review.

## Review Meeting: 06/23/2024
**Attendees**: John, Zoe, Dongie, Bole, Ran, Saranya, Alex, David

### Decisions Addressed

1. **Request Design Pattern:** Should PresignedUrlGetObjectRequest extend S3Request/SdkRequest?
   - Decided to use a standalone request class with minimal parameters (presignedUrl, rangeStart, rangeEnd) to avoid exposing incompatible configurations like credentials and signers. Internally convert to S3Request for ClientHandler compatibility.

2. **Endpoint Resolution Bypass:** Replace IS_DISCOVERED_ENDPOINT execution attribute with a more semantically appropriate solution.
   - Decided to introduce new SKIP_ENDPOINT_RESOLUTION execution attribute specifically for presigned URL scenarios where endpoint resolution should be bypassed, as IS_DISCOVERED_ENDPOINT is tied to deprecated endpoint discovery feature.

3. **Range Parameter Design:** Use separate rangeStart/rangeEnd fields vs single range string parameter.
   - Decided to use separate rangeStart and rangeEnd Long fields for better user experience, as start/end is more intuitive than string parsing.

