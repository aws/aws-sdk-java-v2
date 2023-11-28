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

package software.amazon.awssdk.services.s3.internal.s3express;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;

@SdkInternalApi
public final class S3ExpressPlugin implements SdkPlugin {
    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        S3ServiceClientConfiguration.Builder s3Config = (S3ServiceClientConfiguration.Builder) config;

        if (s3Config.authSchemes().get(S3ExpressAuthScheme.SCHEME_ID) == null) {
            s3Config.putAuthScheme(S3ExpressAuthScheme.create());
        }

        S3AuthSchemeProvider s3ExpressAuthSchemeProvider = S3ExpressAuthSchemeProvider.create(s3Config.authSchemeProvider());
        s3Config.authSchemeProvider(s3ExpressAuthSchemeProvider);
    }
}
