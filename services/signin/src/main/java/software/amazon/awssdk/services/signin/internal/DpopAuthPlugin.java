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

package software.amazon.awssdk.services.signin.internal;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeProvider;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.signin.SigninServiceClientConfiguration;
import software.amazon.awssdk.services.signin.auth.scheme.SigninAuthSchemeParams;
import software.amazon.awssdk.services.signin.auth.scheme.SigninAuthSchemeProvider;
import software.amazon.awssdk.utils.Validate;

/**
 * An SDK plugin that will use DPoP auth for requests by adding the {@link DpopAuthScheme} and overriding the
 * {@link AuthSchemeProvider} with a custom provider that will always return Dpop.
 * The auth scheme uses the {@link DpopSigner} to add the required DPoP header to the request.
 */
@SdkInternalApi
public class DpopAuthPlugin implements SdkPlugin {
    private final String dpopKeyPem;

    private DpopAuthPlugin(String dpopKeyPem) {
        this.dpopKeyPem = Validate.paramNotNull(dpopKeyPem, "dpopKeyPem");
    }

    /**
     * Create an instance of the DpopAuthPlugin using the dpopKey from the {@link LoginAccessToken}
     * @param dpopKeyPem - PEM file contents containing the base64-encoding of the ECPrivateKey format defined by
     *                   RFC5915: Elliptic Curve Private Key Structure. It MUST include the public key coordinates.
     * @return dpopAuthPlugin
     */
    public static DpopAuthPlugin create(String dpopKeyPem) {
        return new DpopAuthPlugin(dpopKeyPem);
    }

    @Override
    public void configureClient(SdkServiceClientConfiguration.Builder config) {
        SigninServiceClientConfiguration.Builder scb =
            Validate.isInstanceOf(SigninServiceClientConfiguration.Builder.class, config,
                                  "DpopAuthPlugin must be applied to a SigninServiceClient");
        scb.authSchemeProvider(new DpopAuthSchemeProvider());
        // we must use a static DpopIdentity here rather than one that dynamically loads from the disk cache
        // the refresh request takes the clientId/refreshToken sourced from the access token on disk as input
        // so we must sign the request with the dpopKey loaded from the same load.  IE: do not read the
        // access token file twice!
        scb.putAuthScheme(DpopAuthScheme.create(StaticDpopIdentityProvider.create(dpopKeyPem)));
    }

    private static class DpopAuthSchemeProvider implements SigninAuthSchemeProvider {

        @Override
        public List<AuthSchemeOption> resolveAuthScheme(SigninAuthSchemeParams authSchemeParams) {
            return Collections.singletonList(AuthSchemeOption.builder().schemeId(DpopAuthScheme.SCHEME_NAME).build());
        }
    }

    /**
     * A identity provider that provides a static {@link DpopIdentity}
     */
    private static class StaticDpopIdentityProvider implements IdentityProvider<DpopIdentity> {
        private final DpopIdentity identity;

        private StaticDpopIdentityProvider(DpopIdentity identity) {
            this.identity = Validate.paramNotNull(identity, "identity");
        }

        public static StaticDpopIdentityProvider create(String dpopKeyPem) {
            return new StaticDpopIdentityProvider(DpopIdentity.create(dpopKeyPem));
        }

        @Override
        public Class<DpopIdentity> identityType() {
            return DpopIdentity.class;
        }

        @Override
        public CompletableFuture<? extends DpopIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return CompletableFuture.completedFuture(identity);
        }
    }
}
