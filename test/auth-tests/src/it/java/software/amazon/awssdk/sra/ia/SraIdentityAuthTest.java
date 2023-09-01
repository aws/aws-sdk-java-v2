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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.NoAuthAuthScheme;
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
    public void disableSigning() {
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

        // After
        AuthSchemeOption noAuth = AuthSchemeOption.builder()
                                                  .schemeId("smithy.api#noAuth")
                                                  .schemeId(NoAuthAuthScheme.SCHEME_ID)
                                                  // .putIdentityProperty(null, null)
                                                  // .putSignerProperty(null, null)
                                                  .build();
        client =
            AcmClient.builder()
                     .authSchemeProvider(p -> singletonList(noAuth))
                     .build();

        client.listCertificates();
    }
}
