/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ServiceAdvancedConfiguration;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

@Immutable
@ThreadSafe
public final class S3AdvancedConfiguration implements
                                           ServiceAdvancedConfiguration,
                                           ToCopyableBuilder<S3AdvancedConfiguration.Builder, S3AdvancedConfiguration> {

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

    private final Boolean pathStyleAccessEnabled;
    private final Boolean accelerateModeEnabled;
    private final Boolean dualstackEnabled;

    private S3AdvancedConfiguration(DefaultS3AdvancedConfigurationBuilder builder) {
        this.dualstackEnabled = resolveBoolean(builder.dualstackEnabled, DEFAULT_DUALSTACK_ENABLED);
        this.accelerateModeEnabled = resolveBoolean(builder.accelerateModeEnabled, DEFAULT_ACCELERATE_MODE_ENABLED);
        this.pathStyleAccessEnabled = resolveBoolean(builder.pathStyleAccessEnabled, DEFAULT_PATH_STYLE_ACCESS_ENABLED);
        if (accelerateModeEnabled && pathStyleAccessEnabled) {
            throw new IllegalArgumentException("Accelerate mode cannot be used with path style addressing");
        }
    }

    /**
     * Create a {@link Builder}, used to create a {@link S3AdvancedConfiguration}.
     */
    public static Builder builder() {
        return new DefaultS3AdvancedConfigurationBuilder();
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

    private boolean resolveBoolean(Boolean customerSuppliedValue, boolean defaultValue) {
        return customerSuppliedValue == null ? defaultValue : customerSuppliedValue;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .dualstackEnabled(dualstackEnabled)
                .accelerateModeEnabled(accelerateModeEnabled)
                .pathStyleAccessEnabled(pathStyleAccessEnabled);
    }

    @NotThreadSafe
    public interface Builder extends CopyableBuilder<Builder, S3AdvancedConfiguration> { // (8)
        /**
         * Option to enable using the dualstack endpoints when accessing S3. Dualstack
         * should be enabled if you want to use IPv6.
         *
         * <p>
         * Dualstack endpoints are disabled by default.
         * </p>
         *
         * @see S3AdvancedConfiguration#dualstackEnabled().
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
         * @see S3AdvancedConfiguration#accelerateModeEnabled().
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
         * @see S3AdvancedConfiguration#pathStyleAccessEnabled().
         */
        Builder pathStyleAccessEnabled(Boolean pathStyleAccessEnabled);
    }

    private static final class DefaultS3AdvancedConfigurationBuilder implements Builder {

        private Boolean dualstackEnabled;
        private Boolean accelerateModeEnabled;
        private Boolean pathStyleAccessEnabled;

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

        public S3AdvancedConfiguration build() {
            return new S3AdvancedConfiguration(this);
        }
    }
}
