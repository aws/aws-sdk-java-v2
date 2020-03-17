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
import software.amazon.awssdk.services.s3.internal.usearnregion.UseArnRegionProviderChain;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@SdkPublicApi
@Immutable
@ThreadSafe
public final class S3Configuration implements ServiceConfiguration, ToCopyableBuilder<S3Configuration.Builder, S3Configuration> {
    private static final UseArnRegionProviderChain USE_ARN_REGION_PROVIDER_CHAIN = UseArnRegionProviderChain.create();

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

    private final boolean pathStyleAccessEnabled;
    private final boolean accelerateModeEnabled;
    private final boolean dualstackEnabled;
    private final boolean checksumValidationEnabled;
    private final boolean chunkedEncodingEnabled;
    private final boolean useArnRegionEnabled;

    private S3Configuration(DefaultS3ServiceConfigurationBuilder builder) {
        this.dualstackEnabled = resolveBoolean(builder.dualstackEnabled, DEFAULT_DUALSTACK_ENABLED);
        this.accelerateModeEnabled = resolveBoolean(builder.accelerateModeEnabled, DEFAULT_ACCELERATE_MODE_ENABLED);
        this.pathStyleAccessEnabled = resolveBoolean(builder.pathStyleAccessEnabled, DEFAULT_PATH_STYLE_ACCESS_ENABLED);
        this.checksumValidationEnabled = resolveBoolean(builder.checksumValidationEnabled, DEFAULT_CHECKSUM_VALIDATION_ENABLED);
        if (accelerateModeEnabled && pathStyleAccessEnabled) {
            throw new IllegalArgumentException("Accelerate mode cannot be used with path style addressing");
        }
        this.chunkedEncodingEnabled = resolveBoolean(builder.chunkedEncodingEnabled, DEFAULT_CHUNKED_ENCODING_ENABLED);
        this.useArnRegionEnabled = Boolean.TRUE.equals(builder.useArnRegionEnabled) ? builder.useArnRegionEnabled :
            resolveUseArnRegionEnabled();
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
        return pathStyleAccessEnabled;
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
        return accelerateModeEnabled;
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
        return dualstackEnabled;
    }

    public boolean checksumValidationEnabled() {
        return checksumValidationEnabled;
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
        return chunkedEncodingEnabled;
    }

    /**
     * Returns whether the client is allowed to make cross-region calls when an S3 Access Point ARN has a different
     * region to the one configured on the client.
     * <p>
     * @return True if a different region in the ARN can be used.
     */
    public boolean useArnRegionEnabled() {
        return useArnRegionEnabled;
    }

    private boolean resolveBoolean(Boolean customerSuppliedValue, boolean defaultValue) {
        return customerSuppliedValue == null ? defaultValue : customerSuppliedValue;
    }

    private boolean resolveUseArnRegionEnabled() {
        return USE_ARN_REGION_PROVIDER_CHAIN.resolveUseArnRegion().orElse(false);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .dualstackEnabled(dualstackEnabled)
                .accelerateModeEnabled(accelerateModeEnabled)
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .useArnRegionEnabled(useArnRegionEnabled);
    }

    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, S3Configuration> { // (8)
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

        /**
         * Option to enable using chunked encoding when signing the request
         * payload for {@link
         * software.amazon.awssdk.services.s3.model.PutObjectRequest} and {@link
         * software.amazon.awssdk.services.s3.model.UploadPartRequest}.
         *
         * @see S3Configuration#chunkedEncodingEnabled()
         */
        Builder chunkedEncodingEnabled(Boolean chunkedEncodingEnabled);

        /**
         * If an S3 resource ARN is passed in as the target of an S3 operation that has a different region to the one
         * the client was configured with, this flag must be set to 'true' to permit the client to make a
         * cross-region call to the region specified in the ARN otherwise an exception will be thrown.
         *
         * @see S3Configuration#useArnRegionEnabled()
         */
        Builder useArnRegionEnabled(Boolean useArnRegionEnabled);
    }

    private static final class DefaultS3ServiceConfigurationBuilder implements Builder {

        private Boolean dualstackEnabled;
        private Boolean accelerateModeEnabled;
        private Boolean pathStyleAccessEnabled;
        private Boolean checksumValidationEnabled;
        private Boolean chunkedEncodingEnabled;
        private Boolean useArnRegionEnabled;

        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        public void setDualstackEnabled(Boolean dualstackEnabled) {
            dualstackEnabled(dualstackEnabled);
        }

        public Builder accelerateModeEnabled(Boolean accelerateModeEnabled) {
            this.accelerateModeEnabled = accelerateModeEnabled;
            return this;
        }

        public void setAccelerateModeEnabled(Boolean accelerateModeEnabled) {
            accelerateModeEnabled(accelerateModeEnabled);
        }

        public Builder pathStyleAccessEnabled(Boolean pathStyleAccessEnabled) {
            this.pathStyleAccessEnabled = pathStyleAccessEnabled;
            return this;
        }

        public void setPathStyleAccessEnabled(Boolean pathStyleAccessEnabled) {
            pathStyleAccessEnabled(pathStyleAccessEnabled);
        }

        public Builder checksumValidationEnabled(Boolean checksumValidationEnabled) {
            this.checksumValidationEnabled = checksumValidationEnabled;
            return this;
        }

        public void setChecksumValidationEnabled(Boolean checksumValidationEnabled) {
            checksumValidationEnabled(checksumValidationEnabled);
        }

        public Builder chunkedEncodingEnabled(Boolean chunkedEncodingEnabled) {
            this.chunkedEncodingEnabled = chunkedEncodingEnabled;
            return this;
        }

        public void setChunkedEncodingEnabled(Boolean chunkedEncodingEnabled) {
            chunkedEncodingEnabled(chunkedEncodingEnabled);
        }

        public Builder useArnRegionEnabled(Boolean useArnRegionEnabled) {
            this.useArnRegionEnabled = useArnRegionEnabled;
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
