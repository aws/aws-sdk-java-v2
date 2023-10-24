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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.CredentialType;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthScheme;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.AsyncSignedRequest;
import software.amazon.awssdk.http.auth.spi.signer.BaseSignRequest;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.identity.spi.IdentityProvider;
import software.amazon.awssdk.identity.spi.IdentityProviders;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.services.endpointproviders.EndpointInterceptorTests;
import software.amazon.awssdk.services.protocolquery.ProtocolQueryClient;
import software.amazon.awssdk.utils.CompletableFutureUtils;

@RunWith(MockitoJUnitRunner.class)
public class SraIdentityResolutionTest {

    @Mock
    private AwsCredentialsProvider credsProvider;

    @Test
    public void testIdentityPropertyBasedResolutionIsUsedAndNotAnotherIdentityResolution() {
        SdkHttpClient mockClient = mock(SdkHttpClient.class);
        when(mockClient.prepareRequest(any())).thenThrow(new RuntimeException("boom"));

        when(credsProvider.identityType()).thenReturn(AwsCredentialsIdentity.class);
        when(credsProvider.resolveIdentity(any(ResolveIdentityRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(AwsBasicCredentials.create("akid1", "skid2")));

        ProtocolQueryClient syncClient = ProtocolQueryClient
            .builder()
            .httpClient(mockClient)
            .credentialsProvider(credsProvider)
            // Below is necessary to create the test case where, addCredentialsToExecutionAttributes was getting called before
            .overrideConfiguration(ClientOverrideConfiguration.builder().build())
            .build();

        assertThatThrownBy(() -> syncClient.allTypes(r -> {
        })).hasMessageContaining("boom");
        verify(credsProvider, times(2)).identityType();

        // This asserts that the identity used is the one from resolveIdentity() called by SRA AuthSchemeInterceptor and not from
        // from another call like from AwsCredentialsAuthorizationStrategy.addCredentialsToExecutionAttributes, asserted by
        // combination of times(1) and verifyNoMoreInteractions.
        verify(credsProvider, times(1)).resolveIdentity(any(ResolveIdentityRequest.class));
        verifyNoMoreInteractions(credsProvider);
    }

}