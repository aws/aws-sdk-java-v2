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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.IdentityProperty;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;


@SdkInternalApi
public final class S3ExpressAuthSchemeProvider implements S3AuthSchemeProvider {

    public static final IdentityProperty<String> BUCKET = IdentityProperty.create(String.class, "Bucket");

    private final S3AuthSchemeProvider delegate;

    private S3ExpressAuthSchemeProvider(S3AuthSchemeProvider delegate) {
        this.delegate = delegate;
    }

    public static S3ExpressAuthSchemeProvider create(S3AuthSchemeProvider delegate) {
        return new S3ExpressAuthSchemeProvider(delegate);
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
        List<AuthSchemeOption> options = delegate.resolveAuthScheme(authSchemeParams);
        List<AuthSchemeOption> result = new ArrayList<>(options.size());
        for (AuthSchemeOption option : options) {
            result.add(option.toBuilder()
                             .putIdentityProperty(BUCKET, authSchemeParams.bucket())
                             .build());
        }
        return result;
    }

    public S3AuthSchemeProvider delegate() {
        return delegate;
    }
}
