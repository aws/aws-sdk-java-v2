/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package software.amazon.awssdk.codegen.naming;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DefaultNamingStrategyTest {

    private DefaultNamingStrategy strat = new DefaultNamingStrategy(null, null);

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

    private void validateConversion(String input, String expectedOutput) {
        assertThat(strat.getEnumValueName(input)).isEqualTo(expectedOutput);
    }
}
