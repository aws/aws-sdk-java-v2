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

package software.amazon.awssdk.services.directory;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import software.amazon.awssdk.services.directory.model.CreateDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DeleteDirectoryRequest;
import software.amazon.awssdk.services.directory.model.DescribeDirectoriesRequest;
import software.amazon.awssdk.services.directory.model.DirectorySize;
import software.amazon.awssdk.services.directory.model.DirectoryVpcSettings;
import software.amazon.awssdk.services.directory.model.InvalidNextTokenException;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Vpc;

public class ServiceIntegrationTest extends IntegrationTestBase {

    private static final String US_EAST_1A = "us-east-1a";
    private static final String US_EAST_1B = "us-east-1b";

    @Test
    public void testDirectories() {

        String vpcId = getVpcId();
        // Creating a directory requires at least two subnets located in
        // different availability zones
        String subnetId_0 = getSubnetIdInVpc(vpcId, US_EAST_1A);
        String subnetId_1 = getSubnetIdInVpc(vpcId, US_EAST_1B);

        String dsId = dsClient
                .createDirectory(CreateDirectoryRequest.builder().description("This is my directory!")
                                                             .name("AWS.Java.SDK.Directory").shortName("md").password("My.Awesome.Password.2015")
                                                             .size(DirectorySize.Small).vpcSettings(
                                DirectoryVpcSettings.builder().vpcId(vpcId).subnetIds(subnetId_0, subnetId_1).build()).build())
                .directoryId();

        dsClient.deleteDirectory(DeleteDirectoryRequest.builder().directoryId(dsId).build());
    }

    private String getVpcId() {
        List<Vpc> vpcs = ec2Client.describeVpcs(DescribeVpcsRequest.builder().build()).vpcs();
        if (vpcs.isEmpty()) {
            Assert.fail("No VPC found in this account.");
        }
        return vpcs.get(0).vpcId();
    }

    private String getSubnetIdInVpc(String vpcId, String az) {
        List<Subnet> subnets = ec2Client.describeSubnets(DescribeSubnetsRequest.builder()
                .filters(
                        Filter.builder()
                                .name("vpc-id")
                                .values(vpcId)
                                .build(),
                        Filter.builder()
                                .name("availabilityZone")
                                .values(az)
                                .build())
                .build())
                .subnets();
        if (subnets.isEmpty()) {
            Assert.fail("No Subnet found in VPC " + vpcId + " AvailabilityZone: " + az);
        }
        return subnets.get(0).subnetId();
    }

    /**
     * Tests that an exception with a member in it is serialized properly. See TT0064111680
     */
    @Test
    public void describeDirectories_InvalidNextToken_ThrowsExceptionWithRequestIdPresent() {
        try {
            dsClient.describeDirectories(DescribeDirectoriesRequest.builder().nextToken("invalid").build());
        } catch (InvalidNextTokenException e) {
            assertNotNull(e.getRequestId());
        }
    }
}
