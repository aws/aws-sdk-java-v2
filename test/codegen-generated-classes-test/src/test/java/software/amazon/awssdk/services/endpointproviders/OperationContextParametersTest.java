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

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;
import software.amazon.awssdk.services.restjsonendpointproviders.model.NestedContainersOperationRequest;
import software.amazon.awssdk.services.restjsonendpointproviders.model.NestedContainersOperationResponse;
import software.amazon.awssdk.services.restjsonendpointproviders.model.NestedContainersStructure;
import software.amazon.awssdk.services.restjsonendpointproviders.model.PayloadStructType;
import software.amazon.awssdk.testutils.LogCaptor;

class OperationContextParametersTest extends LogCaptor.LogCaptorTestBase {
    private static final AwsCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("akid", "skid"));

    private static final Region REGION = Region.of("us-east-9000");

    private RestJsonEndpointProvidersEndpointProvider mockEndpointProvider;

    @BeforeEach
    public void setup() {
        mockEndpointProvider = mock(RestJsonEndpointProvidersEndpointProvider.class);
        when(mockEndpointProvider.resolveEndpoint(any(RestJsonEndpointProvidersEndpointParams.class)))
            .thenThrow(new RuntimeException("boom"));
    }

    static Stream<Arguments> paramAssertions() {
        return Stream.of(
            Arguments.of("Single String parameter", new TestAssertion(
                p -> assertThat(p.pojoString()).isEqualTo("StringMemberA"))),

            Arguments.of("List of String parameter", new TestAssertion(
                p -> assertThat(p.basicListOfString()).isNotEmpty().hasSize(2).containsExactly("StringA", "StringB"))),

            Arguments.of("List of String parameter from wildcard with null members", new TestAssertion(
                p -> assertThat(p.wildcardKeyListOfString()).isNotEmpty().hasSize(1)
                                                            .containsExactly("StringMemberS2"))),

            Arguments.of("List of String parameter from 'keys' function", new TestAssertion(
                p -> assertThat(p.keysListOfString()).isNotEmpty().hasSize(2)
                                                     .containsExactly("PayloadMemberOne", "PayloadMemberTwo"))),

            Arguments.of("List of String parameter is empty if request does not have list elements", new TestAssertion(
                p -> assertThat(p.missingRequestValuesListOfString()).isEmpty()))
        );
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("paramAssertions")
    void operationContextParams_customization_resolvedCorrectly(String description, TestAssertion assertion) {
        assertThatThrownBy(this::createClientAndCallApi).isInstanceOf(RuntimeException.class);

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());
        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        assertion.verify.accept(params);
    }

    private NestedContainersOperationResponse createClientAndCallApi() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();
        return client.nestedContainersOperation(createRequestObject());
    }

    private static NestedContainersOperationRequest createRequestObject() {
        NestedContainersStructure pojoInListWithoutProjectionMember = NestedContainersStructure.builder().build();
        NestedContainersStructure pojoInListWithProjectionMember = NestedContainersStructure.builder()
                                                                                            .stringMember("StringMemberS2")
                                                                                            .build();
        NestedContainersStructure nested = NestedContainersStructure.builder()
                                                                    .stringMember("StringMemberA")
                                                                    .listOfNested(pojoInListWithoutProjectionMember,
                                                                                  pojoInListWithProjectionMember)
                                                                    .build();
        PayloadStructType pojoKeys = PayloadStructType.builder().payloadMemberOne("p1").payloadMemberTwo("p2").build();

        return NestedContainersOperationRequest.builder()
                                               .nested(nested)
                                               .listOfString("StringA", "StringB")
                                               .listOfNested(pojoInListWithoutProjectionMember, pojoInListWithProjectionMember)
                                               .pojoKeys(pojoKeys)
                                               .build();
    }

    private static class TestAssertion {
        private final Consumer<RestJsonEndpointProvidersEndpointParams> verify;

        TestAssertion(Consumer<RestJsonEndpointProvidersEndpointParams> verify) {
            this.verify = verify;
        }
    }
}
