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

package software.amazon.awssdk.protocol.runners;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import org.junit.Assert;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.SdkPlugin;
import software.amazon.awssdk.core.SdkServiceClientConfiguration;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.protocol.model.TestCase;
import software.amazon.awssdk.protocol.reflect.ClientReflector;
import software.amazon.awssdk.protocol.reflect.ShapeModelReflector;
import software.amazon.awssdk.protocol.wiremock.WireMockUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Test runner for test cases exercising the client marshallers.
 */
class MarshallingTestRunner {

    private final IntermediateModel model;
    private final ClientReflector clientReflector;
    private final RequestRecordingInterceptor recordingInterceptor;

    MarshallingTestRunner(IntermediateModel model, ClientReflector clientReflector) {
        this.model = model;
        this.clientReflector = clientReflector;
        this.recordingInterceptor = new RequestRecordingInterceptor();
    }

    /**
     * @return LoggedRequest that wire mock captured.
     */
    private static LoggedRequest getLoggedRequest() {
        List<LoggedRequest> requests = WireMockUtils.findAllLoggedRequests();
        assertEquals(1, requests.size());
        return requests.get(0);
    }

    void runTest(TestCase testCase) throws Exception {
        ShapeModelReflector shapeModelReflector = createShapeModelReflector(testCase);
        AwsRequest request = createRequest(testCase, shapeModelReflector);

        try {
            if (!model.getShapes().get(testCase.getWhen().getOperationName() + "Request").isHasStreamingMember()) {
                clientReflector.invokeMethod(testCase, request);
            } else {
                clientReflector.invokeMethod(testCase,
                                             request,
                                             RequestBody.fromString(shapeModelReflector.getStreamingMemberValue()));
            }
            Assert.fail("Expected SDK client to intercept and record request before transmission.");
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof StopExecutionException) {
                SdkHttpRequest recordedRequest = recordingInterceptor.getRequest();
                testCase.getThen().getMarshallingAssertion().assertMatches(getLoggedRequest());
            } else {
                throw e;
            }
        }
    }

    private AwsRequest createRequest(TestCase testCase, ShapeModelReflector shapeModelReflector) {
        return ((AwsRequest) shapeModelReflector.createShapeObject())
                .toBuilder()
                .overrideConfiguration(requestConfiguration -> requestConfiguration
                    .addPlugin(config -> {
                        config.overrideConfiguration(c -> c.addExecutionInterceptor(recordingInterceptor));

                        if (StringUtils.isNotBlank(testCase.getGiven().getHost())) {
                            config.endpointOverride(URI.create("https://" + testCase.getGiven().getHost()));
                        }
                    }))
                .build();
    }

    private ShapeModelReflector createShapeModelReflector(TestCase testCase) {
        String operationName = testCase.getWhen().getOperationName();
        String requestClassName = getOperationRequestClassName(operationName);
        JsonNode input = testCase.getGiven().getInput();
        return new ShapeModelReflector(model, requestClassName, input);
    }

    /**
     * @return Name of the request class that corresponds to the given operation.
     */
    private String getOperationRequestClassName(String operationName) {
        return operationName + "Request";
    }

    private static final class RequestRecordingInterceptor implements ExecutionInterceptor {
        private SdkHttpRequest request;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            request = context.httpRequest();

            // Log or record the request here
            System.out.println("Recording Request:");
            System.out.println("HTTP Method: " + request.method());
            System.out.println("Endpoint: " + request.getUri());
            System.out.println("Headers: " + request.headers());

            throw new StopExecutionException();
        }

        public SdkHttpRequest getRequest() {
            return request;
        }
    }

    private static class StopExecutionException extends RuntimeException {}
}
