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

package software.amazon.awssdk.services.bedrockagentruntime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentruntime.model.BedrockAgentRuntimeException;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentRequest;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponse;
import software.amazon.awssdk.services.bedrockagentruntime.model.InvokeAgentResponseHandler;
import software.amazon.awssdk.services.bedrockagentruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockagentruntime.model.ResponseStream;
import software.amazon.awssdk.utils.Logger;

public class ServiceIntegrationTest extends IntegrationTestBase {
    private static final Logger log = Logger.loggerFor(ServiceIntegrationTest.class);
    
    private static String restApiId = null;

    @BeforeClass
    public static void createRestApi() {

    }

    @AfterClass
    public static void deleteRestApiKey() {

    }

    @Test
    public void whenStreamContainsExceptionItIsMarshalledToServiceException() {
        InvokeAgentRequest request = InvokeAgentRequest.builder().agentId("test").agentAliasId("test").sessionId("test").inputText("test").build();
        TestHandler asyncResponseHandler = new TestHandler();
      //  assertThatThrownBy(() -> client.invokeAgent(request, asyncResponseHandler).join()).hasCauseInstanceOf
        //  (BedrockAgentRuntimeException.class).hasCauseInstanceOf(ResourceNotFoundException.class);
        bedrock.invokeAgent(request, asyncResponseHandler).join();
    }

    private static class TestHandler implements InvokeAgentResponseHandler {
        private InvokeAgentResponse response;
        private List<ResponseStream> receivedEvents = new ArrayList<>();
        private Throwable exception;

        @Override
        public void responseReceived(InvokeAgentResponse response) {
            this.response = response;
        }

        @Override
        public void onEventStream(SdkPublisher<ResponseStream> publisher) {
            publisher.subscribe(receivedEvents::add);
        }

        @Override
        public void exceptionOccurred(Throwable throwable) {
            exception = throwable;
        }

        @Override
        public void complete() {
        }
    }

}
