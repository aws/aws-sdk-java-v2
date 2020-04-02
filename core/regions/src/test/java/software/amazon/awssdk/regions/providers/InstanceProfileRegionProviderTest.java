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

package software.amazon.awssdk.regions.providers;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtilsServer;

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
            server = new EC2MetadataUtilsServer(0);
            server.start();

            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                               "http://localhost:" + server.getLocalPort());
        }

        @AfterClass
        public static void tearDownFixture() throws IOException {
            server.stop();
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
        }

        @Before
        public void setup() {
            regionProvider = new InstanceProfileRegionProvider();
        }

        @Test
        public void metadataServiceRunning_ProvidesCorrectRegion() {
            assertEquals(Region.US_EAST_1, regionProvider.getRegion());
        }

        @Test(expected = SdkClientException.class)
        public void ec2MetadataDisabled_shouldReturnNull() {
            try {
                System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property(), "true");
                regionProvider.getRegion();
            } finally {
                System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property());
            }
        }
    }

    /**
     * If the EC2 metadata service is not present then the provider will throw an exception. If the provider is used
     * in a {@link AwsRegionProviderChain}, the chain will catch the exception and go on to the next region provider.
     */
    public static class MetadataServiceNotRunning {

        private AwsRegionProvider regionProvider;

        @Before
        public void setup() {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(), "http://localhost:54123");
            regionProvider = new InstanceProfileRegionProvider();
        }

        @Test (expected = SdkClientException.class)
        public void metadataServiceNotRunning_ThrowsException() {
            regionProvider.getRegion();
        }

    }
}
