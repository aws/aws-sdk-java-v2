package software.amazon.awssdk.defaultsmode;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.utils.AttributeMap;

/**
 * Contains a collection of default configuration options for each DefaultsMode
 */
@SdkInternalApi
@Generated("software.amazon.awssdk:codegen")
public final class DefaultsModeConfiguration {
    private static final AttributeMap STANDARD_DEFAULTS = AttributeMap.builder()
                                                                      .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD)
                                                                      .put(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional").build();

    private static final AttributeMap STANDARD_HTTP_DEFAULTS = AttributeMap.builder()
                                                                           .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(2000))
                                                                           .put(SdkHttpConfigurationOption.TLS_NEGOTIATION_TIMEOUT, Duration.ofMillis(2000)).build();

    private static final AttributeMap MOBILE_DEFAULTS = AttributeMap.builder()
                                                                    .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.ADAPTIVE)
                                                                    .put(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional").build();

    private static final AttributeMap MOBILE_HTTP_DEFAULTS = AttributeMap.builder()
                                                                         .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(10000))
                                                                         .put(SdkHttpConfigurationOption.TLS_NEGOTIATION_TIMEOUT, Duration.ofMillis(11000)).build();

    private static final AttributeMap CROSS_REGION_DEFAULTS = AttributeMap.builder()
                                                                          .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD)
                                                                          .put(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional").build();

    private static final AttributeMap CROSS_REGION_HTTP_DEFAULTS = AttributeMap.builder()
                                                                               .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(2800))
                                                                               .put(SdkHttpConfigurationOption.TLS_NEGOTIATION_TIMEOUT, Duration.ofMillis(2800)).build();

    private static final AttributeMap IN_REGION_DEFAULTS = AttributeMap.builder()
                                                                       .put(SdkClientOption.DEFAULT_RETRY_MODE, RetryMode.STANDARD)
                                                                       .put(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional").build();

    private static final AttributeMap IN_REGION_HTTP_DEFAULTS = AttributeMap.builder()
                                                                            .put(SdkHttpConfigurationOption.CONNECTION_TIMEOUT, Duration.ofMillis(1000))
                                                                            .put(SdkHttpConfigurationOption.TLS_NEGOTIATION_TIMEOUT, Duration.ofMillis(1000)).build();

    private static final AttributeMap LEGACY_DEFAULTS = AttributeMap.empty();

    private static final AttributeMap LEGACY_HTTP_DEFAULTS = AttributeMap.empty();

    private static final Map<DefaultsMode, AttributeMap> DEFAULT_CONFIG_BY_MODE = new EnumMap<>(DefaultsMode.class);

    private static final Map<DefaultsMode, AttributeMap> DEFAULT_HTTP_CONFIG_BY_MODE = new EnumMap<>(DefaultsMode.class);

    static {
        DEFAULT_CONFIG_BY_MODE.put(DefaultsMode.STANDARD, STANDARD_DEFAULTS);
        DEFAULT_CONFIG_BY_MODE.put(DefaultsMode.MOBILE, MOBILE_DEFAULTS);
        DEFAULT_CONFIG_BY_MODE.put(DefaultsMode.CROSS_REGION, CROSS_REGION_DEFAULTS);
        DEFAULT_CONFIG_BY_MODE.put(DefaultsMode.IN_REGION, IN_REGION_DEFAULTS);
        DEFAULT_CONFIG_BY_MODE.put(DefaultsMode.LEGACY, LEGACY_DEFAULTS);
        DEFAULT_HTTP_CONFIG_BY_MODE.put(DefaultsMode.STANDARD, STANDARD_HTTP_DEFAULTS);
        DEFAULT_HTTP_CONFIG_BY_MODE.put(DefaultsMode.MOBILE, MOBILE_HTTP_DEFAULTS);
        DEFAULT_HTTP_CONFIG_BY_MODE.put(DefaultsMode.CROSS_REGION, CROSS_REGION_HTTP_DEFAULTS);
        DEFAULT_HTTP_CONFIG_BY_MODE.put(DefaultsMode.IN_REGION, IN_REGION_HTTP_DEFAULTS);
        DEFAULT_HTTP_CONFIG_BY_MODE.put(DefaultsMode.LEGACY, LEGACY_HTTP_DEFAULTS);
    }

    private DefaultsModeConfiguration() {
    }

    /**
     * Return the default config options for a given defaults mode
     */
    public static AttributeMap defaultConfig(DefaultsMode mode) {
        return DEFAULT_CONFIG_BY_MODE.getOrDefault(mode, AttributeMap.empty());
    }

    /**
     * Return the default config options for a given defaults mode
     */
    public static AttributeMap defaultHttpConfig(DefaultsMode mode) {
        return DEFAULT_HTTP_CONFIG_BY_MODE.getOrDefault(mode, AttributeMap.empty());
    }
}
