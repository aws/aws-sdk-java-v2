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
package software.amazon.awssdk.services.minis3.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.minis3.auth.scheme.MiniS3AuthSchemeParams;
import software.amazon.awssdk.services.minis3.auth.scheme.MiniS3AuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultMiniS3AuthSchemeProvider implements MiniS3AuthSchemeProvider {
    private static final DefaultMiniS3AuthSchemeProvider DEFAULT = new DefaultMiniS3AuthSchemeProvider();

    private DefaultMiniS3AuthSchemeProvider() {
    }

    public static DefaultMiniS3AuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(MiniS3AuthSchemeParams params) {
        List<AuthSchemeOption> options = new ArrayList<>();
        options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "mini-s3-service")
                .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id())
                .putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, false)
                .putSignerProperty(AwsV4HttpSigner.NORMALIZE_PATH, false)
                .putSignerProperty(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, false).build());
        return Collections.unmodifiableList(options);
    }
}
