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

package software.amazon.awssdk.services.bedrockagentcore.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.services.bedrockagentcore.model.CreateEventRequest;

public class BedrockAgentCoreArnInterceptorTest {

    private final BedrockAgentCoreArnInterceptor interceptor = new BedrockAgentCoreArnInterceptor();

    @Test
    public void modifyRequest_withFullArn_extractsShortMemoryId() {
        CreateEventRequest request = CreateEventRequest.builder()
                .memoryId("arn:aws:bedrock-agentcore:us-west-2:111122223333:memory/my-memory-store-AbCdEf")
                .build();

        Context.ModifyRequest context = () -> request;
        SdkRequest modified = interceptor.modifyRequest(context, new ExecutionAttributes());

        assertEquals(CreateEventRequest.class, modified.getClass());
        CreateEventRequest modifiedRequest = (CreateEventRequest) modified;
        assertEquals("my-memory-store-AbCdEf", modifiedRequest.memoryId());
    }

    @Test
    public void modifyRequest_withShortMemoryId_leavesUnchanged() {
        CreateEventRequest request = CreateEventRequest.builder()
                .memoryId("my-memory-store-AbCdEf")
                .build();

        Context.ModifyRequest context = () -> request;
        SdkRequest modified = interceptor.modifyRequest(context, new ExecutionAttributes());

        assertSame(request, modified);
    }

    @Test
    public void modifyRequest_withNullMemoryId_leavesUnchanged() {
        CreateEventRequest request = CreateEventRequest.builder().build();

        Context.ModifyRequest context = () -> request;
        SdkRequest modified = interceptor.modifyRequest(context, new ExecutionAttributes());

        assertSame(request, modified);
    }

    @Test
    public void modifyRequest_withMalformedArn_leavesUnchanged() {
        CreateEventRequest request = CreateEventRequest.builder()
                .memoryId("arn:aws:bedrock-agentcore:us-west-2")
                .build();

        Context.ModifyRequest context = () -> request;
        SdkRequest modified = interceptor.modifyRequest(context, new ExecutionAttributes());

        assertSame(request, modified);
    }
}
