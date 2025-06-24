
# S3 Pre-signed URL GET - Decision Log

## Review Meeting: 06/17/2024
**Attendees**: Alban, John, Zoe, Dongie, Bole, Ran, Saranya

### Closed Decisions

1. **Response Type**: Use existing `GetObjectResponse` (HTTP response identical to standard S3 GetObject)
2. **Exception Handling**: Utilize existing SDK exceptions rather than specialized ones
3. **Validation Strategy**: No additional client-side validation for pre-signed URLs

### Discussions Addressed

1. **Credential Signing Bypass**: Added `NoOpSigner()` usage in design
2. **Checksum Support**: S3 response doesn't include checksum for presigned URLs
3. **Helper API Naming**: Options include `PresignedURLManager` vs `PresignedUrlExtension` (TBD in Surface API Review)

## Review Meeting: 06/23/2024
**Attendees**: John, Zoe, Dongie, Bole, Ran, Saranya, Alex, David

### Decisions Addressed

1. **Request Design Pattern**: Use standalone request class with minimal parameters (presignedUrl, rangeStart, rangeEnd) to avoid exposing incompatible configurations. Convert internally to S3Request for ClientHandler compatibility.

2. **Endpoint Resolution Bypass**: Introduce new `SKIP_ENDPOINT_RESOLUTION` execution attribute specifically for presigned URL scenarios, replacing `IS_DISCOVERED_ENDPOINT` which is tied to deprecated endpoint discovery feature.

3. **Range Parameter Design**: Use separate `rangeStart` and `rangeEnd` Long fields for better user experience over string parsing.
