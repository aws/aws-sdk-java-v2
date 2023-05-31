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

package software.amazon.awssdk.services.s3.auth.scheme.internal;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.HttpAuthOption;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultS3AuthSchemeProvider implements S3AuthSchemeProvider {

    private static final DefaultS3AuthSchemeProvider DEFAULT = new DefaultS3AuthSchemeProvider();

    private DefaultS3AuthSchemeProvider() {
    }

    public static DefaultS3AuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<HttpAuthOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
        return new ArrayList<>();
    }
}
