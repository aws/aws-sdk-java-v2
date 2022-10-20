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

package software.amazon.awssdk.services.endpointproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;

public class ParametersTest {
    private static final AwsCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("akid", "skid"));

    private static final Region REGION = Region.of("us-east-9000");

    private RestJsonEndpointProvidersEndpointProvider mockEndpointProvider;

    @Before
    public void setup() {
        mockEndpointProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
        when(mockEndpointProvider.resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class)))
            .thenThrow(new RuntimeException("boom"));
    }

    @Test
    public void parametersObject_defaultStringParam_isPresent() {
        RestJsonEndpointProvidersEndpointParams params = RestJsonEndpointProvidersEndpointParams.builder().build();
        assertThat(params.regionWithDefault()).isEqualTo(Region.of("us-east-1"));
    }

    @Test
    public void parametersObject_defaultBooleanParam_isPresent() {
        RestJsonEndpointProvidersEndpointParams params = RestJsonEndpointProvidersEndpointParams.builder().build();
        assertThat(params.useFips()).isEqualTo(false);
    }

    @Test
    public void parametersObject_defaultStringParam_customValue_isPresent() {
        RestJsonEndpointProvidersEndpointParams params = RestJsonEndpointProvidersEndpointParams.builder()
                                                                                                .regionWithDefault(Region.of(
                                                                                                    "us-east-1000"))
                                                                                                .build();
        assertThat(params.regionWithDefault()).isEqualTo(Region.of("us-east-1000"));
    }

    @Test
    public void parametersObject_defaultBooleanParam_customValue_isPresent() {
        RestJsonEndpointProvidersEndpointParams params = RestJsonEndpointProvidersEndpointParams.builder()
                                                                                                .useFips(true)
                                                                                                .build();
        assertThat(params.useFips()).isEqualTo(true);
    }

    @Test
    public void parametersObject_defaultStringParam_setToNull_usesDefaultValue() {
        RestJsonEndpointProvidersEndpointParams params = RestJsonEndpointProvidersEndpointParams.builder()
                                                                                                .regionWithDefault(null)
                                                                                                .build();
        assertThat(params.regionWithDefault()).isEqualTo(Region.of("us-east-1"));
    }

    @Test
    public void parametersObject_defaultBooleanParam_setToNull_usesDefaultValue() {
        RestJsonEndpointProvidersEndpointParams params = RestJsonEndpointProvidersEndpointParams.builder()
                                                                                                .useFips(null)
                                                                                                .build();
        assertThat(params.useFips()).isEqualTo(false);
    }

    @Test
    public void regionBuiltIn_resolvedCorrectly() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        }));

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertThat(params.region()).isEqualTo(REGION);
    }

    @Test
    public void dualStackBuiltIn_resolvedCorrectly() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .dualstackEnabled(true)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        }));

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertThat(params.useDualStack()).isEqualTo(true);
    }

    @Test
    public void fipsBuiltIn_resolvedCorrectly() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .fipsEnabled(true)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        }));

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertThat(params.useFips()).isEqualTo(true);
    }

    @Test
    public void staticContextParams_OperationWithStaticContextParamA_resolvedCorrectly() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();

        assertThatThrownBy(() -> client.operationWithStaticContextParamA(r -> {
        }));

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertThat(params.staticStringParam()).isEqualTo("operation A");
    }

    @Test
    public void staticContextParams_OperationWithStaticContextParamB_resolvedCorrectly() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();

        assertThatThrownBy(() -> client.operationWithStaticContextParamB(r -> {
        }));

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertThat(params.staticStringParam()).isEqualTo("operation B");
    }

    @Test
    public void contextParams_OperationWithContextParam_resolvedCorrectly() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();

        assertThatThrownBy(() -> client.operationWithContextParam(r -> r.stringMember("foobar")));

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertThat(params.operationContextParam()).isEqualTo("foobar");
    }

    @Test
    public void clientContextParams_setOnBuilder_resolvedCorrectly() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .stringClientContextParam("foobar")
                                                                                .booleanClientContextParam(true)
                                                                                .build();

        assertThatThrownBy(() -> client.operationWithNoInputOrOutput(r -> {
        }));

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());

        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertThat(params.stringClientContextParam()).isEqualTo("foobar");
        assertThat(params.booleanClientContextParam()).isTrue();
    }
}
