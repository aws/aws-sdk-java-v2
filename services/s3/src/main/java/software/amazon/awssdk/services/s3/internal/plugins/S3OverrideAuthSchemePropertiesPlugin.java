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

package software.amazon.awssdk.services.s3.internal.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;

/**
 * Plugin that allows override of signer and identity properties on the selected auth scheme options. The class offers static
 * methods to create plugins for common cases such as enable payload signing by default. For instance, if you want
 * to unconditionally enable payload signing across the board you can create the S3 client, e.g.,
 *
 * {@snippet
 * S3AsyncClient s3 = S3AsyncClient.builder()
 *                                 .region(Region.US_WEST_2)
 *                                 .credentialsProvider(CREDENTIALS)
 *                                 .httpClient(httpClient)
 *                                 .addPlugin(S3OverrideAuthSchemePropertiesPlugin.enablePayloadSigningPlugin())
 *                                 .build();
 * }
 *
 * The plugin can also be used for a particular request, e.g.,
 *
 * {@snippet
 * s3Client.putObject(PutObjectRequest.builder()
 *                            .overrideConfiguration(c -> c.addPlugin(
 *                                S3OverrideAuthSchemePropertiesPlugin.enablePayloadSigningPlugin()))
 *                            .checksumAlgorithm(ChecksumAlgorithm.SHA256)
 *                            .bucket("test").key("test").build(), RequestBody.fromBytes("abc".getBytes()));
 * }
 */
@SdkProtectedApi
public final class S3OverrideAuthSchemePropertiesPlugin implements SdkPlugin {
    private final Map<IdentityProperty<?>, Object> identityProperties;
    private final Map<SignerProperty<?>, Object> signerProperties;
    private final Set<String> operationConstraints;

    private S3OverrideAuthSchemePropertiesPlugin(Builder builder) {
        if (builder.identityProperties.isEmpty()) {
            this.identityProperties = Collections.emptyMap();
        } else {
            this.identityProperties = Collections.unmodifiableMap(new HashMap<>(builder.identityProperties));
        }
        if (builder.signerProperties.isEmpty()) {
            this.signerProperties = Collections.emptyMap();
        } else {
            this.signerProperties = Collections.unmodifiableMap(new HashMap<>(builder.signerProperties));
        }
        if (builder.operationConstraints.isEmpty()) {
            this.operationConstraints = Collections.emptySet();
        } else {
            this.operationConstraints = Collections.unmodifiableSet(new HashSet<>(builder.operationConstraints));
        }
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        if (identityProperties.isEmpty() && signerProperties.isEmpty()) {
            return;
        }
        S3ServiceClientConfiguration.Builder s3Config = (S3ServiceClientConfiguration.Builder) config;
        S3AuthSchemeProvider delegate = s3Config.authSchemeProvider();
        s3Config.authSchemeProvider(params -> {
            List<AuthSchemeOption> options = delegate.resolveAuthScheme(params);
            List<AuthSchemeOption> result = new ArrayList<>(options.size());
            for (AuthSchemeOption option : options) {
                // We check here that the scheme id is sigV4 or sigV4a or some other in the same family.
                // We don't set the overrides for non-sigV4 auth schemes. If the plugin was configured to
                // constraint using operations then that's also checked on the call below.
                if (addConfiguredProperties(option, params)) {
                    AuthSchemeOption.Builder builder = option.toBuilder();
                    identityProperties.forEach((k, v) -> putIdentityProperty(builder, k, v));
                    signerProperties.forEach((k, v) -> putSignerProperty(builder, k, v));
                    result.add(builder.build());
                } else {
                    result.add(option);
                }
            }
            return result;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> void putIdentityProperty(AuthSchemeOption.Builder builder, IdentityProperty<?> key, Object value) {
        // Safe because of Builder#putIdentityProperty
        builder.putIdentityProperty((IdentityProperty<T>) key, (T) value);
    }

    @SuppressWarnings("unchecked")
    private <T> void putSignerProperty(AuthSchemeOption.Builder builder, SignerProperty<?> key, Object value) {
        // Safe because of Builder#putSignerProperty
        builder.putSignerProperty((SignerProperty<T>) key, (T) value);
    }

    private boolean addConfiguredProperties(AuthSchemeOption option, S3AuthSchemeParams params) {
        String schemeId = option.schemeId();
        // We check here that the scheme id is sigV4 or sigV4a or some other in the same family.
        // We don't set the overrides for non-sigV4 auth schemes.
        if (schemeId.startsWith(AwsV4AuthScheme.SCHEME_ID)) {
            if (this.operationConstraints.isEmpty() || this.operationConstraints.contains(params.operation())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new plugin that enables payload signing. This plugin can be used per client or by per-request.
     */
    public static SdkPlugin enablePayloadSigningPlugin() {
        return builder()
            .payloadSigningEnabled(true)
            .build();
    }

    /**
     * Creates a new plugin that disables the ChunkEncoding signers property for the `UploadPart` and `PutObject` operations.
     * This plugin can be used per client or by per-request.
     */
    public static SdkPlugin disableChunkEncodingPlugin() {
        return builder()
            .chunkEncodingEnabled(false)
            .addOperationConstraint("UploadPart")
            .addOperationConstraint("PutObject")
            .build();
    }

    /**
     * Creates a new builder to configure the plugin.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<IdentityProperty<?>, Object> identityProperties = new HashMap<>();
        private final Map<SignerProperty<?>, Object> signerProperties = new HashMap<>();
        private final Set<String> operationConstraints = new HashSet<>();

        /**
         * Adds an operation constraint to use the configured properties.
         */
        public Builder addOperationConstraint(String operation) {
            this.operationConstraints.add(operation);
            return this;
        }

        /**
         * Adds the provided property value as an override.
         */
        public <T> Builder putIdentityProperty(IdentityProperty<T> key, T value) {
            identityProperties.put(key, value);
            return this;
        }

        /**
         * Adds the provided property value as an override.
         */
        public <T> Builder putSignerProperty(SignerProperty<T> key, T value) {
            signerProperties.put(key, value);
            return this;
        }

        /**
         * Sets the {@link AwsV4FamilyHttpSigner#NORMALIZE_PATH} signing property to the given value.
         */
        public Builder normalizePath(Boolean value) {
            return putSignerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, value);
        }

        /**
         * Sets the {@link AwsV4FamilyHttpSigner#CHUNK_ENCODING_ENABLED} signing property to the given value.
         */
        public Builder chunkEncodingEnabled(Boolean value) {
            return putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, value);
        }

        /**
         * Sets the {@link AwsV4FamilyHttpSigner#PAYLOAD_SIGNING_ENABLED} signing property to the given value.
         */
        public Builder payloadSigningEnabled(Boolean value) {
            return putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, value);
        }

        /**
         * Builds and returns a new plugin.
         */
        public S3OverrideAuthSchemePropertiesPlugin build() {
            return new S3OverrideAuthSchemePropertiesPlugin(this);
        }
    }
}
