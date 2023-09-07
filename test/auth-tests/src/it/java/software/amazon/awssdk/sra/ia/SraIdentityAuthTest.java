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

package software.amazon.awssdk.sra.ia;

import static java.util.Collections.singletonList;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.AuthScheme;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.HttpSigner;
import software.amazon.awssdk.http.auth.spi.IdentityProviderConfiguration;
import software.amazon.awssdk.http.auth.spi.NoAuthAuthScheme;
import software.amazon.awssdk.http.auth.spi.SyncSignRequest;
import software.amazon.awssdk.http.auth.spi.SyncSignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.acm.model.ListCertificatesRequest;
import software.amazon.awssdk.services.codecatalyst.CodeCatalystClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeParams;
import software.amazon.awssdk.services.s3.auth.scheme.S3AuthSchemeProvider;

class SraIdentityAuthTest {

    @Test
    @Disabled
    void showChangesToClientBuilder_bearer() {
        CodeCatalystClient.builder()
            .credentialsProvider(null)
            .tokenProvider(null)
            .authSchemeProvider(null)
            .putAuthScheme(null)
            .build();
    }

    @Test
    @Disabled
    void showChangesToClientBuilder_sigv4() {
        AcmClient.builder()
                 .credentialsProvider(null)
                 // .tokenProvider(null)
                 .authSchemeProvider(null)
                 .putAuthScheme(null)
                 .build();
    }

    // From https://quip-amazon.com/BRKSAiAaVE6s/AWS-SDK-for-Java-2x-SRA-Authentication#temp:C:TCW3d7718fff4e0435fb78f01ac0
    @Test
    void disableSigning() {
        // Before
        ClientOverrideConfiguration config =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                          new NoOpSigner())
                                       .build();

        AcmClient client =
            AcmClient.builder()
                     .overrideConfiguration(config)
                     .build();

        client.listCertificates();

        // After
        AuthSchemeOption noAuth = AuthSchemeOption.builder()
                                                  .schemeId("smithy.api#noAuth")
                                                  .schemeId(NoAuthAuthScheme.SCHEME_ID)
                                                  // .putIdentityProperty(null, null)
                                                  // .putSignerProperty(null, null)
                                                  .build();
        AcmClient sraClient =
            AcmClient.builder()
                     .authSchemeProvider(p -> singletonList(noAuth))
                     // TODO: review: This is not needed because NoAuthAuthScheme is configured on client by default.
                     // .putAuthScheme(NoAuthAuthScheme.create())
                     .build();

        sraClient.listCertificates();
    }

    @Test
    void enableSigv4a() {
        // Before
        // Requires Dependency: auth-crt

        ClientOverrideConfiguration config =
            ClientOverrideConfiguration.builder()
                                       .putExecutionAttribute(AwsSignerExecutionAttribute.SIGNING_REGION_SCOPE, // (Internal API)
                                                              RegionScope.GLOBAL)
                                       .putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                          AwsCrtV4aSigner.create())
                                       .build();

        AcmClient client =
            AcmClient.builder()
                     .overrideConfiguration(config)
                     .build();

        client.listCertificates();

        // After
        // Requires dependency: http-auth-aws, and http-auth-aws-crt for dynamically loading the crt (only) implementation

        AuthSchemeOption sigv4aAuth =
            AuthSchemeOption.builder()
                            .schemeId(AwsV4aAuthScheme.SCHEME_ID) // comes from http-auth-aws

                            // TODO: This is incorrect right now, needs to change to parameter that's a collection for scope
                            // https://sim.amazon.com/issues/SMITHY-1989
                            // TODO: Q: Not sure if it is going to be a required SignerProperty or if the signer will default to
                            // this
                            .putSignerProperty(AwsV4aHttpSigner.REGION_NAME, "*")

                            // TODO: Q: It is interesting that this fails on SERVICE_SIGNING_NAME not set, so required to be set.
                            //  With Matt's adapter changes, or Property sharing changes, will it not be required?? What's the
                            //  desired behavior?
                            //  Q: AwsV4aHttpSigner's doc will read as AwsV4aHttpSigner.SERVICE_SIGNING_NAME is required, but if
                            //  the customer doesn't need to set it, is that good?

                            .putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, "acm")
                            .build();

        AcmClient sraClient =
            AcmClient.builder()
                     .authSchemeProvider(p -> singletonList(sigv4aAuth))
                     .putAuthScheme(AwsV4aAuthScheme.create())
                     .build();

        sraClient.listCertificates();
    }


    @Test // very unlikely use case. could achieve what's needed with changing SignerProperties
    void useDifferentSigner() {
        // Before
        ClientOverrideConfiguration config =
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(SdkAdvancedClientOption.SIGNER,
                                                          new MyCustomSigV4Signer())
                                       .build();

        AcmClient client =
            AcmClient.builder()
                     .overrideConfiguration(config)
                     .build();

        client.listCertificates();

        // After
        AcmClient sraClient =
            AcmClient.builder()
                     .putAuthScheme(new MyV4AuthSchemeWithCustomSigner())
                     // .putAuthScheme(new AwsV4AuthScheme() {
                     //     @Override
                     //     public AwsV4HttpSigner signer() {
                     //         return new MyCustomSigV4NewHttpSigner();
                     //     }
                     // })
                     .build();

        sraClient.listCertificates();

    }

    private class MyCustomSigV4Signer implements Signer {
        @Override
        public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
            return null;
        }
    }

    private class MyCustomSigV4NewHttpSigner implements HttpSigner<AwsCredentialsIdentity> {
        @Override
        public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
            return null;
        }

        @Override
        public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
            return null;
        }
    }

    private class MyV4AuthSchemeWithCustomSigner implements AuthScheme<AwsCredentialsIdentity> {
        @Override
        public String schemeId() {
            return AwsV4AuthScheme.SCHEME_ID;
        }

        @Override
        public IdentityProvider<AwsCredentialsIdentity> identityProvider(IdentityProviderConfiguration providers) {
            return providers.identityProvider(AwsCredentialsIdentity.class);
        }

        @Override
        public HttpSigner<AwsCredentialsIdentity> signer() {
            return new HttpSigner<AwsCredentialsIdentity>() {
                @Override
                public SyncSignedRequest sign(SyncSignRequest<? extends AwsCredentialsIdentity> request) {
                    return null;
                }

                @Override
                public CompletableFuture<AsyncSignedRequest> signAsync(AsyncSignRequest<? extends AwsCredentialsIdentity> request) {
                    return null;
                }
            };
        }
    }

    @Test
    void s3PayloadSigningOnByDefault() {
        S3Client sraS3Client =
            S3Client.builder()
                    .authSchemeProvider(new PayloadSigningEnabledS3AuthSchemeProvider())
                    .build();

        sraS3Client.putObject(builder -> builder.bucket("b").key("k"), RequestBody.fromString("contents"));
    }

    private class PayloadSigningEnabledS3AuthSchemeProvider implements S3AuthSchemeProvider {

        S3AuthSchemeProvider defaultS3AuthSchemeProvider = S3AuthSchemeProvider.defaultProvider();

        @Override
        public List<AuthSchemeOption> resolveAuthScheme(S3AuthSchemeParams authSchemeParams) {
            return defaultS3AuthSchemeProvider
                .resolveAuthScheme(authSchemeParams)
                .stream()
                .map(authSchemeOption -> {
                    if (authSchemeOption.schemeId().equals(AwsV4AuthScheme.SCHEME_ID)) {
                        return authSchemeOption
                            .toBuilder()
                            .putSignerProperty(AwsV4HttpSigner.PAYLOAD_SIGNING_ENABLED, true)
                            .build();
                    }
                    return authSchemeOption;
                })
                .collect(Collectors.toList());
        }
    }

    // Covered in Identity surface area review - https://quip-amazon.com/hkexAI3EY2JY
    @Test
    void requestOverrideIdentityProvider() {
        AcmClient sraClient = AcmClient.create();
        sraClient.listCertificates(ListCertificatesRequest.builder()
                                                          .overrideConfiguration(
                                                              r -> r.credentialsProvider(new MyOdinIdentityProvider()))
                                                          .build());
    }

    private class MyOdinIdentityProvider implements IdentityProvider<AwsCredentialsIdentity> {
        @Override
        public Class<AwsCredentialsIdentity> identityType() {
            return null;
        }

        @Override
        public CompletableFuture<? extends AwsCredentialsIdentity> resolveIdentity(ResolveIdentityRequest request) {
            return null;
        }
    }
}
