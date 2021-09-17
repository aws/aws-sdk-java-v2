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
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.conditions.TokenBucketRetryCondition;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.OptionalUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A retry mode is a collection of retry behaviors encoded under a single value. For example, the {@link #LEGACY} retry mode will
 * retry up to three times, and the {@link #STANDARD} will retry up to two times.
 *
 * <p>
 * While the {@link #LEGACY} retry mode is specific to Java, the {@link #STANDARD} retry mode is standardized across all of the
 * AWS SDKs.
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
     *
     * <p>
     * This is the retry mode that is used when no other mode is configured.
     */
    LEGACY,


    /**
     * The STANDARD retry mode, shared by all AWS SDK implementations, and characterized by:
     * <ol>
     *     <li>Up to 2 retries, regardless of service.</li>
     *     <li>Throttling exceptions are treated the same as other exceptions for the purposes of the
     *     {@link TokenBucketRetryCondition}.</li>
     * </ol>
     */
    STANDARD,

    /**
     * Adaptive retry mode builds on {@code STANDARD} mode.
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
     */
    ADAPTIVE,

    ;

    /**
     * Retrieve the default retry mode by consulting the locations described in {@link RetryMode}, or LEGACY if no value is
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
        private static final RetryMode SDK_DEFAULT_RETRY_MODE = LEGACY;
        
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
         * Resolve which retry mode should be used, based on the configured values.
         */
        public RetryMode resolve() {
            return OptionalUtils.firstPresent(Resolver.fromSystemSettings(), () -> fromProfileFile(profileFile, profileName))
                                .orElseGet(this::fromDefaultMode);
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
                    return Optional.of(ADAPTIVE);
                default:
                    throw new IllegalStateException("Unsupported retry policy mode configured: " + string);
            }
        }

        private RetryMode fromDefaultMode() {
            return defaultRetryMode != null ? defaultRetryMode : SDK_DEFAULT_RETRY_MODE;
        }
    }
}
