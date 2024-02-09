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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.utils.Logger;

/**
 * Internal plugin that uses the check if {@link S3Configuration#chunkedEncodingEnabled()} is configured and equals to
 * {@code false}, if so, then it installs an instance of {@link S3DisableChunkEncodingAuthSchemeProvider} wrapping the configured
 * {@link S3AuthSchemeProvider} that sets {@link  AwsV4FamilyHttpSigner#CHUNK_ENCODING_ENABLED} to false.
 * <p>
 * This pre SRA logic was implemented before using an interceptor but now requires wrapping the S3AuthSchemeProvider for it to
 * work.
 */
@SdkInternalApi
public final class S3DisableChunkEncodingIfConfiguredPlugin implements SdkPlugin {

    private static final Logger LOG = Logger.loggerFor(S3DisableChunkEncodingIfConfiguredPlugin.class);

    private final boolean isServiceConfigurationPresent;
    private final boolean isChunkedEncodingEnabledConfigured;
    private final boolean isChunkedEncodingEnabledDisabled;
    private final boolean configuresDisableChunkEncoding;

    public S3DisableChunkEncodingIfConfiguredPlugin(SdkClientConfiguration config) {
        S3Configuration serviceConfiguration =
            (S3Configuration) config.option(SdkClientOption.SERVICE_CONFIGURATION);

        boolean isServiceConfigurationPresent = serviceConfiguration != null;
        boolean shouldAddDisableChunkEncoding = false;
        boolean isChunkedEncodingEnabledConfigured = false;
        boolean isChunkedEncodingEnabledDisabled = false;
        boolean configuresDisableChunkEncoding = false;
        if (isServiceConfigurationPresent) {
            isChunkedEncodingEnabledConfigured = serviceConfiguration.toBuilder().chunkedEncodingEnabled() != null;
            isChunkedEncodingEnabledDisabled = !serviceConfiguration.chunkedEncodingEnabled();
            configuresDisableChunkEncoding = isChunkedEncodingEnabledConfigured && isChunkedEncodingEnabledDisabled;
            if (configuresDisableChunkEncoding) {
                shouldAddDisableChunkEncoding = true;
            }
        }
        this.configuresDisableChunkEncoding = shouldAddDisableChunkEncoding;
        this.isChunkedEncodingEnabledConfigured = isChunkedEncodingEnabledConfigured;
        this.isChunkedEncodingEnabledDisabled = isChunkedEncodingEnabledDisabled;
        this.isServiceConfigurationPresent = isServiceConfigurationPresent;
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        if (configuresDisableChunkEncoding) {
            LOG.debug(() -> String.format("chunkedEncodingEnabled was explicitly disabled in the configuration, adding "
                                          + "`S3DisableChunkEncodingAuthSchemeProvider` auth provider wrapper."));
            S3ServiceClientConfiguration.Builder s3Config = (S3ServiceClientConfiguration.Builder) config;

            S3AuthSchemeProvider disablingAuthSchemeProvider =
                S3DisableChunkEncodingAuthSchemeProvider.create(s3Config.authSchemeProvider());
            s3Config.authSchemeProvider(disablingAuthSchemeProvider);
        }
    }
}
