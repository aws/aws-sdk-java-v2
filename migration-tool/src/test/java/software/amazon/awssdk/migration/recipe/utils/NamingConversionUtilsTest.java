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

package software.amazon.awssdk.migration.recipe.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class NamingConversionUtilsTest {
    @Test
    void v1Pojos_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.model.DescribeJobResult"))
            .isEqualTo("software.amazon.awssdk.services.iot.model.DescribeJobResponse");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.model.DescribeJobRequest"))
            .isEqualTo("software.amazon.awssdk.services.iot.model.DescribeJobRequest");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.model.AuditFinding"))
            .isEqualTo("software.amazon.awssdk.services.iot.model.AuditFinding");
    }

    @Test
    void v1SyncClients_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIot"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotClient"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AbstractAWSIot"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotClientBuilder"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotClientBuilder");
    }

    @Test
    void v1AsyncClients_shouldConvertToV2() {
        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotAsync"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotAsyncClient"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AbstractAWSIotAsync"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClient");

        assertThat(NamingConversionUtils.getV2Equivalent("com.amazonaws.services.iot.AWSIotAsyncClientBuilder"))
            .isEqualTo("software.amazon.awssdk.services.iot.IotAsyncClientBuilder");
    }
}
