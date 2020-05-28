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

package software.amazon.awssdk.services.pinpoint;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.AwsTestBase;
import software.amazon.awssdk.utils.builder.SdkBuilder;

public class PinpointIntegTest extends AwsTestBase {

    protected static PinpointAsyncClient pinpointAsyncClient;

    @BeforeClass
    public static void setup() {
        pinpointAsyncClient = PinpointAsyncClient.builder()
                                                 .region(Region.US_WEST_2)
                                                 .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                                 .build();
    }

    @Test
    public void getApps() {
        assertThat(pinpointAsyncClient.getApps(SdkBuilder::build).join()).isNotNull();
    }
}
