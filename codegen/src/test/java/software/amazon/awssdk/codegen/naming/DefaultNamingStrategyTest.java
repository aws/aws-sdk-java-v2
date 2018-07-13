/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.naming;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.customization.ShareModelConfig;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

@RunWith(MockitoJUnitRunner.class)
public class DefaultNamingStrategyTest {

    private ServiceModel serviceModel = mock(ServiceModel.class);

    @Mock
    private Map<String, Shape> mockShapeMap;

    @Mock
    private Shape mockShape;

    @Mock
    private Member member;

    private DefaultNamingStrategy strat = new DefaultNamingStrategy(serviceModel, null);

    @Test
    public void canConvertStringsWithNonAlphasToClassNames() {
        String anInvalidClassName = "a phrase-With_other.delimiters";
        assertThat(strat.getJavaClassName(anInvalidClassName)).isEqualTo("APhraseWithOtherDelimiters");
    }

    @Test
    public void canConvertAuthorizerStartingWithNumber() {
        String anInvalidClassName = "35-authorizer-implementation";
        assertThat(strat.getAuthorizerClassName(anInvalidClassName)).isEqualTo("I35AuthorizerImplementation");
    }

    @Test
    public void enumNamesConvertCorrectly() {
        validateConversion("Twilio-Sms", "TWILIO_SMS");
        validateConversion("t2.micro", "T2_MICRO");
        validateConversion("GreaterThanThreshold", "GREATER_THAN_THRESHOLD");
        validateConversion("INITIALIZED", "INITIALIZED");
        validateConversion("GENERIC_EVENT", "GENERIC_EVENT");
        validateConversion("WINDOWS_2012", "WINDOWS_2012");
        validateConversion("ec2:spot-fleet-request:TargetCapacity", "EC2_SPOT_FLEET_REQUEST_TARGET_CAPACITY");
        validateConversion("elasticmapreduce:instancegroup:InstanceCount", "ELASTICMAPREDUCE_INSTANCEGROUP_INSTANCE_COUNT");
        validateConversion("application/vnd.amazonaws.card.generic", "APPLICATION_VND_AMAZONAWS_CARD_GENERIC");
        validateConversion("IPV4", "IPV4");
        validateConversion("ipv4", "IPV4");
        validateConversion("IPv4", "IP_V4");
        validateConversion("ipV4", "IP_V4");
        validateConversion("IPMatch", "IP_MATCH");
        validateConversion("S3", "S3");
        validateConversion("EC2Instance", "EC2_INSTANCE");
        validateConversion("aws.config", "AWS_CONFIG");
        validateConversion("AWS::EC2::CustomerGateway", "AWS_EC2_CUSTOMER_GATEWAY");
        validateConversion("application/pdf", "APPLICATION_PDF");
        validateConversion("ADConnector", "AD_CONNECTOR");
        validateConversion("MS-CHAPv1", "MS_CHAP_V1");
        validateConversion("One-Way: Outgoing", "ONE_WAY_OUTGOING");
        validateConversion("scram_sha_1", "SCRAM_SHA_1");
        validateConversion("EC_prime256v1", "EC_PRIME256_V1");
        validateConversion("EC_PRIME256V1", "EC_PRIME256_V1");
        validateConversion("EC2v11.4", "EC2_V11_4");
        validateConversion("nodejs4.3-edge", "NODEJS4_3_EDGE");
        validateConversion("BUILD_GENERAL1_SMALL", "BUILD_GENERAL1_SMALL");
        validateConversion("SSE_S3", "SSE_S3");
        validateConversion("http1.1", "HTTP1_1");
        validateConversion("T100", "T100");
        validateConversion("s3:ObjectCreated:*", "S3_OBJECT_CREATED");
        validateConversion("s3:ObjectCreated:Put", "S3_OBJECT_CREATED_PUT");
        validateConversion("TLSv1", "TLS_V1");
        validateConversion("TLSv1.2", "TLS_V1_2");
        validateConversion("us-east-1", "US_EAST_1");
        validateConversion("io1", "IO1");
        validateConversion("testNESTEDAcronym", "TEST_NESTED_ACRONYM");
        validateConversion("IoT", "IOT");
        validateConversion("textORcsv", "TEXT_OR_CSV");
        validateConversion("__foo___", "FOO");
        validateConversion("TEST__FOO", "TEST_FOO");
        validateConversion("IFrame", "I_FRAME");
        validateConversion("TPain", "T_PAIN");
        validateConversion("S3EC2", "S3_EC2");
        validateConversion("S3Ec2", "S3_EC2");
        validateConversion("s3Ec2", "S3_EC2");
        validateConversion("s3ec2", "S3_EC2");
    }

    @Test
    public void test_GetFluentSetterMethodName_NoEnum() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShape.getEnumValues()).thenReturn(null);
        when(mockShape.getType()).thenReturn("foo");

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockShape)).isEqualTo("awesomeMethod");
    }

    @Test
    public void test_GetFluentSetterMethodName_NoEnum_WithList() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShapeMap.get(any())).thenReturn(mockShape);

        when(mockShape.getEnumValues()).thenReturn(null);
        when(mockShape.getType()).thenReturn("list");
        when(mockShape.getListMember()).thenReturn(member);
        when(member.getShape()).thenReturn(null);

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockShape)).isEqualTo("awesomeMethod");
    }

    @Test
    public void test_GetFluentSetterMethodName_WithEnumShape_NoListOrMap() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShapeMap.get(any())).thenReturn(mockShape);
        when(mockShape.getEnumValues()).thenReturn(new ArrayList<>());
        when(mockShape.getType()).thenReturn("foo");

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockShape)).isEqualTo("awesomeMethod");
    }

    @Test
    public void test_GetFluentSetterMethodName_WithEnumShape_WithList() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShapeMap.get(any())).thenReturn(mockShape);

        when(mockShape.getEnumValues()).thenReturn(new ArrayList<>());
        when(mockShape.getType()).thenReturn("list");
        when(mockShape.getListMember()).thenReturn(member);
        when(member.getShape()).thenReturn("Foo");

        assertThat(strat.getFluentSetterMethodName("AwesomeMethod", mockShape)).isEqualTo("awesomeMethodWithStrings");
    }

    @Test
    public void test_GetFluentSetterMethodName_NoEum_WithMap() {
        when(serviceModel.getShapes()).thenReturn(mockShapeMap);
        when(mockShape.getEnumValues()).thenReturn(new ArrayList<>());

    }

    @Test
    public void nonSharedModel_packageName() {
        String serviceName = "foo";
        DefaultNamingStrategy strategy = new DefaultNamingStrategy(serviceModel, CustomizationConfig.create());
        assertThat(strategy.getClientPackageName(serviceName)).isEqualTo("foo");
        assertThat(strategy.getPaginatorsPackageName(serviceName)).isEqualTo("foo.paginators");
        assertThat(strategy.getSmokeTestPackageName(serviceName)).isEqualTo("foo.smoketests");
        assertThat(strategy.getModelPackageName(serviceName)).isEqualTo("foo.model");
        assertThat(strategy.getRequestTransformPackageName(serviceName)).isEqualTo("foo.transform");
        assertThat(strategy.getTransformPackageName(serviceName)).isEqualTo("foo.transform");
    }

    @Test
    public void sharedModel_notProvidingPackageName_shouldUseServiceName() {
        CustomizationConfig config = CustomizationConfig.create();
        ShareModelConfig shareModelConfig = new ShareModelConfig();
        shareModelConfig.setShareModelWith("foo");
        config.setShareModelConfig(shareModelConfig);
        String serviceName = "bar";

        DefaultNamingStrategy customizedModel = new DefaultNamingStrategy(serviceModel, config);

        assertThat(customizedModel.getClientPackageName(serviceName)).isEqualTo("foo.bar");
        assertThat(customizedModel.getPaginatorsPackageName(serviceName)).isEqualTo("foo.bar.paginators");
        assertThat(customizedModel.getSmokeTestPackageName(serviceName)).isEqualTo("foo.bar.smoketests");
        assertThat(customizedModel.getRequestTransformPackageName(serviceName)).isEqualTo("foo.bar.transform");

        // should share the same model and non-request transform packages
        assertThat(customizedModel.getModelPackageName(serviceName)).isEqualTo("foo.model");
        assertThat(customizedModel.getTransformPackageName(serviceName)).isEqualTo("foo.transform");
    }


    @Test
    public void sharedModel_providingPackageName_shouldUseProvidedPacakgeName() {
        CustomizationConfig config = CustomizationConfig.create();
        ShareModelConfig shareModelConfig = new ShareModelConfig();
        shareModelConfig.setShareModelWith("foo");
        shareModelConfig.setPackageName("b");
        config.setShareModelConfig(shareModelConfig);
        String serviceName = "bar";

        DefaultNamingStrategy customizedModel = new DefaultNamingStrategy(serviceModel, config);

        assertThat(customizedModel.getClientPackageName(serviceName)).isEqualTo("foo.b");
        assertThat(customizedModel.getPaginatorsPackageName(serviceName)).isEqualTo("foo.b.paginators");
        assertThat(customizedModel.getSmokeTestPackageName(serviceName)).isEqualTo("foo.b.smoketests");
        assertThat(customizedModel.getRequestTransformPackageName(serviceName)).isEqualTo("foo.b.transform");

        // should share the same model and non-request transform packages
        assertThat(customizedModel.getModelPackageName(serviceName)).isEqualTo("foo.model");
        assertThat(customizedModel.getTransformPackageName(serviceName)).isEqualTo("foo.transform");
    }

    @Test
    public void modelNameShouldHavePascalCase() {
        assertThat(strat.getRequestClassName("CAPSTest")).isEqualTo("CapsTestRequest");
        assertThat(strat.getExceptionName("CAPSTest")).isEqualTo("CapsTestException");
        assertThat(strat.getResponseClassName("CAPSTest")).isEqualTo("CapsTestResponse");
        assertThat(strat.getResponseClassName("CAPSByIndex")).isEqualTo("CapsByIndexResponse");

        assertThat(strat.getRequestClassName("FollowedByS3")).isEqualTo("FollowedByS3Request");
    }

    private void validateConversion(String input, String expectedOutput) {
        assertThat(strat.getEnumValueName(input)).isEqualTo(expectedOutput);
    }
}
