package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
import software.amazon.awssdk.retries.api.TokenAcquisitionFailedException;
import software.amazon.awssdk.retries.internal.DefaultRetryToken;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.StringInputStream;

class DynamoDbRetryPolicyTest {

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
    void test_numRetries_with_standardRetryPolicy() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "standard");
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(sdkClientConfiguration);
        assertThat(retryStrategy.maxAttempts()).isEqualTo(9);
    }

    @Test
    void test_numRetries_with_legacyRetryPolicy() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "legacy");
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(sdkClientConfiguration);
        assertThat(retryStrategy.maxAttempts()).isEqualTo(9);
    }

    @Test
    void resolve_retryModeSetInEnv_doesNotCallSupplier() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "standard");
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration.builder().build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(sdkClientConfiguration);
        RetryMode retryMode = SdkDefaultRetryStrategy.retryMode(retryStrategy);
        assertThat(retryMode).isEqualTo(RetryMode.STANDARD);
    }

    @Test
    void resolve_retryModeSetWithEnvAndSupplier_resolvesFromEnv() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "standard");
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile default]\n"
                                                                            + "retry_mode = adaptive"))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, () -> profileFile)
            .option(SdkClientOption.PROFILE_NAME, "default")
            .build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(sdkClientConfiguration);
        RetryMode retryMode = SdkDefaultRetryStrategy.retryMode(retryStrategy);

        assertThat(retryMode).isEqualTo(RetryMode.STANDARD);
    }

    @Test
    void resolve_retryModeSetWithSupplier_resolvesFromSupplier() {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile default]\n"
                                                                            + "retry_mode = adaptive"))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, () -> profileFile)
            .option(SdkClientOption.PROFILE_NAME, "default")
            .build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(sdkClientConfiguration);
        RetryMode retryMode = SdkDefaultRetryStrategy.retryMode(retryStrategy);

        assertThat(retryMode).isEqualTo(RetryMode.ADAPTIVE_V2);
    }

    @Test
    void resolve_retryModeSetWithSdkClientOption_resolvesFromSdkClientOption() {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile default]\n"))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, () -> profileFile)
            .option(SdkClientOption.PROFILE_NAME, "default")
            .option(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD)
            .build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(sdkClientConfiguration);
        RetryMode retryMode = SdkDefaultRetryStrategy.retryMode(retryStrategy);

        assertThat(retryMode).isEqualTo(RetryMode.STANDARD);
    }

    @Test
    void resolve_retryModeNotSetWithEnvNorSupplier_resolvesFromSdkDefault() {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile default]\n"))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        SdkClientConfiguration sdkClientConfiguration = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, () -> profileFile)
            .option(SdkClientOption.PROFILE_NAME, "default")
            .build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(sdkClientConfiguration);
        RetryMode retryMode = SdkDefaultRetryStrategy.retryMode(retryStrategy);

        assertThat(retryMode).isEqualTo(RetryMode.LEGACY);
    }

    @ParameterizedTest(name = "V2.1 retries = {0}, max attempts = {1}")
    @CsvSource({
        "false,9",
        "true,4"
    })
    void resolve_maxAttemptsCompliantWithRetriesVersion(boolean retries21, int expectedMaxAttempts) {
        SdkClientConfiguration cfg = SdkClientConfiguration.builder()
                                                           .option(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD)
                                                           .option(SdkClientOption.DEFAULT_NEW_RETRIES_2026, retries21)
                                                           .build();
        RetryStrategy strategy = DynamoDbRetryPolicy.resolveRetryStrategy(cfg);

        // sanity check
        assertThat(strategy).isInstanceOf(StandardRetryStrategy.class);

        assertMaxAttempts(strategy, expectedMaxAttempts);
    }

    void assertMaxAttempts(RetryStrategy strategy, int maxAttempts) {
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();

        SdkServiceException err = SdkServiceException.builder().statusCode(500).build();
        for (int i = 0; i < maxAttempts - 1; ++i) {
            token = strategy.refreshRetryToken(RefreshRetryTokenRequest.builder()
                                                                       .token(token)
                                                                       .failure(err)
                                                                       .build())
                            .token();
        }

        RetryToken finalToken = token;
        assertThatThrownBy(() -> strategy.refreshRetryToken(RefreshRetryTokenRequest.builder()
                                                                                    .token(finalToken)
                                                                                    .failure(err)
                                                                                    .build()))
            .matches(e -> {
                TokenAcquisitionFailedException acquireFailure = (TokenAcquisitionFailedException) e;
                DefaultRetryToken defaultRetryToken = (DefaultRetryToken) acquireFailure.token();
                return defaultRetryToken.state() == DefaultRetryToken.TokenState.MAX_RETRIES_REACHED;
            }, "Token state is MAX_RETRIES_REACHED");
    }
}
