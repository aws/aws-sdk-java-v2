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

package utils.http;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.net.URI;
import org.junit.Rule;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * Base class for tests that use a WireMock server
 */
public abstract class WireMockTestBase {

    @Rule
    public WireMockRule mockServer = new WireMockRule(0);

    protected SdkHttpFullRequest.Builder newGetRequest(String resourcePath) {
        return newRequest(resourcePath)
            .method(SdkHttpMethod.GET);
    }

    protected SdkHttpFullRequest.Builder newRequest(String resourcePath) {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("http://localhost"))
                                 .port(mockServer.port())
                                 .encodedPath(resourcePath);
    }

    protected HttpResponseHandler<SdkServiceException> stubErrorHandler() throws Exception {
        HttpResponseHandler<SdkServiceException> errorHandler = mock(HttpResponseHandler.class);
        when(errorHandler.handle(any(SdkHttpFullResponse.class), any(ExecutionAttributes.class))).thenReturn(mockException());
        return errorHandler;
    }

    private SdkServiceException mockException() {
        SdkServiceException exception = SdkServiceException.builder().message("Dummy error response").statusCode(500).build();
        return exception;
    }
}
