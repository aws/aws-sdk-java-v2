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

package software.amazon.awssdk.http.urlconnection.internal;

import static software.amazon.awssdk.http.Header.CONTENT_LENGTH;
import static software.amazon.awssdk.http.HttpStatusFamily.CLIENT_ERROR;
import static software.amazon.awssdk.http.HttpStatusFamily.SERVER_ERROR;
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.HttpStatusFamily;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.http.urlconnection.UrlConnectionFactory;
import software.amazon.awssdk.http.urlconnection.internal.connection.DefaultHttpConnection;
import software.amazon.awssdk.http.urlconnection.internal.connection.Expect100BugHttpConnection;
import software.amazon.awssdk.http.urlconnection.internal.connection.HttpConnection;
import software.amazon.awssdk.http.urlconnection.internal.connection.ResponseCodeBugHttpConnection;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Executes a {@link HttpExecuteRequest} against a {@link HttpURLConnection}
 */
@SdkInternalApi
public class UrlConnectionExecutableHttpRequest implements ExecutableHttpRequest {
    private final HttpConnection connection;
    private final HttpExecuteRequest request;

    public UrlConnectionExecutableHttpRequest(UrlConnectionFactory connectionFactory,
                                              HttpExecuteRequest executeRequest) {
        this.request = executeRequest;

        HttpURLConnection httpUrlConnection = connectionFactory.createConnection(executeRequest.httpRequest().getUri());
        initializeConnection(httpUrlConnection);

        HttpConnection connection = new DefaultHttpConnection(httpUrlConnection);
        connection = new ResponseCodeBugHttpConnection(connection);
        connection = new Expect100BugHttpConnection(connection, executeRequest.httpRequest());
        this.connection = connection;
    }

    private void initializeConnection(HttpURLConnection connection) {
        // Disable following redirects since it breaks SDK error handling and matches Apache.
        // See: https://github.com/aws/aws-sdk-java-v2/issues/975
        connection.setInstanceFollowRedirects(false);

        request.httpRequest()
               .headers()
               .forEach((key, values) -> values.forEach(value -> connection.setRequestProperty(key, value)));

        invokeSafely(() -> connection.setRequestMethod(request.httpRequest().method().name()));

        if (request.contentStreamProvider().isPresent()) {
            connection.setDoOutput(true);
        }

        request.httpRequest().firstMatchingHeader(CONTENT_LENGTH).map(Long::parseLong)
               .ifPresent(connection::setFixedLengthStreamingMode);
    }

    @Override
    public HttpExecuteResponse call() throws IOException {
        try {
            return doCall();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    private HttpExecuteResponse doCall() throws IOException {
        connection.connect();

        Optional<ContentStreamProvider> requestContent = request.contentStreamProvider();

        if (requestContent.isPresent()) {
            OutputStream outputStream = connection.getRequestStream();
            if (outputStream != null) {
                IoUtils.copy(requestContent.get().newStream(), outputStream);
            }
        }

        int responseCode = connection.getResponseCode();
        boolean isErrorResponse = HttpStatusFamily.of(responseCode).isOneOf(CLIENT_ERROR, SERVER_ERROR);
        InputStream responseContent = isErrorResponse ? connection.getResponseErrorStream()
                                                      : connection.getResponseStream();
        AbortableInputStream responseBody = responseContent != null ? AbortableInputStream.create(responseContent)
                                                                    : null;

        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder()
                                                           .statusCode(responseCode)
                                                           .statusText(connection.getResponseMessage())
                                                           .headers(responseHeaders())
                                                           .build())
                                  .responseBody(responseBody)
                                  .build();
    }

    private Map<String, List<String>> responseHeaders() {
        return connection.getResponseHeaders()
                         .entrySet().stream()
                         .filter(e -> e.getKey() != null)
                         .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void abort() {
        connection.disconnect();
    }
}
