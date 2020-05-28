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

package software.amazon.awssdk.modulepath.tests.mocktests;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;

/**
 * Base classes to mock sync and async api calls.
 */
public abstract class BaseMockApiCall {

    private static final Logger logger = LoggerFactory.getLogger(BaseMockApiCall.class);

    protected final MockHttpClient mockHttpClient;
    protected final MockAyncHttpClient mockAyncHttpClient;
    private final String protocol;

    public BaseMockApiCall(String protocol) {
        this.protocol = protocol;
        this.mockHttpClient = new MockHttpClient();
        this.mockAyncHttpClient = new MockAyncHttpClient();
    }

    public void successfulApiCall() {
        logger.info("stubing successful api call for {} protocol", protocol);
        mockHttpClient.stubNextResponse(successResponse(protocol));
        runnable().run();
        mockHttpClient.reset();
    }

    public void failedApiCall() {
        logger.info("stubing failed api call for {} protocol", protocol);
        mockHttpClient.stubNextResponse(errorResponse(protocol));

        try {
            runnable().run();
        } catch (AwsServiceException e) {
            logger.info("Received expected service exception", e.getMessage());
        }

        mockHttpClient.reset();
    }

    public void successfulAsyncApiCall() {
        logger.info("stubing successful async api call for {} protocol", protocol);
        mockAyncHttpClient.stubNextResponse(successResponse(protocol));
        asyncRunnable().run();
        mockAyncHttpClient.reset();
    }

    public void failedAsyncApiCall() {
        logger.info("stubing failed async api call for {} protocol", protocol);
        mockAyncHttpClient.stubNextResponse(errorResponse(protocol));

        try {
            asyncRunnable().run();
        } catch (CompletionException e) {
            if (e.getCause() instanceof AwsServiceException) {
                logger.info("expected service exception {}", e.getMessage());
            } else {
                throw new RuntimeException("Unexpected exception is thrown");
            }
        }

        mockAyncHttpClient.reset();
    }

    abstract Runnable runnable();

    abstract Runnable asyncRunnable();

    private HttpExecuteResponse successResponse(String protocol) {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(200)
                                                           .build())
                                  .responseBody(generateContent(protocol))
                                  .build();
    }

    private HttpExecuteResponse errorResponse(String protocol) {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(500)
                                                           .build())
                                  .responseBody(generateContent(protocol))
                                  .build();
    }

    private AbortableInputStream generateContent(String protocol) {
        String content;
        switch (protocol) {
            case "xml":
                content = "<foo></foo>";
                break;
            default:
            case "json":
                content = "{}";
        }

        return AbortableInputStream.create(new ByteArrayInputStream(content.getBytes()));
    }
}
