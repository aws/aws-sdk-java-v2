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

package software.amazon.awssdk.services.s3.internal.customclientcontextparams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class S3CustomClientConfigParamsTest {

    @Test
    public void test_setCrossRegionAccessOnClient() {
        S3Client s3Client = S3Client.builder()
                                    .crossRegionAccessEnabled(true).build();

        assertTrue(s3Client.serviceClientConfiguration().crossRegionAccessEnabled());
    }

    @Test
    public void test_setCrossRegionAccess_through_clientLevelPlugin() {
        S3Client s3Client = S3Client.builder()
                                    .addPlugin(new TestCustomClientParamPlugin()).build();

        assertTrue(s3Client.serviceClientConfiguration().crossRegionAccessEnabled());
    }

    @Test
    public void test_setCrossRegionAccess_through_clientLevelPlugin_and_OnClient_verify_pluginOverride() {
        S3Client s3Client = S3Client.builder()
                                    .crossRegionAccessEnabled(false)
                                    .addPlugin(new TestCustomClientParamPlugin()).build();

        assertTrue(s3Client.serviceClientConfiguration().crossRegionAccessEnabled());
    }

    @Test
    public void setCrossRegionAccess_through_requestLevelPlugin_throwsIllegalStateException() {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .overrideConfiguration(o -> o.addPlugin(new TestCustomClientParamPlugin()))
                                                            .build();


        S3Client s3Client = S3Client.builder()
                                    .crossRegionAccessEnabled(false)
                                    .build();

        try {
            s3Client.getObject(getObjectRequest);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            assertEquals(e.getMessage(), "CROSS_REGION_ACCESS_ENABLED cannot be modified by request level plugins");
        }
    }
}
