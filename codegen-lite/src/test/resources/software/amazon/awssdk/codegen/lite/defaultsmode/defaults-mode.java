package software.amazon.awssdk.defaultsmode;

import java.util.Map;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.internal.EnumUtils;

/**
 * A defaults mode determines how certain default configuration options are resolved in the SDK. Based on the provided
 * mode, the SDK will vend sensible default values tailored to the mode for the following settings:
 * <ul>
 * <li>retryMode: PLACEHOLDER</li>
 * <li>s3UsEast1RegionalEndpoints: PLACEHOLDER</li>
 * <li>connectTimeoutInMillis: PLACEHOLDER</li>
 * <li>tlsNegotiationTimeoutInMillis: PLACEHOLDER</li>
 * </ul>
 * <p>
 * All options above can be configured by users, and the overridden value will take precedence.
 * <p>
 * <b>Note:</b> for any mode other than {@link #LEGACY}, the vended default values might change as best practices may
 * evolve. As a result, it is encouraged to perform testing when upgrading the SDK if you are using a mode other than
 * {@link #LEGACY}
 * <p>
 * While the {@link #LEGACY} defaults mode is specific to Java, other modes are standardized across all of the AWS SDKs
 * </p>
 * <p>
 * The defaults mode can be configured:
 * <ol>
 * <li>Directly on a client via {@code AwsClientBuilder.Builder#defaultsMode(DefaultsMode)}.</li>
 * <li>On a configuration profile via the "defaults_mode" profile file property.</li>
 * <li>Globally via the "aws.defaultsMode" system property.</li>
 * <li>Globally via the "AWS_DEFAULTS_MODE" environment variable.</li>
 * </ol>
 */
@SdkPublicApi
@Generated("software.amazon.awssdk:codegen")
public enum DefaultsMode {
    /**
     * PLACEHOLDER
     */
    LEGACY("legacy"),

    /**
     * PLACEHOLDER
     */
    STANDARD("standard"),

    /**
     * PLACEHOLDER
     */
    MOBILE("mobile"),

    /**
     * PLACEHOLDER
     */
    CROSS_REGION("cross-region"),

    /**
     * PLACEHOLDER
     */
    IN_REGION("in-region"),

    /**
     * PLACEHOLDER
     */
    AUTO("auto");

    private static final Map<String, DefaultsMode> VALUE_MAP = EnumUtils.uniqueIndex(DefaultsMode.class, DefaultsMode::toString);

    private final String value;

    private DefaultsMode(String value) {
        this.value = value;
    }

    /**
     * Use this in place of valueOf to convert the raw string returned by the service into the enum value.
     *
     * @param value
     *        real value
     * @return DefaultsMode corresponding to the value
     */
    public static DefaultsMode fromValue(String value) {
        Validate.paramNotNull(value, "value");
        if (!VALUE_MAP.containsKey(value)) {
            throw new IllegalArgumentException("The provided value is not a valid defaults mode " + value);
        }
        return VALUE_MAP.get(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
