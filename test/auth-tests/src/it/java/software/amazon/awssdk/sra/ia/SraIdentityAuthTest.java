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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.AwsCrtV4aSigner;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.auth.aws.AwsV4aAuthScheme;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.NoAuthAuthScheme;
import software.amazon.awssdk.regions.RegionScope;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.codecatalyst.CodeCatalystClient;

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
}
