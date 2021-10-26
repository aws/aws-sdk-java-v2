package software.amazon.awssdk.defaultsmode;

import java.time.Duration;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Contains a collection of default configuration options for each DefaultsMode
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultsModeConfiguration {
    private static final AttributeMap STANDARD_DEFAULTS = AttributeMap.builder()
                                                                      .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD).build();

    private static final AttributeMap STANDARD_HTTP_DEFAULTS = AttributeMap.builder()
                                                                           .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(2000)).build();

    private static final AttributeMap MOBILE_DEFAULTS = AttributeMap.builder()
                                                                    .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.ADAPTIVE).build();

    private static final AttributeMap MOBILE_HTTP_DEFAULTS = AttributeMap.builder()
                                                                         .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(10000)).build();

    private static final AttributeMap CROSS_REGION_DEFAULTS = AttributeMap.builder()
                                                                          .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD).build();

    private static final AttributeMap CROSS_REGION_HTTP_DEFAULTS = AttributeMap.builder()
                                                                               .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(2800)).build();

    private static final AttributeMap IN_REGION_DEFAULTS = AttributeMap.builder()
                                                                       .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD).build();

    private static final AttributeMap IN_REGION_HTTP_DEFAULTS = AttributeMap.builder()
                                                                            .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(1000)).build();

    private static final AttributeMap LEGACY_DEFAULTS = AttributeMap.empty();

    private static final AttributeMap LEGACY_HTTP_DEFAULTS = AttributeMap.empty();

    private DefaultsModeConfiguration() {
    }

    /**
     * Return the default HTTP config options for a given defaults mode
     */
    public static AttributeMap defaultHttpConfig(DefaultsMode mode) {
        switch (mode) {
            case STANDARD:
                return STANDARD_HTTP_DEFAULTS;
            case MOBILE:
                return MOBILE_HTTP_DEFAULTS;
            case CROSS_REGION:
                return CROSS_REGION_HTTP_DEFAULTS;
            case IN_REGION:
                return IN_REGION_HTTP_DEFAULTS;
            case LEGACY:
                return LEGACY_HTTP_DEFAULTS;
            default:
                throw new IllegalArgumentException("Unsupported mode: " + mode);
        }
    }

    /**
     * Return the default SDK config options for a given defaults mode
     */
    public static AttributeMap defaultConfig(DefaultsMode mode) {
        switch (mode) {
            case STANDARD:
                return STANDARD_DEFAULTS;
            case MOBILE:
                return MOBILE_DEFAULTS;
            case CROSS_REGION:
                return CROSS_REGION_DEFAULTS;
            case IN_REGION:
                return IN_REGION_DEFAULTS;
            case LEGACY:
                return LEGACY_DEFAULTS;
            default:
                throw new IllegalArgumentException("Unsupported mode: " + mode);
        }
    }
}
