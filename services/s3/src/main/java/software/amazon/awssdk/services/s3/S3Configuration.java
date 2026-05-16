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

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.ServiceConfiguration;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.services.s3.internal.FieldWithDefault;
import software.amazon.awssdk.services.s3.internal.settingproviders.DisableMultiRegionProviderChain;
import software.amazon.awssdk.services.s3.internal.settingproviders.UseArnRegionProviderChain;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketAccelerateConfigurationRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
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

    /**
     * By default, the SDK sends the {@code Expect: 100-continue} header for {@link PutObjectRequest}
     * and {@link UploadPartRequest}.
     */
    private static final boolean DEFAULT_EXPECT_CONTINUE_ENABLED = true;

    /**
     * The default minimum content-length in bytes at which the {@code Expect: 100-continue} header is added.
     * Requests with a content-length below this threshold will not include the header.
     */
    private static final long DEFAULT_EXPECT_CONTINUE_THRESHOLD_IN_BYTES = 1_048_576L;

    private final FieldWithDefault<Boolean> pathStyleAccessEnabled;
    private final FieldWithDefault<Boolean> accelerateModeEnabled;
    private final FieldWithDefault<Boolean> dualstackEnabled;
    private final FieldWithDefault<Boolean> checksumValidationEnabled;
    private final FieldWithDefault<Boolean> chunkedEncodingEnabled;
    private final FieldWithDefault<Boolean> expectContinueEnabled;
    private final FieldWithDefault<Long> expectContinueThresholdInBytes;
    private final Boolean useArnRegionEnabled;
    private final Boolean multiRegionEnabled;
    private final FieldWithDefault<Supplier<ProfileFile>> profileFile;
    private final FieldWithDefault<String> profileName;

    private S3Configuration(DefaultS3ServiceConfigurationBuilder builder) {
        this.dualstackEnabled = FieldWithDefault.create(builder.dualstackEnabled, DEFAULT_DUALSTACK_ENABLED);
        this.accelerateModeEnabled = FieldWithDefault.create(builder.accelerateModeEnabled, DEFAULT_ACCELERATE_MODE_ENABLED);
        this.pathStyleAccessEnabled = FieldWithDefault.create(builder.pathStyleAccessEnabled, DEFAULT_PATH_STYLE_ACCESS_ENABLED);
        this.checksumValidationEnabled = FieldWithDefault.create(builder.checksumValidationEnabled,
                                                                 DEFAULT_CHECKSUM_VALIDATION_ENABLED);
        this.chunkedEncodingEnabled = FieldWithDefault.create(builder.chunkedEncodingEnabled, DEFAULT_CHUNKED_ENCODING_ENABLED);
        this.expectContinueEnabled = FieldWithDefault.create(builder.expectContinueEnabled,
                                                             DEFAULT_EXPECT_CONTINUE_ENABLED);
        this.expectContinueThresholdInBytes = FieldWithDefault.create(builder.expectContinueThresholdInBytes,
                                                                      DEFAULT_EXPECT_CONTINUE_THRESHOLD_IN_BYTES);
        if (this.expectContinueThresholdInBytes.value() < 0) {
            throw new IllegalArgumentException(
                "expectContinueThresholdInBytes must not be negative, but was: "
                + this.expectContinueThresholdInBytes.value());
        }
        this.profileFile = FieldWithDefault.create(builder.profileFile, ProfileFile::defaultProfileFile);
        this.profileName = FieldWithDefault.create(builder.profileName,
                                                   ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow());
        this.useArnRegionEnabled = builder.useArnRegionEnabled;
        this.multiRegionEnabled = builder.multiRegionEnabled;

        if (accelerateModeEnabled() && pathStyleAccessEnabled()) {
            throw new IllegalArgumentException("Accelerate mode cannot be used with path style addressing");
        }
    }

    private boolean resolveUseArnRegionEnabled() {
        return UseArnRegionProviderChain.create(this.profileFile.value(), this.profileName.value())
                                        .resolveUseArnRegion()
                                        .orElse(false);
    }

    private boolean resolveMultiRegionEnabled() {
        return !DisableMultiRegionProviderChain.create(this.profileFile.value(), this.profileName.value())
                                               .resolve()
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

    /**
     * Returns whether MD5 trailing checksum validation is enabled. This is enabled by default.
     *
     * <p>
     * The recommended approach is to specify a {@link ChecksumAlgorithm} on the {@link PutObjectRequest} and enable
     * {@link ChecksumMode} on the {@link GetObjectRequest}. In that case, validation will be performed for the specified
     * flexible checksum, and validation will not be performed for MD5 checksum.
     *
     * <p>
     * For {@link PutObjectRequest}, MD5 trailing checksum validation will be performed if:
     * <ul>
     *     <li>Checksum validation is not disabled</li>
     *     <li>Server-side encryption is not used</li>
     *     <li>Flexible checksum {@link ChecksumAlgorithm} is not specified</li>
     * </ul>
     *
     * For {@link GetObjectRequest}, MD5 trailing checksum validation will be performed if:
     * <ul>
     *     <li>Checksum validation is not disabled</li>
     *     <li>{@link ChecksumMode} is disabled (default)</li>
     *     <li>Regular S3 is used (non-S3Express)</li>
     * </ul>
     *
     * @return True if trailing checksum validation is enabled
     */
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
     * Returns whether the S3 SDK client's explicit setting of the {@code Expect: 100-continue} header is enabled for
     * {@link PutObjectRequest} and {@link UploadPartRequest}. This controls whether the SDK adds the header during
     * request interceptor processing.
     * <p>
     * By default, the SDK sends the {@code Expect: 100-continue} header for these operations, allowing the server to
     * reject the request before the client sends the full payload. Setting this to {@code false} disables this behavior.
     * <p>
     * <b>Note:</b> When using the {@code ApacheHttpClient} (Apache 4), the Apache 4 client also independently adds the
     * {@code Expect: 100-continue} header by default via its own {@code expectContinueEnabled} setting. To fully
     * suppress the header on the wire, you must also disable it on the Apache4 HTTP client builder using
     * {@code ApacheHttpClient.builder().expectContinueEnabled(false)}. This does NOT apply to the {@code Apache5HttpClient}
     * which defaults {@code expectContinueEnabled} to false.
     *
     * @return True if the Expect: 100-continue header is enabled.
     * @see S3Configuration.Builder#expectContinueEnabled(Boolean)
     */
    public boolean expectContinueEnabled() {
        return expectContinueEnabled.value();
    }

    /**
     * Returns the minimum content-length in bytes at which the {@code Expect: 100-continue} header is added to
     * {@link PutObjectRequest} and {@link UploadPartRequest}. Requests with a content-length below this threshold
     * will not include the header.
     * <p>
     * The default value is 1048576 bytes (1 MB).
     * <p>
     * <b>Note:</b> When using the {@code ApacheHttpClient} (Apache 4), the Apache 4 client also independently adds the
     * {@code Expect: 100-continue} header by default without any threshold via its own {@code expectContinueEnabled}
     * setting. To benefit from the `expectContinueThresholdInBytes` you must disable {@code expectContinueEnabled}
     * on the Apache4 HTTP client builder using {@code ApacheHttpClient.builder().expectContinueEnabled(false)}.
     * This does NOT apply to the {@code Apache5HttpClient} which defaults {@code expectContinueEnabled} to false.
     *
     * @return The threshold in bytes.
     * @see S3Configuration.Builder#expectContinueThresholdInBytes(Long)
     */
    public long expectContinueThresholdInBytes() {
        return expectContinueThresholdInBytes.value();
    }

    /**
     * Returns whether the client is allowed to make cross-region calls when an S3 Access Point ARN has a different
     * region to the one configured on the client.
     * <p>
     * @return True if a different region in the ARN can be used.
     */
    public boolean useArnRegionEnabled() {
        return Optional.ofNullable(useArnRegionEnabled)
                       .orElseGet(this::resolveUseArnRegionEnabled);
    }

    /**
     * Returns whether the client is allowed to make cross-region calls when using an S3 Multi-Region Access Point ARN.
     * <p>
     * @return True if multi-region ARNs is enabled.
     */
    public boolean multiRegionEnabled() {
        return Optional.ofNullable(multiRegionEnabled)
                       .orElseGet(this::resolveMultiRegionEnabled);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .dualstackEnabled(dualstackEnabled.valueOrNullIfDefault())
                .multiRegionEnabled(multiRegionEnabled)
                .accelerateModeEnabled(accelerateModeEnabled.valueOrNullIfDefault())
                .pathStyleAccessEnabled(pathStyleAccessEnabled.valueOrNullIfDefault())
                .checksumValidationEnabled(checksumValidationEnabled.valueOrNullIfDefault())
                .chunkedEncodingEnabled(chunkedEncodingEnabled.valueOrNullIfDefault())
                .expectContinueEnabled(expectContinueEnabled.valueOrNullIfDefault())
                .expectContinueThresholdInBytes(expectContinueThresholdInBytes.valueOrNullIfDefault())
                .useArnRegionEnabled(useArnRegionEnabled)
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
         * @deprecated This option has been replaced with {@link S3ClientBuilder#dualstackEnabled(Boolean)} and
         * {@link S3Presigner.Builder#dualstackEnabled(Boolean)}. If both this and one of those options are set, an exception
         * will be thrown.
         */
        @Deprecated
        Builder dualstackEnabled(Boolean dualstackEnabled);

        Boolean accelerateModeEnabled();

        /**
         * Option to enable using the accelerate endpoint when accessing S3. Accelerate
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

        /**
         * @deprecated This option has been replaced with
         * {@link S3ClientBuilder#requestChecksumCalculation(RequestChecksumCalculation)} and
         * {@link S3ClientBuilder#responseChecksumValidation(ResponseChecksumValidation)}. If both this and one of those options
         * are set, an exception will be thrown.
         */
        @Deprecated
        Boolean checksumValidationEnabled();

        /**
         * Option to disable MD5 trailing checksum validation of an object stored in S3. This is enabled by default.
         *
         * <p>
         * The recommended approach is to specify a {@link ChecksumAlgorithm} on the {@link PutObjectRequest} and enable
         * {@link ChecksumMode} on the {@link GetObjectRequest}. In that case, validation will be performed for the specified
         * flexible checksum, and validation will not be performed for MD5 checksum.
         *
         * <p>
         * For {@link PutObjectRequest}, MD5 trailing checksum validation will be performed if:
         * <ul>
         *     <li>Checksum validation is not disabled</li>
         *     <li>Server-side encryption is not used</li>
         *     <li>Flexible checksum algorithm is not specified</li>
         * </ul>
         *
         * For {@link GetObjectRequest}, MD5 trailing checksum validation will be performed if:
         * <ul>
         *     <li>Checksum validation is not disabled</li>
         *     <li>{@link ChecksumMode} is disabled (default)</li>
         *     <li>Regular S3 is used (non-S3Express)</li>
         * </ul>
         *
         * @see S3Configuration#checksumValidationEnabled().
         *
         * @deprecated This option has been replaced with
         * {@link S3ClientBuilder#requestChecksumCalculation(RequestChecksumCalculation)} and
         * {@link S3ClientBuilder#responseChecksumValidation(ResponseChecksumValidation)}. If both this and one of those options
         * are set, an exception will be thrown.
         */
        @Deprecated
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

        Boolean expectContinueEnabled();

        /**
         * Option to enable or disable the S3 SDK client's explicit setting of the {@code Expect: 100-continue} header
         * for {@link PutObjectRequest} and {@link UploadPartRequest}.
         * <p>
         * By default, the SDK sends the {@code Expect: 100-continue} header for these operations, allowing the server to
         * reject the request before the client sends the full payload. Setting this to {@code false} disables this behavior.
         * <p>
         * <b>Note:</b> When using the Apache HTTP client, the Apache client also independently adds the
         * {@code Expect: 100-continue} header by default via its own {@code expectContinueEnabled} setting. To fully
         * suppress the header on the wire, you must also disable it on the Apache HTTP client builder using
         * {@code ApacheHttpClient.builder().expectContinueEnabled(false)}.
         * <p>
         * Enabled by default (i.e., the header is sent).
         *
         * @see S3Configuration#expectContinueEnabled()
         */
        Builder expectContinueEnabled(Boolean expectContinueEnabled);

        Long expectContinueThresholdInBytes();

        /**
         * Option to configure the minimum content-length in bytes at which the {@code Expect: 100-continue} header
         * is added to {@link PutObjectRequest} and {@link UploadPartRequest}. Requests with a content-length below
         * this threshold will not include the header, reducing latency for small uploads where the round-trip cost
         * of the 100-continue handshake outweighs the benefit.
         * <p>
         * The default value is 1048576 bytes (1 MB). Setting this to 0 restores the pre-threshold behavior where
         * the header is added for all non-zero content-length requests.
         * <p>
         * This setting only takes effect when {@link #expectContinueEnabled(Boolean)} is {@code true} (the default).
         * <p>
         * When content length is not known, the {@code Expect: 100-continue} header will always be added
         * when {@link #expectContinueEnabled(Boolean)} is {@code true}.
         * <p>
         * <b>Note:</b> When using the {@code ApacheHttpClient} (Apache 4), the Apache 4 client also independently adds the
         * {@code Expect: 100-continue} header by default via its own {@code expectContinueEnabled} setting. This threshold
         * only controls the SDK's own header addition; it does not affect the Apache client's behavior.
         *
         * @param expectContinueThresholdInBytes The threshold in bytes, or {@code null} to use the default (1048576).
         * @return This builder for method chaining.
         * @see S3Configuration#expectContinueThresholdInBytes()
         */
        Builder expectContinueThresholdInBytes(Long expectContinueThresholdInBytes);

        Boolean useArnRegionEnabled();

        /**
         * If an S3 resource ARN is passed in as the target of an S3 operation that has a different region to the one
         * the client was configured with, this flag must be set to 'true' to permit the client to make a
         * cross-region call to the region specified in the ARN otherwise an exception will be thrown.
         *
         * @see S3Configuration#useArnRegionEnabled()
         */
        Builder useArnRegionEnabled(Boolean useArnRegionEnabled);

        Boolean multiRegionEnabled();

        /**
         * Option to enable or disable the usage of multi-region access point ARNs. Multi-region access point ARNs
         * can result in cross-region calls, and can be prevented by setting this flag to false. This option is
         * enabled by default.
         *
         * @see S3Configuration#multiRegionEnabled()
         */
        Builder multiRegionEnabled(Boolean multiRegionEnabled);

        ProfileFile profileFile();

        /**
         * The profile file that should be consulted to determine the default value of {@link #useArnRegionEnabled(Boolean)}
         * or {@link #multiRegionEnabled(Boolean)}.
         * This is not used, if those parameters are configured.
         *
         * <p>
         * By default, the {@link ProfileFile#defaultProfileFile()} is used.
         * </p>
         */
        Builder profileFile(ProfileFile profileFile);

        Supplier<ProfileFile> profileFileSupplier();

        /**
         * The supplier of profile file instances that should be consulted to determine the default value of
         * {@link #useArnRegionEnabled(Boolean)} or {@link #multiRegionEnabled(Boolean)}.
         * This is not used, if those parameters are configured on the builder.
         *
         * <p>
         * By default, the {@link ProfileFile#defaultProfileFile()} is used.
         * </p>
         */
        Builder profileFile(Supplier<ProfileFile> profileFile);

        String profileName();

        /**
         * The profile name that should be consulted to determine the default value of {@link #useArnRegionEnabled(Boolean)}
         * or {@link #multiRegionEnabled(Boolean)}.
         * This is not used, if those parameters are configured.
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
        private Boolean expectContinueEnabled;
        private Long expectContinueThresholdInBytes;
        private Boolean useArnRegionEnabled;
        private Boolean multiRegionEnabled;
        private Supplier<ProfileFile> profileFile;
        private String profileName;

        @Override
        public Boolean dualstackEnabled() {
            return dualstackEnabled;
        }

        @Override
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

        @Override
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

        @Override
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

        @Override
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

        @Override
        public Builder chunkedEncodingEnabled(Boolean chunkedEncodingEnabled) {
            this.chunkedEncodingEnabled = chunkedEncodingEnabled;
            return this;
        }

        @Override
        public Boolean expectContinueEnabled() {
            return expectContinueEnabled;
        }

        public void setChunkedEncodingEnabled(Boolean chunkedEncodingEnabled) {
            chunkedEncodingEnabled(chunkedEncodingEnabled);
        }

        @Override
        public Builder expectContinueEnabled(Boolean expectContinueEnabled) {
            this.expectContinueEnabled = expectContinueEnabled;
            return this;
        }

        public void setExpectContinueEnabled(Boolean expectContinueEnabled) {
            expectContinueEnabled(expectContinueEnabled);
        }

        @Override
        public Long expectContinueThresholdInBytes() {
            return expectContinueThresholdInBytes;
        }

        @Override
        public Builder expectContinueThresholdInBytes(Long expectContinueThresholdInBytes) {
            this.expectContinueThresholdInBytes = expectContinueThresholdInBytes;
            return this;
        }

        public void setExpectContinueThresholdInBytes(Long expectContinueThresholdInBytes) {
            expectContinueThresholdInBytes(expectContinueThresholdInBytes);
        }

        @Override
        public Boolean useArnRegionEnabled() {
            return useArnRegionEnabled;
        }

        @Override
        public Builder useArnRegionEnabled(Boolean useArnRegionEnabled) {
            this.useArnRegionEnabled = useArnRegionEnabled;
            return this;
        }

        @Override
        public Boolean multiRegionEnabled() {
            return multiRegionEnabled;
        }

        @Override
        public Builder multiRegionEnabled(Boolean multiRegionEnabled) {
            this.multiRegionEnabled = multiRegionEnabled;
            return this;
        }

        @Override
        public ProfileFile profileFile() {
            return Optional.ofNullable(profileFile)
                .map(Supplier::get)
                .orElse(null);
        }

        @Override
        public Builder profileFile(ProfileFile profileFile) {
            return profileFile(Optional.ofNullable(profileFile)
                                       .map(ProfileFileSupplier::fixedProfileFile)
                                       .orElse(null));
        }

        @Override
        public Supplier<ProfileFile> profileFileSupplier() {
            return profileFile;
        }

        @Override
        public Builder profileFile(Supplier<ProfileFile> profileFile) {
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

        @Override
        public S3Configuration build() {
            return new S3Configuration(this);
        }
    }
}
