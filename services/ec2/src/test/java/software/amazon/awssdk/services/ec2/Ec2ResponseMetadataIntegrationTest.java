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

package software.amazon.awssdk.services.ec2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesResponse;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class Ec2ResponseMetadataIntegrationTest extends AwsIntegrationTestBase {

    @Test
    public void ec2Response_shouldHaveRequestId() {
        Ec2Client ec2Client = Ec2Client.builder()
                                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                       .build();

        DescribeAvailabilityZonesResponse response = ec2Client.describeAvailabilityZones();
        String requestId = response.responseMetadata().requestId();
        assertThat(requestId).isNotEqualTo("UNKNOWN");
    }
}
