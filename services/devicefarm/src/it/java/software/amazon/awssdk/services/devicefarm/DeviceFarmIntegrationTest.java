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

package software.amazon.awssdk.services.devicefarm;

import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.devicefarm.model.CreateProjectRequest;
import software.amazon.awssdk.services.devicefarm.model.CreateProjectResponse;
import software.amazon.awssdk.services.devicefarm.model.DeleteProjectRequest;
import software.amazon.awssdk.services.devicefarm.model.ListDevicePoolsRequest;
import software.amazon.awssdk.services.devicefarm.model.Project;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Smoke tests for device farm service.
 */
public class DeviceFarmIntegrationTest extends AwsTestBase {

    private static final String PROJECT_NAME = "df-java-project-"
                                               + System.currentTimeMillis();
    private static DeviceFarmClient client;

    private static String projectArn;

    @BeforeClass
    public static void setup() throws Exception {
        setUpCredentials();
        client = DeviceFarmClient.builder()
                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                 .region(Region.US_WEST_2)
                                 .build();
    }

    @AfterClass
    public static void teardown() {
        client.deleteProject(DeleteProjectRequest.builder().arn(projectArn).build());
    }

    @Test
    public void testCreateProject() {
        CreateProjectResponse result = client
                .createProject(CreateProjectRequest.builder()
                        .name(PROJECT_NAME)
                        .build());
        final Project project = result.project();
        assertNotNull(project);
        projectArn = project.arn();
        assertNotNull(projectArn);
    }

    @Test(expected = SdkServiceException.class)
    public void testExceptionHandling() {
        client.listDevicePools(ListDevicePoolsRequest.builder().nextToken("fake-token").build());
    }
}
