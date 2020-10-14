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
package software.amazon.awssdk.services.s3control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.s3control.model.DeletePublicAccessBlockRequest;
import software.amazon.awssdk.services.s3control.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3control.model.NoSuchPublicAccessBlockConfigurationException;
import software.amazon.awssdk.services.s3control.model.PutPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class S3ControlIntegrationTest extends AwsIntegrationTestBase {

    private String accountId;

    private static final String INVALID_ACCOUNT_ID = "1";

    private S3ControlClient client;

    @Before
    public void setup() {
        StsClient sts = StsClient.create();
        accountId = sts.getCallerIdentity().account();
        client = S3ControlClient.builder()
                                .overrideConfiguration(o -> o.addExecutionInterceptor(new AssertPayloadIsSignedExecutionInterceptor()))
                                .build();
    }

    @After
    public void tearDown() {
        try {
            client.deletePublicAccessBlock(DeletePublicAccessBlockRequest.builder().accountId(accountId).build());
        } catch (Exception ignore) {

        }
    }

    @Test
    public void putGetAndDeletePublicAccessBlock_ValidAccount() throws InterruptedException {
        PutPublicAccessBlockResponse result =
            client.putPublicAccessBlock(r -> r.accountId(accountId)
                                              .publicAccessBlockConfiguration(r2 -> r2.blockPublicAcls(true)
                                                                                      .ignorePublicAcls(true)));
        assertNotNull(result);

        // Wait a bit for the put to take affect
        Thread.sleep(5000);

        GetPublicAccessBlockResponse config = client.getPublicAccessBlock(r -> r.accountId(accountId));
        assertTrue(config.publicAccessBlockConfiguration().blockPublicAcls());
        assertTrue(config.publicAccessBlockConfiguration().ignorePublicAcls());

        assertNotNull(client.deletePublicAccessBlock(r -> r.accountId(accountId)));
    }

    @Test
    public void putPublicAccessBlock_NoSuchAccount() {
        try {
            assertNotNull(client.putPublicAccessBlock(r -> r.accountId(INVALID_ACCOUNT_ID)
                                                            .publicAccessBlockConfiguration(r2 -> r2.restrictPublicBuckets(true))));
            fail("Expected exception");
        } catch (S3ControlException e) {
            assertEquals("AccessDenied", e.awsErrorDetails().errorCode());
            assertNotNull(e.requestId());
        }
    }

    @Test
    public void getPublicAccessBlock_NoSuchAccount() {
        try {
            client.getPublicAccessBlock(r -> r.accountId(INVALID_ACCOUNT_ID));
            fail("Expected exception");
        } catch (S3ControlException e) {
            assertEquals("AccessDenied", e.awsErrorDetails().errorCode());
            assertNotNull(e.requestId());
        }
    }

    @Test
    public void deletePublicAccessBlock_NoSuchAccount() {
        try {
            client.deletePublicAccessBlock(r -> r.accountId(INVALID_ACCOUNT_ID));
            fail("Expected exception");
        } catch (S3ControlException e) {
            assertEquals("AccessDenied", e.awsErrorDetails().errorCode());
            assertNotNull(e.requestId());
        }
    }

    /**
     * Request handler to assert that payload signing is enabled.
     */
    private static final class AssertPayloadIsSignedExecutionInterceptor implements ExecutionInterceptor {
        @Override
        public void afterTransmission(Context.AfterTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpFullRequest request = (SdkHttpFullRequest) context.httpRequest();
            assertThat(context.httpRequest().headers().get("x-amz-content-sha256").get(0)).doesNotContain("UNSIGNED-PAYLOAD");
        }
    }

}