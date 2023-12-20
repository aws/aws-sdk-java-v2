package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.Arrays;
import java.util.Collection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RunWith(Parameterized.class)
public class DeleteObjectsIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(DeleteObjectsIntegrationTest.class);
    private static final String XML_KEY = "<Key>objectId</Key>";
    private static final String ENCODED_KEY = "&lt;Key&gt;objectId&lt;/Key&gt;";
    private static final String MIXED_KEY = "&lt;<";

    @BeforeClass
    public static void init() {
        createBucket(BUCKET);
    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { XML_KEY },
            { ENCODED_KEY },
            { MIXED_KEY }
        });
    }

    private final String key;

    public DeleteObjectsIntegrationTest(String key) {
        this.key = key;
    }

    @Test
    public void deleteObjects_shouldCorrectlyDeleteObjectsWithSpecifiedKeys() {
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(key)
                                     .build(), RequestBody.fromString("contents"));

        // Does not throw exception
        s3.headObject(r -> r.bucket(BUCKET).key(key));

        Delete delete = Delete.builder().objects(o -> o.key(key)).build();
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().bucket(BUCKET).delete(delete).build();
        DeleteObjectsResponse response = s3.deleteObjects(request);

        assertThat(response.deleted().get(0).key()).isEqualTo(key);
        assertThrows(NoSuchKeyException.class, () -> s3.headObject(r -> r.bucket(BUCKET).key(key)));
    }
}
