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

package software.amazon.awssdk.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryServiceClientConfiguration;
import software.amazon.awssdk.utils.Validate;

@RunWith(MockitoJUnitRunner.class)
public class SraIdentityResolutionUsingPluginsTest {

    @Mock
    private AwsCredentialsProvider credentialsProvider;

    @Test
    public void testIdentityBasedPluginsResolutionIsUsedAndNotAnotherIdentityResolution() {
        when(credentialsProvider.identityType()).thenReturn(AwsCredentialsIdentity.class);
        when(credentialsProvider.resolveIdentity(any(ResolveIdentityRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AwsBasicCredentials.create("akid1", "skid2")));

        ProtocolQueryClient syncClient = ProtocolQueryClient
            .builder()
            .addPlugin(new TestPlugin(credentialsProvider))
            // Below is necessary to create the test case where, addCredentialsToExecutionAttributes was getting called before
            .overrideConfiguration(ClientOverrideConfiguration.builder().build())
            .build();

        try {
            syncClient.allTypes(builder -> {
            });
        } catch (Exception expected) {
            expected.printStackTrace(System.out);
        }

        verify(credentialsProvider, times(2)).identityType();

        // This asserts that the identity used is the one from resolveIdentity() called by SRA AuthSchemeInterceptor and not
        // from another call like from AwsCredentialsAuthorizationStrategy.addCredentialsToExecutionAttributes, asserted by
        // combination of times(1) and verifyNoMoreInteractions.
        verify(credentialsProvider, times(1)).resolveIdentity(any(ResolveIdentityRequest.class));
        verifyNoMoreInteractions(credentialsProvider);
    }

    static class TestPlugin implements SdkPlugin {
        private final AwsCredentialsProvider credentialsProvider;

        TestPlugin(AwsCredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
        }

        @Override
        public void configureClient(SdkServiceClientConfiguration.Builder config) {
            ProtocolQueryServiceClientConfiguration.Builder builder =
                Validate.isInstanceOf(ProtocolQueryServiceClientConfiguration.Builder.class,
                                      config,
                                      "Expecting an instance of " +
                                      ProtocolQueryServiceClientConfiguration.class);
            builder.credentialsProvider(credentialsProvider);
        }
    }
}
