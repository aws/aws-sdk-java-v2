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

package software.amazon.awssdk.regions.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import software.amazon.awssdk.AwsSystemSetting;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.util.EC2MetadataUtilsServer;

/**
 * Tests broken up by fixture.
 */
@RunWith(Enclosed.class)
public class InstanceProfileRegionProviderTest {

    /**
     * If the EC2 metadata service is running it should return the region the server is mocked
     * with.
     */
    public static class MetadataServiceRunningTest {

        private static EC2MetadataUtilsServer server;

        private AwsRegionProvider regionProvider;

        @BeforeClass
        public static void setupFixture() throws IOException {
            server = new EC2MetadataUtilsServer("localhost", 0);
            server.start();

            System.setProperty(AwsSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                               "http://localhost:" + server.getLocalPort());
        }

        @AfterClass
        public static void tearDownFixture() throws IOException {
            server.stop();
            System.clearProperty(AwsSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        }

        @Before
        public void setup() {
            regionProvider = new InstanceProfileRegionProvider();
        }

        @Test
        public void metadataServiceRunning_ProvidesCorrectRegion() {
            assertEquals(Region.US_EAST_1, regionProvider.getRegion());
        }

    }

    /**
     * If the EC2 metadata service is not present then the provider should just return null instead
     * of failing. This is to allow the provider to be used in a chain context where another
     * provider further down the chain may be able to provide the region.
     */
    public static class MetadataServiceNotRunning {

        private AwsRegionProvider regionProvider;

        @Before
        public void setup() {
            System.setProperty(AwsSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:54123");
            regionProvider = new InstanceProfileRegionProvider();
        }

        @Test
        public void metadataServiceNotRunning_ProvidesCorrectRegion() {
            assertNull(regionProvider.getRegion());
        }

    }


}
