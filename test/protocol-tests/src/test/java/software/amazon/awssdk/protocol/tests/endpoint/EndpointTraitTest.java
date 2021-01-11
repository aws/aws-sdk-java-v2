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

package software.amazon.awssdk.protocol.tests.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocoljsonendpointtrait.ProtocolJsonEndpointTraitClient;
import software.amazon.awssdk.services.protocoljsonendpointtrait.ProtocolJsonEndpointTraitClientBuilder;
import software.amazon.awssdk.services.protocoljsonendpointtrait.model.EndpointTraitOneRequest;
import software.amazon.awssdk.services.protocoljsonendpointtrait.model.EndpointTraitTwoRequest;

@RunWith(MockitoJUnitRunner.class)
public class EndpointTraitTest {

    @Mock
    private SdkHttpClient mockHttpClient;

    @Mock
    private ExecutableHttpRequest abortableCallable;

    private ProtocolJsonEndpointTraitClient client;

    private ProtocolJsonEndpointTraitClient clientWithDisabledHostPrefix;

    @Before
    public void setup() throws Exception {
        client = clientBuilder().build();

        clientWithDisabledHostPrefix = clientBuilder().overrideConfiguration(
            ClientOverrideConfiguration.builder()
                                       .putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true)
                                       .build()).build();

        when(mockHttpClient.prepareRequest(any())).thenReturn(abortableCallable);

        when(abortableCallable.call()).thenThrow(SdkClientException.create("Dummy exception"));
    }

    @Test
    public void hostExpression_withoutInputMemberLabel() throws URISyntaxException {
        try {
            client.endpointTraitOne(EndpointTraitOneRequest.builder().build());
            Assert.fail("Expected an exception");
        } catch (SdkClientException exception) {
            ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
            verify(mockHttpClient).prepareRequest(httpRequestCaptor.capture());

            SdkHttpRequest request = httpRequestCaptor.getAllValues().get(0).httpRequest();
            assertThat(request.host()).isEqualTo("data.localhost.com");
            assertThat(request.port()).isEqualTo(443);
            assertThat(request.encodedPath()).isEqualTo("/");
            assertThat(request.getUri()).isEqualTo(new URI("http://data.localhost.com:443/"));
        }
    }

    @Test
    public void hostExpression_withInputMemberLabel() throws URISyntaxException {
        try {
            client.endpointTraitTwo(EndpointTraitTwoRequest.builder()
                                                           .stringMember("123456")
                                                           .pathIdempotentToken("dummypath")
                                                           .build());
            Assert.fail("Expected an exception");
        } catch (SdkClientException exception) {
            ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
            verify(mockHttpClient).prepareRequest(httpRequestCaptor.capture());

            SdkHttpRequest request = httpRequestCaptor.getAllValues().get(0).httpRequest();
            assertThat(request.host()).isEqualTo("123456-localhost.com");
            assertThat(request.port()).isEqualTo(443);
            assertThat(request.encodedPath()).isEqualTo("/dummypath");
            assertThat(request.getUri()).isEqualTo(new URI("http://123456-localhost.com:443/dummypath"));
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void validationException_whenInputMember_inHostPrefix_isNull() {
        client.endpointTraitTwo(EndpointTraitTwoRequest.builder().build());
    }

    @Test (expected = IllegalArgumentException.class)
    public void validationException_whenInputMember_inHostPrefix_isEmpty() {
        client.endpointTraitTwo(EndpointTraitTwoRequest.builder().stringMember("").build());
    }

    @Test
    public void clientWithDisabledHostPrefix_withoutInputMemberLabel_usesOriginalUri() {
        try {
            clientWithDisabledHostPrefix.endpointTraitOne(EndpointTraitOneRequest.builder().build());
            Assert.fail("Expected an exception");
        } catch (SdkClientException exception) {
            ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
            verify(mockHttpClient).prepareRequest(httpRequestCaptor.capture());

            SdkHttpRequest request = httpRequestCaptor.getAllValues().get(0).httpRequest();
            assertThat(request.host()).isEqualTo("localhost.com");
        }
    }

    @Test
    public void clientWithDisabledHostPrefix_withInputMemberLabel_usesOriginalUri() {
        try {
            clientWithDisabledHostPrefix.endpointTraitTwo(EndpointTraitTwoRequest.builder()
                                                                                 .stringMember("123456")
                                                                                 .build());
            Assert.fail("Expected an exception");
        } catch (SdkClientException exception) {
            ArgumentCaptor<HttpExecuteRequest> httpRequestCaptor = ArgumentCaptor.forClass(HttpExecuteRequest.class);
            verify(mockHttpClient).prepareRequest(httpRequestCaptor.capture());

            SdkHttpRequest request = httpRequestCaptor.getAllValues().get(0).httpRequest();
            assertThat(request.host()).isEqualTo("localhost.com");
        }
    }

    private StaticCredentialsProvider mockCredentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));
    }

    private String getEndpoint() {
        return "http://localhost.com:443";
    }

    private ProtocolJsonEndpointTraitClientBuilder clientBuilder() {
        return ProtocolJsonEndpointTraitClient.builder()
                                              .httpClient(mockHttpClient)
                                              .credentialsProvider(mockCredentials())
                                              .region(Region.US_EAST_1)
                                              .endpointOverride(URI.create(getEndpoint()));
    }
}
