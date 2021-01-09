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

package software.amazon.awssdk.services;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingInputOperationRequest;

/**
 * Test to ensure that operations that use the {@link software.amazon.awssdk.auth.signer.AsyncAws4Signer} don't apply
 * the override when the signer is overridden by the customer.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncSignerOverrideTest {
    @Mock
    public Signer mockSigner;

    @Test
    public void test_signerOverriddenForStreamingInput_takesPrecedence() {
        ProtocolRestJsonAsyncClient asyncClient = ProtocolRestJsonAsyncClient.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                .region(Region.US_WEST_2)
                .overrideConfiguration(o -> o.putAdvancedOption(SIGNER, mockSigner))
                .build();

        try {
            asyncClient.streamingInputOperation(StreamingInputOperationRequest.builder().build(),
                    AsyncRequestBody.fromString("test")).join();
        } catch (Exception expected) {
        }

        verify(mockSigner).sign(any(SdkHttpFullRequest.class), any(ExecutionAttributes.class));
    }
}
