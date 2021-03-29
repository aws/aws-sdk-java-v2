package software.amazon.awssdk.custom.s3.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class DefaultUploadRequestTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void toApiRequest_usesGivenBucketAndKey() {
        String bucket = "bucket";
        String key = "key";

        UploadRequest request = UploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        assertThat(request.bucket()).isEqualTo(bucket);
        assertThat(request.key()).isEqualTo(key);
    }

    @Test
    public void upload_noRequestParamsProvided_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Exactly one");

        UploadRequest.builder().build();
    }

    @Test
    public void upload_bothBucketKeyPairAndRequestProvided_throws() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Exactly one");

        UploadRequest.builder()
                .bucket("bucket")
                .key("key")
                .apiRequest(PutObjectRequest.builder().build())
                .build();
    }

}
