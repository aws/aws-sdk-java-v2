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
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;

/**
 * Internal plugin that sets the signer property {@link AwsV4FamilyHttpSigner#CHUNK_ENCODING_ENABLED} to {@code false}. This
 * plugin is invoked by the client builder only if {@link S3Configuration#chunkedEncodingEnabled()} is set to {@code false}.
 */
@SdkInternalApi
public final class S3DisableChunkEncodingAuthSchemeProvider implements S3AuthSchemeProvider {

    private final S3AuthSchemeProvider delegate;

    private S3DisableChunkEncodingAuthSchemeProvider(S3AuthSchemeProvider delegate) {
        this.delegate = delegate;
    }

    public static S3DisableChunkEncodingAuthSchemeProvider create(S3AuthSchemeProvider delegate) {
        return new S3DisableChunkEncodingAuthSchemeProvider(delegate);
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
        List<AuthSchemeOption> options = delegate.resolveAuthScheme(authSchemeParams);
        List<AuthSchemeOption> result = options;

        // Disables chunk encoding but only for PutObject or UploadPart operations.
        String operation = authSchemeParams.operation();
        if ("PutObject".equals(operation) || "UploadPart".equals(operation)) {
            result = new ArrayList<>(options.size());
            for (AuthSchemeOption option : options) {
                String schemeId = option.schemeId();
                // We check here that the scheme id is sigV4 or sigV4a or some other in the same family.
                // We don't set the overrides for non-sigV4 auth schemes.
                if (schemeId.startsWith(AwsV4AuthScheme.SCHEME_ID)) {
                    result.add(option.toBuilder()
                                     .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, false)
                                     .build());
                }
            }
        }
        return result;
    }
}
