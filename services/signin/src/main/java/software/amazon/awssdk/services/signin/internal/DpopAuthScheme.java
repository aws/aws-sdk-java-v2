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

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.signin.SigninServiceClientConfiguration;
import software.amazon.awssdk.services.signin.auth.scheme.SigninAuthSchemeParams;
import software.amazon.awssdk.services.signin.auth.scheme.SigninAuthSchemeProvider;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.Validate;

public class DpopAuthScheme implements AuthScheme<DpopAuthScheme.DpopIdentity> {
    public static final String SCHEME_NAME = "DPOP";

    private final DpopIdentityProvider identityProvider;

    private DpopAuthScheme(DpopIdentityProvider identityProvider) {
        this.identityProvider = Validate.paramNotNull(identityProvider, "identityProvider");
    }

    public static DpopAuthScheme create(ECPublicKey ecPublicKey) {
        return new DpopAuthScheme(DpopIdentityProvider.create(ecPublicKey));
    }

    @Override
    public String schemeId() {
        return SCHEME_NAME;
    }

    @Override
    public IdentityProvider<DpopIdentity> identityProvider(IdentityProviders providers) {
        // we don't currently support adding an arbitrary identityProvider as a request level override
        // return the identity provider configured up front instead
        return identityProvider;
    }

    @Override
    public HttpSigner<DpopIdentity> signer() {
        return new DpopSigner();
    }

    public static class DpopIdentity implements Identity {
        private final ECPublicKey publicKey;
        private final ECPrivateKey privateKey;

        private DpopIdentity(ECPublicKey publicKey, ECPrivateKey privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public static DpopIdentity create(ECPublicKey publicKey, ECPrivateKey privateKey) {
            return new DpopIdentity(publicKey, privateKey);
        }

        public static DpopIdentity create(String dpopKeyPem) {
            Pair<ECPrivateKey, ECPublicKey> keys = EcKeyLoader.loadSec1Pem(dpopKeyPem);
            return new DpopIdentity(keys.right(), keys.left());
        }

        public ECPublicKey getPublicKey() {
            return publicKey;
        }

        public ECPrivateKey getPrivateKey() {
            return privateKey;
        }
    }

    private static class DpopSigner implements HttpSigner<DpopIdentity> {

        @Override
        public SignedRequest sign(SignRequest<? extends DpopIdentity> request) {
            return SignedRequest.builder()
                                .request(request.request())
                                .payload(request.payload().orElse(null))
                                .build();
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends DpopIdentity> request) {
            return CompletableFuture.completedFuture(
                AsyncSignedRequest.builder()
                                                                       .request(request.request())
                                                                       .payload(request.payload().orElse(null))
                                                                       .build());
        }
    }

    private static class DpopIdentityProvider implements IdentityProvider<DpopIdentity> {
        private final DpopIdentity identity;

        private DpopIdentityProvider(DpopIdentity identity) {
            this.identity = Validate.paramNotNull(identity, "identity");
        }

        public static DpopIdentityProvider create(ECPublicKey ecPublicKey) {
            return new DpopIdentityProvider(DpopIdentity.create(ecPublicKey));
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

    public static class DpopAuthSchemeResolver implements SigninAuthSchemeProvider {

        @Override
        public List<AuthSchemeOption> resolveAuthScheme(SigninAuthSchemeParams authSchemeParams) {
            return Collections.singletonList(AuthSchemeOption.builder().schemeId(SCHEME_NAME).build());
        }
    }

    public static class DpopAuthPlugin implements SdkPlugin {
        private final ECPublicKey ecPublicKey;


        @Override
        public void configureClient(SdkServiceClientConfiguration.Builder config) {
            SigninServiceClientConfiguration.Builder scb =
                Validate.isInstanceOf(SigninServiceClientConfiguration.Builder.class, config, "bad");
            scb.authSchemeProvider(new DpopAuthSchemeResolver());
            scb.putAuthScheme(new DpopAuthScheme());
        }
    }
}