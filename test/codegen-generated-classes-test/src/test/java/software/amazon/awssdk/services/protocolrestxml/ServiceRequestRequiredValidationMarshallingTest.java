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

package software.amazon.awssdk.services.protocolrestxml;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.services.protocolrestxml.model.NestedQueryParameterOperation;
import software.amazon.awssdk.services.protocolrestxml.model.QueryParameterOperationRequest;
import software.amazon.awssdk.services.protocolrestxml.transform.QueryParameterOperationRequestMarshaller;

class ServiceRequestRequiredValidationMarshallingTest {

    private static QueryParameterOperationRequestMarshaller marshaller;
    private static AwsXmlProtocolFactory awsXmlProtocolFactory;

    @BeforeAll
    static void setup() {
        awsXmlProtocolFactory = AwsXmlProtocolFactory
            .builder()
            .clientConfiguration(SdkClientConfiguration
                                     .builder()
                                     .option(SdkClientOption.ENDPOINT, URI.create("http://localhost"))
                                     .build())
            .build();
        marshaller = new QueryParameterOperationRequestMarshaller(awsXmlProtocolFactory);
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
    void marshal_missingRequiredMemberAtHeaderLocation_doesNotThrowException() {
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

        assertThatNoException().isThrownBy(() -> marshaller.marshall(request));
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

}
