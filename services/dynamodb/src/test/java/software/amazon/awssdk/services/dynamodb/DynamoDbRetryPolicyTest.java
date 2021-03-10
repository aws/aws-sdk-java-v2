package software.amazon.awssdk.services.dynamodb;

import org.junit.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

import static org.assertj.core.api.Assertions.assertThat;

public class DynamoDbRetryPolicyTest {

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @Test
    public void test_numRetries_with_standardRetryPolicy() {
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "standard");
        System.setProperty(ProfileFileSystemSetting.AWS_PROFILE.property(), "default");
        final SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        final RetryPolicy retryPolicy = DynamoDbRetryPolicy.resolveRetryPolicy(sdkClientConfiguration);
        assertThat(retryPolicy.numRetries()).isEqualTo(8);
    }

    @Test
    public void test_numRetries_with_legacyRetryPolicy() {
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "legacy");
        System.setProperty(ProfileFileSystemSetting.AWS_PROFILE.property(), "default");
        final SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        final RetryPolicy retryPolicy = DynamoDbRetryPolicy.resolveRetryPolicy(sdkClientConfiguration);
        assertThat(retryPolicy.numRetries()).isEqualTo(8);
    }


}
