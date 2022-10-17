package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class DynamoDbRetryPolicyTest {

    private EnvironmentVariableHelper environmentVariableHelper;

    @BeforeEach
    public void setup() {
        environmentVariableHelper = new EnvironmentVariableHelper();
    }

    @AfterEach
    public void reset() {
        environmentVariableHelper.reset();
    }

    @Test
    public void test_numRetries_with_standardRetryPolicy() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "standard");
        final SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        final RetryPolicy retryPolicy = DynamoDbRetryPolicy.resolveRetryPolicy(sdkClientConfiguration);
        assertThat(retryPolicy.numRetries()).isEqualTo(8);
    }

    @Test
    public void test_numRetries_with_legacyRetryPolicy() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "legacy");
        final SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        final RetryPolicy retryPolicy = DynamoDbRetryPolicy.resolveRetryPolicy(sdkClientConfiguration);
        assertThat(retryPolicy.numRetries()).isEqualTo(8);
    }

    @Test
    public void test_backoffBaseDelay_with_standardRetryPolicy() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "standard");
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        RetryPolicy retryPolicy = DynamoDbRetryPolicy.resolveRetryPolicy(sdkClientConfiguration);
        BackoffStrategy backoffStrategy = retryPolicy.backoffStrategy();

        assertThat(backoffStrategy).isInstanceOfSatisfying(FullJitterBackoffStrategy.class, fjbs -> {
            assertThat(fjbs.toBuilder().baseDelay()).isEqualTo(Duration.ofMillis(25));
        });
    }

}
