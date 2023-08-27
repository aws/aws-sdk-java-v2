package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class AsyncS3ResponseTransformer {
    public static AsyncResponseTransformer<GetObjectResponse, ResponseBytes<GetObjectResponse>> toBytes() {
        return AsyncResponseTransformer.toBytes(r -> r.contentLength().intValue());
    }
}
