import org.junit.Test;
import software.amazon.awssdk.core.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.AwsS3V4Signer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


public class S3SignerTest {

    @Test
    public void test() {

        String bucket = "list-objects-integ-test-varunkn-57";
        Region region = Region.US_WEST_2;

        S3Client s3Client = S3Client.builder()
                                    .region(region)
                                    .overrideConfiguration(ClientOverrideConfiguration
                                                               .builder()
                                                               .build())
                                    .build();

        s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build())
                .contents()
                .forEach(o -> System.out.println(o.key()));
    }


    @Test
    public void testPutObject() {
        String bucket = "list-objects-integ-test-varunkn-57";
        Region region = Region.US_WEST_2;

        S3Client s3Client = S3Client.builder()
                                    .region(region)
                                    .overrideConfiguration(ClientOverrideConfiguration
                                                               .builder()
                                                               .advancedOption(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.builder()
                                                                                                                            .build())
                                                               .build())
                                    .build();


        s3Client.putObject(PutObjectRequest.builder()
                                           .bucket(bucket)
                                           .key("foofoo")
                                           .build(),
                           RequestBody.fromString("hello world!"));
    }
}
