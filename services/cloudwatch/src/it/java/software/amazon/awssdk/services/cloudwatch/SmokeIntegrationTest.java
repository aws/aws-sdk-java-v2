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

package software.amazon.awssdk.services.cloudwatch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.DashboardNotFoundErrorException;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class SmokeIntegrationTest extends AwsIntegrationTestBase {
    private static CloudWatchClient cloudwatch;

    @BeforeClass
    public static void setUp() throws IOException {
        cloudwatch = CloudWatchClient.builder()
                                     .credentialsProvider(getCredentialsProvider())
                                     .region(Region.US_WEST_2)
                                     .build();
    }

    @AfterClass
    public static void teardown() {
        cloudwatch.close();
    }

    @Test
    public void test_AmbiguousErrorResolution() {
        assertThatThrownBy(() -> {
            cloudwatch.getDashboard(r -> r.dashboardName("foo"));
        }).isInstanceOf(DashboardNotFoundErrorException.class)
            .matches(e -> {
                DashboardNotFoundErrorException dnf = (DashboardNotFoundErrorException) e;
                return "ResourceNotFound".equals(dnf.awsErrorDetails().errorCode());
            });
    }
}
