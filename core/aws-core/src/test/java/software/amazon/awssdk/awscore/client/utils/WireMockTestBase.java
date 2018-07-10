/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.awscore.client.utils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Rule;
import software.amazon.awssdk.awscore.client.http.NoopTestAwsRequest;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.http.response.AwsJsonErrorResponseHandler;
import software.amazon.awssdk.core.DefaultRequest;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.http.HttpMethodName;
import software.amazon.awssdk.core.http.HttpResponse;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

/**
 * Base class for tests that use a WireMock server
 */
public abstract class WireMockTestBase {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    protected Request<?> newGetRequest(String resourcePath) {
        Request<?> request = newRequest(resourcePath);
        request.setHttpMethod(HttpMethodName.GET);
        return request;
    }

    protected Request<?> newRequest(String resourcePath) {
        Request<?> request = new DefaultRequest<NoopTestAwsRequest>("mock");
        request.setEndpoint(URI.create("http://localhost:" + mockServer.port() + resourcePath));
        return request;
    }

    protected HttpResponseHandler<AwsServiceException> stubErrorHandler() throws Exception {
        HttpResponseHandler<AwsServiceException> errorHandler = mock(AwsJsonErrorResponseHandler.class);
        when(errorHandler.handle(any(HttpResponse.class), any(ExecutionAttributes.class))).thenReturn(mockException());
        return errorHandler;
    }

    private AwsServiceException mockException() {
        AwsServiceException exception = new AwsServiceException("Dummy error response");
        exception.statusCode(500);
        return exception;
    }
}
