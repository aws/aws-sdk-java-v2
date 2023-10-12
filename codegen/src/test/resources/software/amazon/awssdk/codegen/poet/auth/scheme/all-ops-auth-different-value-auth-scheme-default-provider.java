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

package software.amazon.awssdk.services.database.auth.scheme.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeParams;
import software.amazon.awssdk.services.database.auth.scheme.DatabaseAuthSchemeProvider;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultDatabaseAuthSchemeProvider implements DatabaseAuthSchemeProvider {
    private static final DefaultDatabaseAuthSchemeProvider DEFAULT = new DefaultDatabaseAuthSchemeProvider();

    private DefaultDatabaseAuthSchemeProvider() {
    }

    public static DefaultDatabaseAuthSchemeProvider create() {
        return DEFAULT;
    }

    @Override
    public List<AuthSchemeOption> resolveAuthScheme(DatabaseAuthSchemeParams params) {
        List<AuthSchemeOption> options = new ArrayList<>();
        switch (params.operation()) {
        case "DeleteRow":
            options.add(AuthSchemeOption.builder().schemeId("smithy.api#httpBearerAuth").build());
            options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                    .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "database-service")
                    .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id()).build());
            break;
        case "GetRow":
            options.add(AuthSchemeOption.builder().schemeId("smithy.api#httpBearerAuth").build());
            break;
        case "PutRow":
            options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                    .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "database-service")
                    .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id()).build());
            break;
        default:
            options.add(AuthSchemeOption.builder().schemeId("aws.auth#sigv4")
                    .putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, "database-service")
                    .putSignerProperty(AwsV4HttpSigner.REGION_NAME, params.region().id()).build());
            options.add(AuthSchemeOption.builder().schemeId("smithy.api#httpBearerAuth").build());
            break;
        }
        return Collections.unmodifiableList(options);
    }
}
