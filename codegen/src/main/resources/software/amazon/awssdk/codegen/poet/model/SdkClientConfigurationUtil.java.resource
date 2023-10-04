
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.internal.SdkInternalTestAdvancedClientOption;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.ScheduledExecutorUtils;

@SdkInternalApi
public final class SdkClientConfigurationUtil {
    private SdkClientConfigurationUtil() {
    }

    /**
     * Copies the {@link ClientOverrideConfiguration} values to the configuration builder.
     */
    public static SdkClientConfiguration.Builder copyOverridesToConfiguration(ClientOverrideConfiguration overrides,
                                                                              SdkClientConfiguration.Builder builder) {
        // misc
        setClientOption(builder, SdkClientOption.ADDITIONAL_HTTP_HEADERS, overrides.headers());
        setClientOption(builder, SdkClientOption.RETRY_POLICY, overrides.retryPolicy());
        setClientOption(builder, SdkClientOption.API_CALL_TIMEOUT, overrides.apiCallTimeout());
        setClientOption(builder, SdkClientOption.API_CALL_ATTEMPT_TIMEOUT, overrides.apiCallAttemptTimeout());
        setClientOption(builder, SdkClientOption.SCHEDULED_EXECUTOR_SERVICE,
                        overrides.scheduledExecutorService()
                                 .map(ScheduledExecutorUtils::unmanagedScheduledExecutor));
        setClientListOption(builder, SdkClientOption.EXECUTION_INTERCEPTORS, overrides.executionInterceptors());
        setClientOption(builder, SdkClientOption.EXECUTION_ATTRIBUTES, overrides.executionAttributes());

        // advanced option
        Signer signer = overrides.advancedOption(SdkAdvancedClientOption.SIGNER).orElse(null);
        if (signer != null) {
            builder.option(SdkAdvancedClientOption.SIGNER, signer);
            builder.option(SdkClientOption.SIGNER_OVERRIDDEN, true);
        }
        setClientOption(builder, SdkAdvancedClientOption.USER_AGENT_SUFFIX,
                        overrides.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX));
        setClientOption(builder, SdkAdvancedClientOption.USER_AGENT_PREFIX,
                        overrides.advancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX));
        setClientOption(builder, SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION,
                        overrides.advancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION));
        overrides.advancedOption(SdkInternalTestAdvancedClientOption.ENDPOINT_OVERRIDDEN_OVERRIDE).ifPresent(value -> {
            builder.option(SdkClientOption.ENDPOINT_OVERRIDDEN, value);
        });

        // profile
        Supplier<ProfileFile> profileFileSupplier =
            OptionalUtils.firstPresent(overrides.defaultProfileFileSupplier(),
                                       () -> overrides.defaultProfileFile().map(ProfileFileSupplier::fixedProfileFile))
                         .orElse(null);

        setClientOption(builder, SdkClientOption.PROFILE_FILE_SUPPLIER, profileFileSupplier);
        setClientOption(builder, SdkClientOption.PROFILE_NAME, overrides.defaultProfileName());

        // misc
        setClientListOption(builder, SdkClientOption.METRIC_PUBLISHERS, overrides.metricPublishers());
        setClientOption(builder, SdkAdvancedClientOption.TOKEN_SIGNER,
                        overrides.advancedOption(SdkAdvancedClientOption.TOKEN_SIGNER));
        setClientOption(builder, SdkClientOption.CONFIGURED_COMPRESSION_CONFIGURATION, overrides.compressionConfiguration());

        return builder;
    }

    static <T> void setClientOption(SdkClientConfiguration.Builder builder, ClientOption<T> option, T newValue) {
        if (newValue != null) {
            T oldValue = builder.option(option);
            if (oldValue == null || !oldValue.equals(newValue)) {
                builder.option(option, newValue);
            }
        }
    }

    static <T> void setClientOption(SdkClientConfiguration.Builder builder, ClientOption<T> option, Optional<T> newValueOpt) {
        setClientOption(builder, option, newValueOpt.orElse(null));
    }

    static <T> void setClientListOption(SdkClientConfiguration.Builder builder, ClientOption<List<T>> option, List<T> newValue) {
        if (newValue == null || newValue.isEmpty()) {
            return;
        }
        List<T> oldValue = builder.option(option);
        if (oldValue == null || oldValue.isEmpty()) {
            builder.option(option, newValue);
            return;
        }
        // Make sure that we don't override derived values or duplicate existing ones.
        List<T> result = new ArrayList<>(oldValue);
        Set<T> dedup = new HashSet<>();
        dedup.addAll(oldValue);
        for (T value : newValue) {
            if (!dedup.contains(value)) {
                result.add(value);
            }
        }
        builder.option(option, result);
    }
}
