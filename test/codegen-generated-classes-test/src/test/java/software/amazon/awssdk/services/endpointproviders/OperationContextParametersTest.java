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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.restjsonendpointproviders.RestJsonEndpointProvidersClient;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointParams;
import software.amazon.awssdk.services.restjsonendpointproviders.endpoints.RestJsonEndpointProvidersEndpointProvider;
import software.amazon.awssdk.services.restjsonendpointproviders.model.NestedContainersOperationResponse;
import software.amazon.awssdk.services.restjsonendpointproviders.model.NestedContainersStructure;

class OperationContextParametersTest {
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

    @ParameterizedTest
    @MethodSource("testCases")
    void operationContextParams_customization_resolvedCorrectly(TestCase tc) {
        assertThatThrownBy(this::createClientAndCallApi).isInstanceOf(RuntimeException.class);

        ArgumentCaptor<RestJsonEndpointProvidersEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(RestJsonEndpointProvidersEndpointParams.class);

        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());
        RestJsonEndpointProvidersEndpointParams params = paramsCaptor.getValue();

        tc.verify.accept(params);
    }

    static List<TestCase> testCases() {
        List<TestCase> testCases = new ArrayList<>();

        testCases.add(new TestCase(p -> {
            String listOfPojoKeysArray = p.pojoString();
            assertThat(listOfPojoKeysArray).isEqualTo("StringMemberA");
        }));

        testCases.add(new TestCase(p -> {
            List<String> listOfPojoKeysArray = p.basicListOfString();
            assertThat(listOfPojoKeysArray).isNotEmpty().hasSize(2).containsExactly("StringA", "StringB");
        }));

        return testCases;
    }

    private NestedContainersOperationResponse createClientAndCallApi() {
        RestJsonEndpointProvidersClient client = RestJsonEndpointProvidersClient.builder()
                                                                                .region(REGION)
                                                                                .credentialsProvider(CREDENTIALS)
                                                                                .endpointProvider(mockEndpointProvider)
                                                                                .build();

        NestedContainersStructure nestedShallow1 = NestedContainersStructure.builder()
                                                                            .stringMember("StringMemberS1")
                                                                            .build();
        NestedContainersStructure nestedShallow2 = NestedContainersStructure.builder()
                                                                            .stringMember("StringMemberS2")
                                                                            .build();
        NestedContainersStructure nested = NestedContainersStructure.builder()
                                                                    .stringMember("StringMemberA")
                                                                    .listOfNested(nestedShallow1, nestedShallow2)
                                                                    .build();
        return client.nestedContainersOperation(r -> r.nested(nested)
                                                      .listOfString("StringA", "StringB")
                                                      .listOfNested(nestedShallow1, nestedShallow2));
    }

    private static class TestCase {
        private final Consumer<RestJsonEndpointProvidersEndpointParams> verify;

        TestCase(Consumer<RestJsonEndpointProvidersEndpointParams> verify) {
            this.verify = verify;
        }
    }
}
