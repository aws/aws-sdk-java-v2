/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.storagegateway;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.AmazonServiceException;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.storagegateway.model.DeleteGatewayRequest;
import software.amazon.awssdk.services.storagegateway.model.InvalidGatewayRequestException;
import software.amazon.awssdk.services.storagegateway.model.ListGatewaysRequest;
import software.amazon.awssdk.services.storagegateway.model.ListGatewaysResponse;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Tests service methods in storage gateway. Because of the non-trivial amount of set-up required,
 * this is more of a spot check than an exhaustive test.
 */
public class ServiceIntegrationTest extends AwsTestBase {

    private static StorageGatewayClient sg;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpCredentials();
        sg = StorageGatewayClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_EAST_1).build();
    }

    @Test
    public void testListGateways() {
        ListGatewaysResponse listGateways = sg.listGateways(ListGatewaysRequest.builder().build());
        assertNotNull(listGateways);
        assertThat(listGateways.gateways().size(), greaterThanOrEqualTo(0));
    }

    @Test(expected = InvalidGatewayRequestException.class)
    public void deleteGateway_InvalidArn_ThrowsException() {
        sg.deleteGateway(DeleteGatewayRequest.builder().gatewayARN("arn:aws:storagegateway:us-west-2:111122223333:gateway/sgw-12A3456B/volume/vol-1122AABBCCDDEEFFG").build());
    }

    @Test(expected = AmazonServiceException.class)
    public void deleteGateway_NullArn_ThrowsAmazonServiceException() {
        sg.deleteGateway(DeleteGatewayRequest.builder().build());

    }

}
