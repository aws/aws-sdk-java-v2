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

package software.amazon.awssdk.services.protocolrestjson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.services.protocolrestjson.model.NestedQueryParameterOperation;
import software.amazon.awssdk.services.protocolrestjson.model.QueryParameterOperationRequest;
import software.amazon.awssdk.services.protocolrestjson.transform.QueryParameterOperationRequestMarshaller;

class ServiceRequestRequiredValidationMarshallingTest {

    private static QueryParameterOperationRequestMarshaller marshaller;
    private static AwsJsonProtocolFactory awsJsonProtocolFactory;

    @BeforeAll
    static void setup() {
        awsJsonProtocolFactory = AwsJsonProtocolFactory
            .builder()
            .clientConfiguration(SdkClientConfiguration
                                     .builder()
                                     .option(SdkClientOption.ENDPOINT, URI.create("http://localhost"))
                                     .build())
            .build();
        marshaller = new QueryParameterOperationRequestMarshaller(awsJsonProtocolFactory);
    }

    @Test
    void marshal_notMissingRequiredMembers_doesNotThrowException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_missingRequiredMemberAtQueryParameterLocation_throwsException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne(null)
                .queryParamTwo("myParamTwo")
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Parameter 'QueryParamOne' must not be null");
    }

    @Test
    void marshal_missingRequiredMemberListAtQueryParameterLocation_defaultsToEmptyCollectionAndDoesNotThrowException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .requiredListQueryParams((Collection<Integer>) null)
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThat(request.requiredListQueryParams()).isEmpty();
        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_forceMissingRequiredMemberListAtQueryParameterLocation_throwsException() {
        QueryParameterOperationRequest.Builder builder = QueryParameterOperationRequest
            .builder()
            .pathParam("myPathParam")
            .stringHeaderMember("myHeader")
            .queryParamOne("myParamOne")
            .queryParamTwo("myParamTwo")
            .nestedQueryParameterOperation(NestedQueryParameterOperation
                                               .builder()
                                               .queryParamOne("myNestedParamOne")
                                               .queryParamTwo("myNestedParamTwo")
                                               .build());
        forceNull(builder, "requiredListQueryParams");
        QueryParameterOperationRequest request = builder.build();

        assertThat(request.requiredListQueryParams()).isNull();
        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Parameter 'RequiredListQueryParams' must not be null");
    }

    @Test
    void marshal_missingRequiredMemberOfListAtQueryParameterLocation_doesNotThrowException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .requiredListQueryParams(1, null, 3)
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_missingNonRequiredMemberListAtQueryParameterLocation_defaultsToEmptyCollectionAndDoesNotThrowException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .optionalListQueryParams((Collection<String>) null)
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThat(request.optionalListQueryParams()).isEmpty();
        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_forceMissingNonRequiredMemberListAtQueryParameterLocation_doesNotThrowException() {
        QueryParameterOperationRequest.Builder builder = QueryParameterOperationRequest
            .builder()
            .pathParam("myPathParam")
            .stringHeaderMember("myHeader")
            .queryParamOne("myParamOne")
            .queryParamTwo("myParamTwo")
            .nestedQueryParameterOperation(NestedQueryParameterOperation
                                               .builder()
                                               .queryParamOne("myNestedParamOne")
                                               .queryParamTwo("myNestedParamTwo")
                                               .build());
        forceNull(builder, "optionalListQueryParams");
        QueryParameterOperationRequest request = builder.build();

        assertThat(request.optionalListQueryParams()).isNull();
        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_missingNonRequiredMemberOfListAtQueryParameterLocation_doesNotThrowException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .optionalListQueryParams("1", null, "3")
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_missingRequiredMemberMapAtQueryParameterLocation_defaultsToEmptyCollectionAndDoesNotThrowException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .requiredMapQueryParams(null)
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThat(request.requiredMapQueryParams()).isEmpty();
        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_forceMissingRequiredMemberMaoAtQueryParameterLocation_throwsException() {
        QueryParameterOperationRequest.Builder builder = QueryParameterOperationRequest
            .builder()
            .pathParam("myPathParam")
            .stringHeaderMember("myHeader")
            .queryParamOne("myParamOne")
            .queryParamTwo("myParamTwo")
            .nestedQueryParameterOperation(NestedQueryParameterOperation
                                               .builder()
                                               .queryParamOne("myNestedParamOne")
                                               .queryParamTwo("myNestedParamTwo")
                                               .build());
        forceNull(builder, "requiredMapQueryParams");
        QueryParameterOperationRequest request = builder.build();

        assertThat(request.requiredMapQueryParams()).isNull();
        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Parameter 'RequiredMapQueryParams' must not be null");
    }

    @Test
    void marshal_missingRequiredMemberOfMapAtQueryParameterLocation_doesNotThrowException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .requiredListQueryParams()
                .optionalListQueryParams()
                .requiredMapQueryParams(Stream.of(new String[][] {{"key1", "value1"}, {"key2", null}})
                                            .collect(HashMap::new, (map, array) -> map.put(array[0], array[1]), HashMap::putAll))
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
    }

    @Test
    void marshal_missingRequiredMemberAtUriLocation_throwsException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam(null)
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Parameter 'PathParam' must not be null");
    }

    @Test
    void marshal_missingRequiredMemberAtHeaderLocation_throwsException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember(null)
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne("myNestedParamOne")
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Parameter 'x-amz-header-string' must not be null");
    }

    @Test
    void marshal_missingRequiredMemberAtQueryParameterLocationOfNestedShape_throwsException() {
        QueryParameterOperationRequest request =
            QueryParameterOperationRequest
                .builder()
                .pathParam("myPathParam")
                .stringHeaderMember("myHeader")
                .queryParamOne("myParamOne")
                .queryParamTwo("myParamTwo")
                .nestedQueryParameterOperation(NestedQueryParameterOperation
                                                   .builder()
                                                   .queryParamOne(null)
                                                   .queryParamTwo("myNestedParamTwo")
                                                   .build())
                .build();

        assertThatThrownBy(() -> marshaller.marshall(request))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Parameter 'QueryParamOne' must not be null");
    }

    private static void forceNull(QueryParameterOperationRequest.Builder builder, String fieldName) {
        try {
            Field field = builder.getClass().getDeclaredField(fieldName);
            boolean originallyAccessible = field.isAccessible();
            if (!originallyAccessible) {
                field.setAccessible(true);
            }
            field.set(builder, null);
            if (!originallyAccessible) {
                field.setAccessible(false);
            }
        } catch (NoSuchFieldException|IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
