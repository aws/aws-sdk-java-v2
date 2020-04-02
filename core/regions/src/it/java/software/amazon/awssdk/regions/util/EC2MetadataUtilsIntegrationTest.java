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

    @Test
    public void testIamInfo() {
        EC2MetadataUtils.IamInfo info = EC2MetadataUtils
                .getIamInstanceProfileInfo();

        Assert.assertEquals("Success", info.code);
        Assert.assertNull(info.message);
        Assert.assertEquals("2014-04-07T08:18:41Z", info.lastUpdated);
        Assert.assertEquals("foobar", info.instanceProfileArn);
        Assert.assertEquals("moobily", info.instanceProfileId);
    }

    @Test
    public void testIamCredentials() {
        Map<String, EC2MetadataUtils.IamSecurityCredential> map = EC2MetadataUtils
                .getIamSecurityCredentials();

        Assert.assertEquals(2, map.size());

        for (Map.Entry<String, EC2MetadataUtils.IamSecurityCredential> entry : map
                .entrySet()) {

            Assert.assertNotNull(entry.getKey());
            Assert.assertNotNull(entry.getValue().code);
            Assert.assertNotNull(entry.getValue().lastUpdated);
            Assert.assertEquals("AWS-HMAC", entry.getValue().type);
            Assert.assertEquals("foobar", entry.getValue().accessKeyId);
            Assert.assertEquals("moobily", entry.getValue().secretAccessKey);
            Assert.assertEquals("beebop", entry.getValue().token);
            Assert.assertNotNull(entry.getValue().expiration);
        }
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
    public void testInstanceInfo() {
        EC2MetadataUtils.InstanceInfo info = EC2MetadataUtils.getInstanceInfo();
        Assert.assertEquals("2014-08-07T22:07:46Z", info.getPendingTime());
        Assert.assertEquals("m1.small", info.getInstanceType());
        Assert.assertEquals("ami-a49665cc", info.getImageId());
        Assert.assertEquals("i-6b2de041", info.getInstanceId());
        Assert.assertEquals("foo", info.getBillingProducts()[0]);
        Assert.assertEquals("x86_64", info.getArchitecture());
        Assert.assertEquals("599169622985", info.getAccountId());
        Assert.assertEquals("aki-919dcaf8", info.getKernelId());
        Assert.assertEquals("baz", info.getRamdiskId());
        Assert.assertEquals("us-east-1", info.getRegion());
        Assert.assertEquals("2010-08-31", info.getVersion());
        Assert.assertEquals("us-east-1b", info.getAvailabilityZone());
        Assert.assertEquals("10.201.215.38", info.getPrivateIp());
        Assert.assertEquals("bar", info.getDevpayProductCodes()[0]);
        Assert.assertEquals("qaz", info.getMarketplaceProductCodes()[0]);
    }

    @Test
    public void testInstanceSignature() {
        String signature = EC2MetadataUtils.getInstanceSignature();
        Assert.assertEquals("foobar", signature);
    }
}
