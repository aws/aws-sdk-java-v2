package software.amazon.awssdk.stability.tests.s3;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.stability.tests.exceptions.StabilityTestsRetryableException;
import software.amazon.awssdk.stability.tests.utils.RetryableTest;

import java.time.Duration;

public class S3NettyAsyncStabilityTest extends S3AsyncBaseStabilityTest {

    private static String bucketName = "s3nettyasyncstabilitytests" + System.currentTimeMillis();

    private static S3AsyncClient s3NettyClient;

    static {
        s3NettyClient = S3AsyncClient.builder()
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .maxConcurrency(CONCURRENCY))
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(10))
                        // Retry at test level
                        .retryPolicy(RetryPolicy.none()))
                .build();
    }

    public S3NettyAsyncStabilityTest() {
        super(s3NettyClient);
    }

    @BeforeAll
    public static void setup() {
        s3NettyClient.createBucket(b -> b.bucket(bucketName)).join();
    }

    @AfterAll
    public static void cleanup() {
        deleteBucketAndAllContents(s3NettyClient, bucketName);
        s3NettyClient.close();
    }

    @Override
    protected String getTestBucketName() { return bucketName; }

    @RetryableTest(maxRetries = 3, retryableException = StabilityTestsRetryableException.class)
    public void getBucketAcl_lowTpsLongInterval_Netty() {
        doGetBucketAcl_lowTpsLongInterval();
    }
}
