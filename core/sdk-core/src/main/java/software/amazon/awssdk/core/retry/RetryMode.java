/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.core.retry;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.conditions.TokenBucketRetryCondition;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A retry mode is a collection of retry behaviors encoded under a single value. For example, the {@link #LEGACY} retry mode will
 * retry up to three times, and the {@link #STANDARD} will retry up to two times for most services.
 *
 * <p>
 * While the {@link #LEGACY} retry mode is specific to Java, the {@link #STANDARD} retry mode is standardized across all of the
 * AWS SDKs.
 *
 * <p>
 * The {@link #STANDARD} retry mode is used when no other mode is configured.
 *
 * <p>
 * The retry mode can be configured:
 * <ol>
 *     <li>Directly on a client via {@link ClientOverrideConfiguration.Builder#retryPolicy(RetryMode)}.</li>
 *     <li>Directly on a client via a combination of {@link RetryPolicy#builder(RetryMode)} or
 *     {@link RetryPolicy#forRetryMode(RetryMode)}, and {@link ClientOverrideConfiguration.Builder#retryPolicy(RetryPolicy)}</li>
 *     <li>On a configuration profile via the "retry_mode" profile file property.</li>
 *     <li>Globally via the "aws.retryMode" system property.</li>
 *     <li>Globally via the "AWS_RETRY_MODE" environment variable.</li>
 * </ol>
 */
@SdkPublicApi
public enum RetryMode {
    /**
     * The LEGACY retry mode, specific to the Java SDK, and characterized by:
     * <ol>
     *     <li>Up to 3 retries, or more for services like DynamoDB (which has up to 8).</li>
     *     <li>Zero token are subtracted from the {@link TokenBucketRetryCondition} when throttling exceptions are encountered.
     *     </li>
     * </ol>
     */
    LEGACY,


    /**
     * The STANDARD retry mode, shared by all AWS SDK implementations. When used with a retry strategy, it follows retry
     * behavior 2.1, characterized by:
     * <ol>
     *     <li>Up to 2 retries, or more for services like DynamoDB (which has up to 3). The {@code max_attempts} profile
     *     property, the {@code aws.maxAttempts} system property and the {@code AWS_MAX_ATTEMPTS} environment variable
     *     override this.</li>
     *     <li>A 50 ms base backoff, or 1 s for throttling errors.</li>
     *     <li>Each retry costs 14 tokens from the retry quota, or 5 for throttling errors.</li>
     * </ol>
     *
     * <p>
     * When used with a {@link RetryPolicy}, it keeps its previous behavior: up to 2 retries, and throttling exceptions are
     * treated the same as other exceptions for the purposes of the {@link TokenBucketRetryCondition}.
     *
     * <p>
     * This is the retry mode that is used when no other mode is configured.
     */
    STANDARD,

    /**
     * Adaptive retry mode builds on {@link #STANDARD} mode.
     * <p>
     * Adaptive retry mode dynamically limits the rate of AWS requests to maximize success rate. This may be at the
     * expense of request latency. Adaptive retry mode is not recommended when predictable latency is important.
     * <p>
     * <b>Warning:</b> Adaptive retry mode assumes that the client is working against a single resource (e.g. one
     * DynamoDB Table or one S3 Bucket). If you use a single client for multiple resources, throttling or outages
     * associated with one resource will result in increased latency and failures when accessing all other resources via
     * the same client. When using adaptive retry mode, we recommend using a single client per resource.
     *
     * @see RetryPolicy#isFastFailRateLimiting()
     * @deprecated As of 2.25.xx, replaced by {@link #ADAPTIVE_V2}. The ADAPTIVE implementation has a bug that prevents it
     * from remembering its state across requests which is needed to correctly estimate its sending rate. Given that
     * this bug has been present since its introduction and that correct version might change the traffic patterns of the SDK we
     * deemed too risky to fix this implementation.
     */
    @Deprecated
    ADAPTIVE,

    /**
     * Adaptive V2 retry mode builds on {@link #STANDARD} mode.
     * <p>
     * Adaptive retry mode dynamically limits the rate of AWS requests to maximize success rate. This may be at the
     * expense of request latency. Adaptive V2 retry mode is not recommended when predictable latency is important.
     * <p>
     * {@code ADAPTIVE_V2} mode differs from {@link #ADAPTIVE} mode in the computed delays between calls, including the first 
     * attempt
     * that might be delayed if the algorithm considers that it's needed to increase the odds of a successful response.
     * <p>
     * <b>Warning:</b> Adaptive V2 retry mode assumes that the client is working against a single resource (e.g. one
     * DynamoDB Table or one S3 Bucket). If you use a single client for multiple resources, throttling or outages
     * associated with one resource will result in increased latency and failures when accessing all other resources via
     * the same client. When using adaptive retry mode, we recommend using a single client per resource.
     */
    ADAPTIVE_V2,

    ;

    /**
     * Retrieve the default retry mode by consulting the locations described in {@link RetryMode}, or STANDARD if no value is
     * configured.
     */
    public static RetryMode defaultRetryMode() {
        return resolver().resolve();
    }

    /**
     * Create a {@link Resolver} that allows customizing the variables used during determination of a {@link RetryMode}.
     */
    public static Resolver resolver() {
        return new Resolver();
    }

    /**
     * Allows customizing the variables used during determination of a {@link RetryMode}. Created via {@link #resolver()}.
     */
    public static class Resolver {
        private static final Logger LOG = Logger.loggerFor(RetryMode.class);
        private static final AtomicBoolean NEW_RETRIES_2026_WARNING_LOGGED = new AtomicBoolean(false);

        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private RetryMode defaultRetryMode;

        private Resolver() {
        }

        /**
         * Configure the profile file that should be used when determining the {@link RetryMode}. The supplier is only consulted
         * if a higher-priority determinant (e.g. environment variables) does not find the setting.
         */
        public Resolver profileFile(Supplier<ProfileFile> profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        /**
         * Configure the profile file name should be used when determining the {@link RetryMode}.
         */
        public Resolver profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Configure the {@link RetryMode} that should be used if the mode is not specified anywhere else.
         */
        public Resolver defaultRetryMode(RetryMode defaultRetryMode) {
            this.defaultRetryMode = defaultRetryMode;
            return this;
        }

        /**
         * This setting is ignored. The standard and adaptive retry modes use retry behavior 2.1, and the SDK default retry
         * mode is {@link RetryMode#STANDARD}.
         *
         * @deprecated The standard and adaptive retry modes use retry behavior 2.1. This method has no effect.
         */
        @Deprecated
        public Resolver defaultNewRetries2026(Boolean defaultNewRetries2026) {
            return this;
        }

        /**
         * Resolve which retry mode should be used, based on the configured values.
         */
        public RetryMode resolve() {
            warnIfNewRetries2026SettingIsSet();
            return OptionalUtils.firstPresent(Resolver.fromSystemSettings(), () -> fromProfileFile(profileFile, profileName))
                                .orElseGet(this::fromDefaultMode);
        }

        @SuppressWarnings("deprecation")
        private static void warnIfNewRetries2026SettingIsSet() {
            if (NEW_RETRIES_2026_WARNING_LOGGED.get()) {
                return;
            }

            Optional<String> value = SdkSystemSetting.AWS_NEW_RETRIES_2026.getStringValue();
            if (!value.isPresent() || !NEW_RETRIES_2026_WARNING_LOGGED.compareAndSet(false, true)) {
                return;
            }

            // "true" asks for what is now the default, so only a different value means the setting no longer has an effect
            String message = "The AWS_NEW_RETRIES_2026 environment variable and aws.newRetries2026 system property are "
                             + "ignored. The standard and adaptive retry modes now use retry behavior 2.1. To use the "
                             + "previous default retry behavior, set the retry mode to legacy, for example with "
                             + "AWS_RETRY_MODE=legacy.";
            if ("true".equalsIgnoreCase(value.get().trim())) {
                LOG.debug(() -> message);
            } else {
                LOG.warn(() -> message);
            }
        }

        private static Optional<RetryMode> fromSystemSettings() {
            return SdkSystemSetting.AWS_RETRY_MODE.getStringValue()
                                                  .flatMap(Resolver::fromString);
        }

        private static Optional<RetryMode> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
            profileFile = profileFile != null ? profileFile : ProfileFile::defaultProfileFile;
            profileName = profileName != null ? profileName : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
            return profileFile.get()
                              .profile(profileName)
                              .flatMap(p -> p.property(ProfileProperty.RETRY_MODE))
                              .flatMap(Resolver::fromString);
        }

        private static Optional<RetryMode> fromString(String string) {
            if (string == null || string.isEmpty()) {
                return Optional.empty();
            }

            switch (StringUtils.lowerCase(string)) {
                case "legacy":
                    return Optional.of(LEGACY);
                case "standard":
                    return Optional.of(STANDARD);
                case "adaptive":
                    return Optional.of(ADAPTIVE_V2);
                default:
                    throw new IllegalStateException("Unsupported retry policy mode configured: " + string);
            }
        }

        private RetryMode fromDefaultMode() {
            return defaultRetryMode != null ? defaultRetryMode : sdkDefaultRetryMode();
        }

        /**
         * The SDK default retry mode, used when no mode is configured anywhere else.
         */
        private RetryMode sdkDefaultRetryMode() {
            return STANDARD;
        }
    }
}
