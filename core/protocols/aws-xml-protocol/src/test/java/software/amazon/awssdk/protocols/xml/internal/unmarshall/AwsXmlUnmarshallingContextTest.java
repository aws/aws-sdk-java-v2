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

package software.amazon.awssdk.protocols.xml.internal.unmarshall;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.internal.InternalCoreExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.protocols.query.unmarshall.XmlElement;

@RunWith(MockitoJUnitRunner.class)
public class AwsXmlUnmarshallingContextTest {
    private static final XmlElement XML_ELEMENT_1 = XmlElement.builder().elementName("one").build();
    private static final XmlElement XML_ELEMENT_2 = XmlElement.builder().elementName("two").build();
    private static final XmlElement XML_ERROR_ELEMENT_1 = XmlElement.builder().elementName("error-one").build();
    private static final XmlElement XML_ERROR_ELEMENT_2 = XmlElement.builder().elementName("error-two").build();
    private static final ExecutionAttributes EXECUTION_ATTRIBUTES_1 =
        new ExecutionAttributes().putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, 1);
    private static final ExecutionAttributes EXECUTION_ATTRIBUTES_2 =
        new ExecutionAttributes().putAttribute(InternalCoreExecutionAttribute.EXECUTION_ATTEMPT, 2);

    @Mock
    private SdkHttpFullResponse mockSdkHttpFullResponse;

    private AwsXmlUnmarshallingContext minimal() {
        return AwsXmlUnmarshallingContext.builder().build();
    }

    private AwsXmlUnmarshallingContext maximal() {
        return AwsXmlUnmarshallingContext.builder()
                                         .parsedXml(XML_ELEMENT_1)
                                         .parsedErrorXml(XML_ERROR_ELEMENT_1)
                                         .isResponseSuccess(true)
                                         .sdkHttpFullResponse(mockSdkHttpFullResponse)
                                         .executionAttributes(EXECUTION_ATTRIBUTES_1)
                                         .build();
    }

    @Test
    public void builder_minimal() {
        AwsXmlUnmarshallingContext result = minimal();

        assertThat(result.isResponseSuccess()).isNull();
        assertThat(result.sdkHttpFullResponse()).isNull();
        assertThat(result.parsedRootXml()).isNull();
        assertThat(result.executionAttributes()).isNull();
        assertThat(result.parsedErrorXml()).isNull();
    }

    @Test
    public void builder_maximal() {
        AwsXmlUnmarshallingContext result = maximal();

        assertThat(result.isResponseSuccess()).isTrue();
        assertThat(result.sdkHttpFullResponse()).isEqualTo(mockSdkHttpFullResponse);
        assertThat(result.parsedRootXml()).isEqualTo(XML_ELEMENT_1);
        assertThat(result.executionAttributes()).isEqualTo(EXECUTION_ATTRIBUTES_1);
        assertThat(result.parsedErrorXml()).isEqualTo(XML_ERROR_ELEMENT_1);
    }

    @Test
    public void toBuilder_maximal() {
        assertThat(maximal().toBuilder().build()).isEqualTo(maximal());
    }

    @Test
    public void toBuilder_minimal() {
        assertThat(minimal().toBuilder().build()).isEqualTo(minimal());
    }

    @Test
    public void equals_maximal_positive() {
        assertThat(maximal()).isEqualTo(maximal());
    }

    @Test
    public void equals_minimal() {
        assertThat(minimal()).isEqualTo(minimal());
    }

    @Test
    public void equals_maximal_negative() {
        assertThat(maximal().toBuilder().isResponseSuccess(false).build()).isNotEqualTo(maximal());
        assertThat(maximal().toBuilder().sdkHttpFullResponse(mock(SdkHttpFullResponse.class)).build()).isNotEqualTo(maximal());
        assertThat(maximal().toBuilder().parsedXml(XML_ELEMENT_2).build()).isNotEqualTo(maximal());
        assertThat(maximal().toBuilder().parsedErrorXml(XML_ERROR_ELEMENT_2).build()).isNotEqualTo(maximal());
        assertThat(maximal().toBuilder().executionAttributes(EXECUTION_ATTRIBUTES_2).build()).isNotEqualTo(maximal());
    }

    @Test
    public void hashcode_maximal_positive() {
        assertThat(maximal().hashCode()).isEqualTo(maximal().hashCode());
    }

    @Test
    public void hashcode_minimal_positive() {
        assertThat(minimal().hashCode()).isEqualTo(minimal().hashCode());
    }

    @Test
    public void hashcode_maximal_negative() {
        assertThat(maximal().toBuilder().isResponseSuccess(false).build().hashCode())
            .isNotEqualTo(maximal().hashCode());
        assertThat(maximal().toBuilder().sdkHttpFullResponse(mock(SdkHttpFullResponse.class)).build().hashCode())
            .isNotEqualTo(maximal().hashCode());
        assertThat(maximal().toBuilder().parsedXml(XML_ELEMENT_2).build().hashCode())
            .isNotEqualTo(maximal().hashCode());
        assertThat(maximal().toBuilder().parsedErrorXml(XML_ERROR_ELEMENT_2).build().hashCode())
            .isNotEqualTo(maximal().hashCode());
        assertThat(maximal().toBuilder().executionAttributes(EXECUTION_ATTRIBUTES_2).build().hashCode())
            .isNotEqualTo(maximal().hashCode());
    }

}