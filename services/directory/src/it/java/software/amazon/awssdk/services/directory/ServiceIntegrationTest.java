/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

    /**
     * Tests that an exception with a member in it is serialized properly. See TT0064111680
     */
    @Test
    public void describeDirectories_InvalidNextToken_ThrowsExceptionWithRequestIdPresent() {
        try {
            dsClient.describeDirectories(DescribeDirectoriesRequest.builder().nextToken("invalid").build());
        } catch (InvalidNextTokenException e) {
            assertNotNull(e.requestId());
        }
    }
}
