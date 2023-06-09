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

package software.amazon.awssdk.services.query.auth.scheme;

import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.AuthSchemeProvider;
import software.amazon.awssdk.services.query.auth.scheme.internal.DefaultQueryAuthSchemeProvider;

/**
 * An auth scheme provider for Query service. The auth scheme provider takes a set of parameters using
 * {@link QueryAuthSchemeParams}, and resolves a list of {@link AuthSchemeOption} based on the given parameters.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface QueryAuthSchemeProvider extends AuthSchemeProvider {
    /**
     * Resolve the auth schemes based on the given set of parameters.
     */
    List<AuthSchemeOption> resolveAuthScheme(QueryAuthSchemeParams authSchemeParams);

    /**
     * Resolve the auth schemes based on the given set of parameters.
     */
    default List<AuthSchemeOption> resolveAuthScheme(Consumer<QueryAuthSchemeParams.Builder> consumer) {
        QueryAuthSchemeParams.Builder builder = QueryAuthSchemeParams.builder();
        consumer.accept(builder);
        return resolveAuthScheme(builder.build());
    }

    /**
     * Get the default auth scheme provider.
     */
    static QueryAuthSchemeProvider defaultProvider() {
        return DefaultQueryAuthSchemeProvider.create();
    }
}
