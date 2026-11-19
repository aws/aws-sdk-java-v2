package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.internal.retry.RetryPolicyAdapter;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetryStrategy;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.retries.LegacyRetryStrategy;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.AcquireInitialTokenRequest;
import software.amazon.awssdk.retries.api.RefreshRetryTokenRequest;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.retries.api.RetryToken;
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
        assertThat(retryStrategy.maxAttempts()).isEqualTo(4);
    }

    @Test
    void test_numRetries_with_noRetryConfiguration() {
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(emptyProfileConfig().build());
        assertThat(retryStrategy).isInstanceOf(StandardRetryStrategy.class);
        assertThat(retryStrategy.maxAttempts()).isEqualTo(4);
    }

    @Test
    void test_numRetries_with_legacyRetryPolicy_ignoresMaxAttemptsSetting() {
        environmentVariableHelper.set(SdkSystemSetting.AWS_RETRY_MODE.environmentVariable(), "legacy");
        environmentVariableHelper.set(SdkSystemSetting.AWS_MAX_ATTEMPTS.environmentVariable(), "7");
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(SdkClientConfiguration.builder().build());
        assertThat(retryStrategy).isInstanceOf(LegacyRetryStrategy.class);
        assertThat(retryStrategy.maxAttempts()).isEqualTo(9);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void test_numRetries_maxAttemptsEnvVar_overridesDefault(RetryMode mode) {
        environmentVariableHelper.set(SdkSystemSetting.AWS_MAX_ATTEMPTS.environmentVariable(), "7");
        SdkClientConfiguration config = emptyProfileConfig().option(SdkClientOption.DEFAULT_RETRY_MODE, mode).build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(config);
        assertThat(SdkDefaultRetryStrategy.retryMode(retryStrategy)).isEqualTo(mode);
        assertThat(retryStrategy.maxAttempts()).isEqualTo(7);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void test_numRetries_maxAttemptsSystemProperty_overridesDefault(RetryMode mode) {
        System.setProperty(SdkSystemSetting.AWS_MAX_ATTEMPTS.property(), "6");
        try {
            SdkClientConfiguration config = emptyProfileConfig().option(SdkClientOption.DEFAULT_RETRY_MODE, mode).build();
            assertThat(DynamoDbRetryPolicy.resolveRetryStrategy(config).maxAttempts()).isEqualTo(6);
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_MAX_ATTEMPTS.property());
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void test_numRetries_maxAttemptsInClientProfile_overridesDefault(RetryMode mode) {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile custom]\n"
                                                                            + "max_attempts = 5"))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        SdkClientConfiguration config = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, () -> profileFile)
            .option(SdkClientOption.PROFILE_NAME, "custom")
            .option(SdkClientOption.DEFAULT_RETRY_MODE, mode)
            .build();
        assertThat(DynamoDbRetryPolicy.resolveRetryStrategy(config).maxAttempts()).isEqualTo(5);
    }

    @Test
    @SuppressWarnings("deprecation")
    void resolve_adaptiveMode_returnsRetryPolicyAdapter() {
        SdkClientConfiguration config = emptyProfileConfig().option(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.ADAPTIVE)
                                                            .build();
        assertThat(DynamoDbRetryPolicy.resolveRetryStrategy(config)).isInstanceOf(RetryPolicyAdapter.class);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = RetryMode.class, names = {"STANDARD", "ADAPTIVE_V2"})
    void resolve_retries21Strategy_usesDynamoDbBaseDelayAndClampsSuggestedDelay(RetryMode mode) {
        SdkClientConfiguration config = emptyProfileConfig().option(SdkClientOption.DEFAULT_RETRY_MODE, mode).build();
        RetryStrategy retryStrategy = DynamoDbRetryPolicy.resolveRetryStrategy(config);

        // 25ms base delay: the first backoff is at most 25ms
        for (int i = 0; i < 32; ++i) {
            assertThat(refreshDelay(retryStrategy, null)).isBetween(Duration.ZERO, Duration.ofMillis(25));
        }

        // 2.1 clamps the suggested delay to at most the backoff plus 5 seconds, even after toBuilder()
        assertThat(refreshDelay(retryStrategy, Duration.ofSeconds(60)))
            .isBetween(Duration.ofSeconds(5), Duration.ofSeconds(5).plusMillis(25));
        RetryStrategy rebuilt = retryStrategy.toBuilder().maxAttempts(6).build();
        assertThat(refreshDelay(rebuilt, Duration.ofSeconds(60)))
            .isBetween(Duration.ofSeconds(5), Duration.ofSeconds(5).plusMillis(25));
    }

    private static Duration refreshDelay(RetryStrategy strategy, Duration suggestedDelay) {
        RetryToken token = strategy.acquireInitialToken(AcquireInitialTokenRequest.create("test")).token();
        RefreshRetryTokenRequest.Builder refresh = RefreshRetryTokenRequest.builder()
                                                                           .token(token)
                                                                           .failure(SdkServiceException.builder()
                                                                                                       .statusCode(500)
                                                                                                       .build());
        if (suggestedDelay != null) {
            refresh.suggestedDelay(suggestedDelay);
        }
        return strategy.refreshRetryToken(refresh.build()).delay();
    }

    private static SdkClientConfiguration.Builder emptyProfileConfig() {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(new StringInputStream("[profile default]\n"))
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();
        return SdkClientConfiguration.builder()
                                     .option(SdkClientOption.PROFILE_FILE_SUPPLIER, () -> profileFile)
                                     .option(SdkClientOption.PROFILE_NAME, "default");
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

        assertThat(retryMode).isEqualTo(RetryMode.STANDARD);
    }

    @ParameterizedTest(name = "DEFAULT_NEW_RETRIES_2026 = {0}")
    @CsvSource({
        "false",
        "true"
    })
    @SuppressWarnings("deprecation")
    void resolve_defaultNewRetries2026_isIgnored(boolean retries21) {
        SdkClientConfiguration cfg = emptyProfileConfig().option(SdkClientOption.DEFAULT_NEW_RETRIES_2026, retries21)
                                                         .build();
        RetryStrategy strategy = DynamoDbRetryPolicy.resolveRetryStrategy(cfg);

        assertThat(strategy).isInstanceOf(StandardRetryStrategy.class);
        assertThat(strategy.maxAttempts()).isEqualTo(4);
    }
}
