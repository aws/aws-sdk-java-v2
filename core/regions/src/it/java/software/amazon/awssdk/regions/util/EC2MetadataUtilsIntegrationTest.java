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

package software.amazon.awssdk.regions.util;


import java.io.IOException;
import java.util.Map;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtils;
import software.amazon.awssdk.regions.internal.util.EC2MetadataUtilsServer;

public class EC2MetadataUtilsIntegrationTest {

    private static EC2MetadataUtilsServer SERVER = null;

    @BeforeClass
    public static void setUp() throws IOException {
        SERVER = new EC2MetadataUtilsServer( 0);
        SERVER.start();

        System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                           "http://localhost:" + SERVER.getLocalPort());
    }

    @AfterClass
    public static void cleanUp() throws IOException {
        if (SERVER != null) {
            SERVER.stop();
        }

        System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.property());
    }

    @Test(expected = SdkClientException.class)
    public void ec2MetadataDisabled_shouldThrowException() {
        try {
            System.setProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property(), "true");
            EC2MetadataUtils.getInstanceId();
        } finally {
            System.clearProperty(SdkSystemSetting.AWS_EC2_METADATA_DISABLED.property());
        }
    }

    @Test
    public void testInstanceSignature() {
        String signature = EC2MetadataUtils.getInstanceSignature();
        Assert.assertEquals("foobar", signature);
    }
}
