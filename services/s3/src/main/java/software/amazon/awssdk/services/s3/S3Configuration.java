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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.services.s3.internal.FieldWithDefault;
import software.amazon.awssdk.services.s3.internal.usearnregion.UseArnRegionProviderChain;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3Configuration implements ServiceConfiguration, ToCopyableBuilder<S3Configuration.Builder, S3Configuration> {
    /**
     * The default setting for use of path style addressing.
     */
    private static final boolean DEFAULT_PATH_STYLE_ACCESS_ENABLED = false;

    /**
     * S3 accelerate is by default not enabled
     */
    private static final boolean DEFAULT_ACCELERATE_MODE_ENABLED = false;

    /**
     * S3 dualstack endpoint is by default not enabled
     */
    private static final boolean DEFAULT_DUALSTACK_ENABLED = false;

    /**
     * S3 payload checksum validation is by default enabled;
     */
    private static final boolean DEFAULT_CHECKSUM_VALIDATION_ENABLED = true;

    /**
     * S3 The default value for enabling chunked encoding for {@link
     * software.amazon.awssdk.services.s3.model.PutObjectRequest} and {@link
     * software.amazon.awssdk.services.s3.model.UploadPartRequest}.
     */
    private static final boolean DEFAULT_CHUNKED_ENCODING_ENABLED = true;

    private final FieldWithDefault<Boolean> pathStyleAccessEnabled;
    private final FieldWithDefault<Boolean> accelerateModeEnabled;
    private final FieldWithDefault<Boolean> dualstackEnabled;
    private final FieldWithDefault<Boolean> checksumValidationEnabled;
    private final FieldWithDefault<Boolean> chunkedEncodingEnabled;
    private final FieldWithDefault<Boolean> useArnRegionEnabled;
    private final FieldWithDefault<ProfileFile> profileFile;
    private final FieldWithDefault<String> profileName;

    private S3Configuration(DefaultS3ServiceConfigurationBuilder builder) {
        this.dualstackEnabled = FieldWithDefault.create(builder.dualstackEnabled, DEFAULT_DUALSTACK_ENABLED);
        this.accelerateModeEnabled = FieldWithDefault.create(builder.accelerateModeEnabled, DEFAULT_ACCELERATE_MODE_ENABLED);
        this.pathStyleAccessEnabled = FieldWithDefault.create(builder.pathStyleAccessEnabled, DEFAULT_PATH_STYLE_ACCESS_ENABLED);
        this.checksumValidationEnabled =  FieldWithDefault.create(builder.checksumValidationEnabled,
                                                                 DEFAULT_CHECKSUM_VALIDATION_ENABLED);
        this.chunkedEncodingEnabled = FieldWithDefault.create(builder.chunkedEncodingEnabled, DEFAULT_CHUNKED_ENCODING_ENABLED);
        this.profileFile = FieldWithDefault.createLazy(builder.profileFile, ProfileFile::defaultProfileFile);
        this.profileName = FieldWithDefault.create(builder.profileName,
                                                   ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow());
        this.useArnRegionEnabled = FieldWithDefault.createLazy(builder.useArnRegionEnabled, this::resolveUserArnRegionEnabled);

        if (accelerateModeEnabled() && pathStyleAccessEnabled()) {
            throw new IllegalArgumentException("Accelerate mode cannot be used with path style addressing");
        }
    }

    private boolean resolveUserArnRegionEnabled() {
        return UseArnRegionProviderChain.create(this.profileFile.value(), this.profileName.value())
                                        .resolveUseArnRegion()
                                        .orElse(false);
    }

    /**
     * Create a {@link Builder}, used to create a {@link S3Configuration}.
     */
    public static Builder builder() {
        return new DefaultS3ServiceConfigurationBuilder();
    }

    /**
     * <p>
     * Returns whether the client uses path-style access for all requests.
     * </p>
     * <p>
     * Amazon S3 supports virtual-hosted-style and path-style access in all
     * Regions. The path-style syntax, however, requires that you use the
     * region-specific endpoint when attempting to access a bucket.
     * </p>
     * <p>
     * The default behaviour is to detect which access style to use based on
     * the configured endpoint (an IP will result in path-style access) and
     * the bucket being accessed (some buckets are not valid DNS names).
     * Setting this flag will result in path-style access being used for all
     * requests.
     * </p>
     *
     * @return True is the client should always use path-style access
     */
    public boolean pathStyleAccessEnabled() {
        return pathStyleAccessEnabled.value();
    }

    /**
     * <p>
     * Returns whether the client has enabled accelerate mode for getting and putting objects.
     * </p>
     * <p>
     * The default behavior is to disable accelerate mode for any operations (GET, PUT, DELETE). You need to call
     * {@link DefaultS3Client#putBucketAccelerateConfiguration(PutBucketAccelerateConfigurationRequest)}
     * first to use this feature.
     * </p>
     *
     * @return True if accelerate mode is enabled.
     */
    public boolean accelerateModeEnabled() {
        return accelerateModeEnabled.value();
    }

    /**
     * <p>
     * Returns whether the client is configured to use dualstack mode for
     * accessing S3. If you want to use IPv6 when accessing S3, dualstack
     * must be enabled.
     * </p>
     *
     * <p>
     * Dualstack endpoints are disabled by default.
     * </p>
     *
     * @return True if the client will use the dualstack endpoints
     */
    public boolean dualstackEnabled() {
        return dualstackEnabled.value();
    }

    public boolean checksumValidationEnabled() {
        return checksumValidationEnabled.value();
    }

    /**
     * Returns whether the client should use chunked encoding when signing the
     * payload body.
     * <p>
     * This option only currently applies to {@link
     * software.amazon.awssdk.services.s3.model.PutObjectRequest} and {@link
     * software.amazon.awssdk.services.s3.model.UploadPartRequest}.
     *
     * @return True if chunked encoding should be used.
     */
    public boolean chunkedEncodingEnabled() {
        return chunkedEncodingEnabled.value();
    }

    /**
     * Returns whether the client is allowed to make cross-region calls when an S3 Access Point ARN has a different
     * region to the one configured on the client.
     * <p>
     * @return True if a different region in the ARN can be used.
     */
    public boolean useArnRegionEnabled() {
        return useArnRegionEnabled.value();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .dualstackEnabled(dualstackEnabled.valueOrNullIfDefault())
                .accelerateModeEnabled(accelerateModeEnabled.valueOrNullIfDefault())
                .pathStyleAccessEnabled(pathStyleAccessEnabled.valueOrNullIfDefault())
                .checksumValidationEnabled(checksumValidationEnabled.valueOrNullIfDefault())
                .chunkedEncodingEnabled(chunkedEncodingEnabled.valueOrNullIfDefault())
                .useArnRegionEnabled(useArnRegionEnabled.valueOrNullIfDefault())
                .profileFile(profileFile.valueOrNullIfDefault())
                .profileName(profileName.valueOrNullIfDefault());
    }

    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, S3Configuration> {
        Boolean dualstackEnabled();

        /**
         * Option to enable using the dualstack endpoints when accessing S3. Dualstack
         * should be enabled if you want to use IPv6.
         *
         * <p>
         * Dualstack endpoints are disabled by default.
         * </p>
         *
         * @see S3Configuration#dualstackEnabled().
         */
        Builder dualstackEnabled(Boolean dualstackEnabled);

        Boolean accelerateModeEnabled();

        /**
         * Option to enable using the accelerate enedpoint when accessing S3. Accelerate
         * endpoints allow faster transfer of objects by using Amazon CloudFront's
         * globally distributed edge locations.
         *
         * <p>
         * Accelerate mode is disabled by default.
         * </p>
         *
         * @see S3Configuration#accelerateModeEnabled().
         */
        Builder accelerateModeEnabled(Boolean accelerateModeEnabled);

        Boolean pathStyleAccessEnabled();

        /**
         * Option to enable using path style access for accessing S3 objects
         * instead of DNS style access. DNS style access is preferred as it
         * will result in better load balancing when accessing S3.
         *
         * <p>
         * Path style access is disabled by default. Path style may still be used for legacy
         * buckets that are not DNS compatible.
         * </p>
         *
         * @see S3Configuration#pathStyleAccessEnabled().
         */
        Builder pathStyleAccessEnabled(Boolean pathStyleAccessEnabled);

        Boolean checksumValidationEnabled();

        /**
         * Option to disable doing a validation of the checksum of an object stored in S3.
         *
         * <p>
         * Checksum validation is enabled by default.
         * </p>
         *
         * @see S3Configuration#checksumValidationEnabled().
         */
        Builder checksumValidationEnabled(Boolean checksumValidationEnabled);

        Boolean chunkedEncodingEnabled();

        /**
         * Option to enable using chunked encoding when signing the request
         * payload for {@link
         * software.amazon.awssdk.services.s3.model.PutObjectRequest} and {@link
         * software.amazon.awssdk.services.s3.model.UploadPartRequest}.
         *
         * @see S3Configuration#chunkedEncodingEnabled()
         */
        Builder chunkedEncodingEnabled(Boolean chunkedEncodingEnabled);

        Boolean useArnRegionEnabled();

        /**
         * If an S3 resource ARN is passed in as the target of an S3 operation that has a different region to the one
         * the client was configured with, this flag must be set to 'true' to permit the client to make a
         * cross-region call to the region specified in the ARN otherwise an exception will be thrown.
         *
         * @see S3Configuration#useArnRegionEnabled()
         */
        Builder useArnRegionEnabled(Boolean useArnRegionEnabled);

        ProfileFile profileFile();

        /**
         * The profile file that should be consulted to determine the default value of {@link #useArnRegionEnabled(Boolean)}.
         * This is not used, if the {@link #useArnRegionEnabled(Boolean)} is configured.
         *
         * <p>
         * By default, the {@link ProfileFile#defaultProfileFile()} is used.
         * </p>
         */
        Builder profileFile(ProfileFile profileFile);

        String profileName();

        /**
         * The profile name that should be consulted to determine the default value of {@link #useArnRegionEnabled(Boolean)}.
         * This is not used, if the {@link #useArnRegionEnabled(Boolean)} is configured.
         *
         * <p>
         * By default, the {@link ProfileFileSystemSetting#AWS_PROFILE} is used.
         * </p>
         */
        Builder profileName(String profileName);
    }

    static final class DefaultS3ServiceConfigurationBuilder implements Builder {
        private Boolean dualstackEnabled;
        private Boolean accelerateModeEnabled;
        private Boolean pathStyleAccessEnabled;
        private Boolean checksumValidationEnabled;
        private Boolean chunkedEncodingEnabled;
        private Boolean useArnRegionEnabled;
        private ProfileFile profileFile;
        private String profileName;

        @Override
        public Boolean dualstackEnabled() {
            return dualstackEnabled;
        }

        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        @Override
        public Boolean accelerateModeEnabled() {
            return accelerateModeEnabled;
        }

        public void setDualstackEnabled(Boolean dualstackEnabled) {
            dualstackEnabled(dualstackEnabled);
        }

        public Builder accelerateModeEnabled(Boolean accelerateModeEnabled) {
            this.accelerateModeEnabled = accelerateModeEnabled;
            return this;
        }

        @Override
        public Boolean pathStyleAccessEnabled() {
            return pathStyleAccessEnabled;
        }

        public void setAccelerateModeEnabled(Boolean accelerateModeEnabled) {
            accelerateModeEnabled(accelerateModeEnabled);
        }

        public Builder pathStyleAccessEnabled(Boolean pathStyleAccessEnabled) {
            this.pathStyleAccessEnabled = pathStyleAccessEnabled;
            return this;
        }

        @Override
        public Boolean checksumValidationEnabled() {
            return checksumValidationEnabled;
        }

        public void setPathStyleAccessEnabled(Boolean pathStyleAccessEnabled) {
            pathStyleAccessEnabled(pathStyleAccessEnabled);
        }

        public Builder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            this.checksumValidationEnabled = checksumValidationEnabled;
            return this;
        }

        @Override
        public Boolean chunkedEncodingEnabled() {
            return chunkedEncodingEnabled;
        }

        public void setChecksumValidationEnabled(Boolean checksumValidationEnabled) {
            checksumValidationEnabled(checksumValidationEnabled);
        }

        public Builder chunkedEncodingEnabled(Boolean chunkedEncodingEnabled) {
            this.chunkedEncodingEnabled = chunkedEncodingEnabled;
            return this;
        }

        @Override
        public Boolean useArnRegionEnabled() {
            return useArnRegionEnabled;
        }

        public void setChunkedEncodingEnabled(Boolean chunkedEncodingEnabled) {
            chunkedEncodingEnabled(chunkedEncodingEnabled);
        }

        public Builder useArnRegionEnabled(Boolean useArnRegionEnabled) {
            this.useArnRegionEnabled = useArnRegionEnabled;
            return this;
        }

        @Override
        public ProfileFile profileFile() {
            return profileFile;
        }

        @Override
        public Builder profileFile(ProfileFile profileFile) {
            this.profileFile = profileFile;
            return this;
        }

        @Override
        public String profileName() {
            return profileName;
        }

        @Override
        public Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        public void setUseArnRegionEnabled(Boolean useArnRegionEnabled) {
            useArnRegionEnabled(useArnRegionEnabled);
        }

        public S3Configuration build() {
            return new S3Configuration(this);
        }
    }
}
