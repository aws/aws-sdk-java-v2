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

package software.amazon.awssdk.http.apache;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static software.amazon.awssdk.utils.Validate.notNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.AbortableCallable;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpConfigurationOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkRequestContext;
import software.amazon.awssdk.http.apache.internal.ApacheHttpRequestConfig;
import software.amazon.awssdk.http.apache.internal.impl.ApacheHttpRequestFactory;
import software.amazon.awssdk.http.apache.internal.impl.ConnectionManagerAwareHttpClient;
import software.amazon.awssdk.http.apache.internal.utils.ApacheUtils;
import software.amazon.awssdk.utils.AttributeMap;

@SdkInternalApi
class ApacheHttpClient implements SdkHttpClient {

    private final ApacheHttpRequestFactory apacheHttpRequestFactory = new ApacheHttpRequestFactory();
    private final ConnectionManagerAwareHttpClient httpClient;
    private final ApacheHttpRequestConfig requestConfig;
    private final AttributeMap resolvedOptions;

    ApacheHttpClient(ConnectionManagerAwareHttpClient httpClient,
                     ApacheHttpRequestConfig requestConfig,
                     AttributeMap resolvedOptions) {
        this.httpClient = notNull(httpClient, "httpClient must not be null.");
        this.requestConfig = notNull(requestConfig, "requestConfig must not be null.");
        this.resolvedOptions = notNull(resolvedOptions, "resolvedOptions must not be null");
    }

    @Override
    public AbortableCallable<SdkHttpFullResponse> prepareRequest(SdkHttpFullRequest request, SdkRequestContext context) {
        final HttpRequestBase apacheRequest = toApacheRequest(request);
        return new AbortableCallable<SdkHttpFullResponse>() {
            @Override
            public SdkHttpFullResponse call() throws Exception {
                return execute(apacheRequest);
            }

            @Override
            public void abort() {
                apacheRequest.abort();
            }
        };
    }

    @Override
    public <T> Optional<T> getConfigurationValue(SdkHttpConfigurationOption<T> key) {
        return Optional.ofNullable(resolvedOptions.get(key));
    }

    @Override
    public void close() {
        httpClient.getHttpClientConnectionManager().shutdown();
    }

    private SdkHttpFullResponse execute(HttpRequestBase apacheRequest) throws IOException {
        HttpClientContext localRequestContext = ApacheUtils.newClientContext(requestConfig.proxyConfiguration());
        HttpResponse httpResponse = httpClient.execute(apacheRequest, localRequestContext);
        return createResponse(httpResponse, apacheRequest);
    }

    private HttpRequestBase toApacheRequest(SdkHttpFullRequest request) {
        return apacheHttpRequestFactory.create(request, requestConfig);
    }

    /**
     * Creates and initializes an HttpResponse object suitable to be passed to an HTTP response
     * handler object.
     *
     * @return The new, initialized HttpResponse object ready to be passed to an HTTP response handler object.
     * @throws IOException If there were any problems getting any response information from the
     *                     HttpClient method object.
     */
    private SdkHttpFullResponse createResponse(org.apache.http.HttpResponse apacheHttpResponse,
                                               HttpRequestBase apacheRequest) throws IOException {
        return SdkHttpFullResponse.builder()
                                  .statusCode(apacheHttpResponse.getStatusLine().getStatusCode())
                                  .statusText(apacheHttpResponse.getStatusLine().getReasonPhrase())
                                  .content(apacheHttpResponse.getEntity() != null ?
                                                   toAbortableInputStream(apacheHttpResponse, apacheRequest) : null)
                                  .headers(transformHeaders(apacheHttpResponse))
                                  .build();

    }

    private AbortableInputStream toAbortableInputStream(HttpResponse apacheHttpResponse, HttpRequestBase apacheRequest)
            throws IOException {
        return new AbortableInputStream(apacheHttpResponse.getEntity().getContent(), apacheRequest::abort);
    }

    private Map<String, List<String>> transformHeaders(HttpResponse apacheHttpResponse) {
        return Stream.of(apacheHttpResponse.getAllHeaders())
                     .collect(groupingBy(Header::getName, mapping(Header::getValue, toList())));
    }
}
