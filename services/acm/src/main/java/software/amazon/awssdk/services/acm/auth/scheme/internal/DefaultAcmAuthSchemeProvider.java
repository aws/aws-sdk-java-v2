/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.acm.auth.scheme.internal;

import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.REQUEST_SIGNING_INSTANT;
import static software.amazon.awssdk.http.auth.AwsV4HttpSigner.SERVICE_SIGNING_NAME;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.services.acm.auth.scheme.AcmAuthSchemeParams;
import software.amazon.awssdk.services.acm.auth.scheme.AcmAuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultAcmAuthSchemeProvider implements AcmAuthSchemeProvider {
    private static final DefaultAcmAuthSchemeProvider DEFAULT = new DefaultAcmAuthSchemeProvider();

    private DefaultAcmAuthSchemeProvider() {
    }

    public static DefaultAcmAuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(AcmAuthSchemeParams authSchemeParams) {
        AwsV4AuthScheme awsV4AuthScheme = AwsV4AuthScheme.create();
        return Arrays.asList(AuthSchemeOption.builder()
                                             .schemeId(awsV4AuthScheme.schemeId())
                                             .putSignerProperty(SERVICE_SIGNING_NAME, "acm")
                                             .putSignerProperty(REGION_NAME, authSchemeParams.region().id())
                                             // TODO: Make this signer property optional
                                             .putSignerProperty(REQUEST_SIGNING_INSTANT, Instant.now())
                                             .build());
    }
}
