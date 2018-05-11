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

package software.amazon.awssdk.http.apache.async.internal;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.async.AbortableRunnable;
import software.amazon.awssdk.http.async.SdkHttpResponseHandler;

/**
 * Implementation of {@link AbortableRunnable} used by {@link ApacheAsyncHttpClient}.
 */
class AbortableRunnableImpl implements AbortableRunnable {
    private final InputStreamToPublisherAdapter inputStreamToPublisherAdapter = new InputStreamToPublisherAdapter();
    private final ExecutorService exec;
    private final HttpClient httpClient;
    private final HttpRequestBase apacheRequest;
    private final HttpClientContext clientContext;
    private final SdkHttpResponseHandler responseHandler;

    AbortableRunnableImpl(ExecutorService exec, HttpClient httpClient, HttpRequestBase apacheRequest,
                                 HttpClientContext clientContext, SdkHttpResponseHandler responseHandler) {
        this.exec = exec;
        this.httpClient = httpClient;
        this.apacheRequest = apacheRequest;
        this.clientContext = clientContext;
        this.responseHandler = responseHandler;
    }

    @Override
    public void run() {
        exec.submit(this::doRun);
    }

    private void doRun() {
        try {
            HttpResponse apacheResponse = httpClient.execute(apacheRequest, clientContext);
            responseHandler.headersReceived(createResponse(apacheResponse));
            responseHandler.onStream(inputStreamToPublisherAdapter.adapt(apacheResponse.getEntity().getContent()));
            responseHandler.complete();
        } catch (IOException e) {
            responseHandler.exceptionOccurred(e);
        }
    }

    @Override
    public void abort() {
        apacheRequest.abort();
    }

    private SdkHttpResponse createResponse(org.apache.http.HttpResponse apacheHttpResponse) {
        return SdkHttpFullResponse.builder()
                .statusCode(apacheHttpResponse.getStatusLine().getStatusCode())
                .statusText(apacheHttpResponse.getStatusLine().getReasonPhrase())
                .headers(transformHeaders(apacheHttpResponse))
                .build();
    }

    private Map<String, List<String>> transformHeaders(HttpResponse apacheHttpResponse) {
        return Stream.of(apacheHttpResponse.getAllHeaders())
                .collect(groupingBy(Header::getName, mapping(Header::getValue, toList())));
    }
}
