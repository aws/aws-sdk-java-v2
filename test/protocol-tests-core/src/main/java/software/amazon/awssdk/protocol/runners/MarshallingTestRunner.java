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


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import org.junit.Assert;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
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
    private final LocalhostOnlyForWiremockInterceptor localhostOnlyForWiremockInterceptor;

    MarshallingTestRunner(IntermediateModel model, ClientReflector clientReflector) {
        this.model = model;
        this.clientReflector = clientReflector;
        this.localhostOnlyForWiremockInterceptor = new LocalhostOnlyForWiremockInterceptor();
    }

    void runTest(TestCase testCase) throws Exception {
        resetWireMock();
        ShapeModelReflector shapeModelReflector = createShapeModelReflector(testCase);
        AwsRequest request = createRequest(testCase, shapeModelReflector);

        if (!model.getShapes().get(testCase.getWhen().getOperationName() + "Request").isHasStreamingMember()) {
            clientReflector.invokeMethod(testCase, request);
        } else {
            clientReflector.invokeMethod(testCase,
                                         request,
                                         RequestBody.fromString(shapeModelReflector.getStreamingMemberValue()));
        }
        testCase.getThen().getMarshallingAssertion()
                .assertMatches(localhostOnlyForWiremockInterceptor.getLoggedRequestWithOriginalHost());
    }

    /**
     * Reset wire mock and re-configure stubbing.
     */
    private void resetWireMock() {
        WireMock.reset();
        // Stub to return 200 for all requests
        ResponseDefinitionBuilder responseDefBuilder = aResponse().withStatus(200);
        // XML Unmarshallers expect at least one level in the XML document.
        if (model.getMetadata().isXmlProtocol()) {
            responseDefBuilder.withBody("<foo></foo>");
        }
        stubFor(any(urlMatching(".*")).willReturn(responseDefBuilder));
    }

    private AwsRequest createRequest(TestCase testCase, ShapeModelReflector shapeModelReflector) {
        return ((AwsRequest) shapeModelReflector.createShapeObject())
                .toBuilder()
                .overrideConfiguration(requestConfiguration -> requestConfiguration
                    .addPlugin(config -> {
                        config.overrideConfiguration(c -> c.addExecutionInterceptor(localhostOnlyForWiremockInterceptor));

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

    /**
     * Wiremock requires that requests use "localhost" as the host - any prefixes such as "foo.localhost" will
     * result in a DNS lookup that will fail.  This interceptor modifies the request to ensure this and captures
     * the original host.
     */
    private static final class LocalhostOnlyForWiremockInterceptor implements ExecutionInterceptor {
        private String originalHost;
        private String originalProtocol;
        private int originalPort;

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            originalHost = context.httpRequest().host();
            originalProtocol = context.httpRequest().protocol();
            originalPort = context.httpRequest().port();

            return context.httpRequest().toBuilder()
                          .host("localhost")
                          .port(WireMockUtils.port())
                          .protocol("http")
                          .build();
        }

        /**
         * @return LoggedRequest that wire mock captured modified with the original host captured by this
         * interceptor.
         */
        public LoggedRequest getLoggedRequestWithOriginalHost() {
            List<LoggedRequest> requests = WireMockUtils.findAllLoggedRequests();
            assertEquals(1, requests.size());
            LoggedRequest loggedRequest = requests.get(0);
            return new LoggedRequest(
                loggedRequest.getUrl(),
                originalProtocol + "://" + originalHost + ":" + originalPort,
                loggedRequest.getMethod(),
                loggedRequest.getClientIp(),
                loggedRequest.getHeaders(),
                loggedRequest.getCookies(),
                loggedRequest.isBrowserProxyRequest(),
                loggedRequest.getLoggedDate(),
                loggedRequest.getBody(),
                loggedRequest.getParts()
            );
        }
    }
}
