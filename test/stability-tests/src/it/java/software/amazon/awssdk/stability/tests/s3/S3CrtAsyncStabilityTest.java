package software.amazon.awssdk.stability.tests.s3;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;

import java.time.Duration;

public class S3CrtAsyncStabilityTest extends S3BaseStabilityTest {

    private static String bucketName = "s3crtasyncstabilitytests" + System.currentTimeMillis();

    private static S3AsyncClient s3CrtClient;

    static {
        int numThreads = Runtime.getRuntime().availableProcessors();
        SdkAsyncHttpClient.Builder httpClientBuilder = AwsCrtAsyncHttpClient.builder()
                .connectionMaxIdleTime(Duration.ofSeconds(5));

        s3CrtClient = S3AsyncClient.builder()
                .httpClientBuilder(httpClientBuilder)
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(10))
                        // Retry at test level
                        .retryPolicy(RetryPolicy.none()))
                .build();
    }

    @BeforeAll
    public static void setup() {
        s3CrtClient.createBucket(b -> b.bucket(bucketName)).join();
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(s3CrtClient, bucketName);
        s3CrtClient.close();
    }

    @Override
    protected S3AsyncClient getTestClient() { return s3CrtClient; }

    @Override
    protected String getTestBucketName() { return bucketName; }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void putObject_getObject_highConcurrency() {
        putObject();
        getObject();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void largeObject_put_get_usingFile() {
        uploadLargeObjectFromFile();
        downloadLargeObjectToFile();
    }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void getBucketAcl_lowTpsLongInterval_Crt() {
        doGetBucketAcl_lowTpsLongInterval();
    }
}
