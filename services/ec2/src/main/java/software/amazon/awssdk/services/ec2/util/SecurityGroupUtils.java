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

package software.amazon.awssdk.services.ec2.util;

import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.AmazonServiceException;
import software.amazon.awssdk.services.ec2.EC2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;

public class SecurityGroupUtils {

    private static final String INVALID_GROUP_NOT_FOUND = "InvalidGroup.NotFound";

    /**
     * Provides a quick answer to whether a security group exists.
     *
     * @param ec2
     *            the EC2 client to use for making service requests
     * @param securityGroupName
     *            the name of the security group being queried
     * @throws AmazonClientException
     *             If any internal errors are encountered inside the client
     *             while attempting to make the request or handle the response.
     *             For example if a network connection is not available.
     * @throws AmazonServiceException
     *             If an error response is returned by AmazonEC2 indicating
     *             either a problem with the data in the request, or a server
     *             side issue.
     */
    public static boolean doesSecurityGroupExist(EC2Client ec2, String securityGroupName) throws AmazonClientException {

        DescribeSecurityGroupsRequest securityGroupsRequest = DescribeSecurityGroupsRequest.builder()
                .groupNames(securityGroupName)
                .build();

        try {
            ec2.describeSecurityGroups(securityGroupsRequest);
            return true;

        } catch (AmazonServiceException ase) {
            if (INVALID_GROUP_NOT_FOUND.equals(ase.getErrorCode())) {
                return false;
            }
            throw ase;
        }
    }
}
