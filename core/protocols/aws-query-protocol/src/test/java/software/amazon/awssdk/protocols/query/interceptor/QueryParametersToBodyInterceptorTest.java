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

package software.amazon.awssdk.protocols.query.interceptor;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.IoUtils;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryParametersToBodyInterceptorTest {

    public static final URI HTTP_LOCALHOST = URI.create("http://localhost:8080");

    private QueryParametersToBodyInterceptor interceptor;
    private ExecutionAttributes executionAttributes;

    private SdkHttpFullRequest.Builder requestBuilder;

    @Before
    public void setup() {

        interceptor = new QueryParametersToBodyInterceptor();
        executionAttributes = new ExecutionAttributes();

        requestBuilder = SdkHttpFullRequest.builder()
                .protocol(Protocol.HTTPS.toString())
                .method(SdkHttpMethod.POST)
                .putRawQueryParameter("key", singletonList("value"))
                .uri(HTTP_LOCALHOST);
    }

    @Test
    public void postRequestsWithNoBodyHaveTheirParametersMovedToTheBody() throws Exception {

        SdkHttpFullRequest request = requestBuilder.build();

        SdkHttpFullRequest output = (SdkHttpFullRequest) interceptor.modifyHttpRequest(
                new HttpRequestOnlyContext(request, null), executionAttributes);

        assertThat(output.rawQueryParameters()).hasSize(0);
        assertThat(output.headers())
                .containsKey("Content-Length")
                .containsEntry("Content-Type", singletonList("application/x-www-form-urlencoded; charset=utf-8"));
        assertThat(output.contentStreamProvider()).isNotEmpty();
    }

    @Test
    public void nonPostRequestsWithNoBodyAreUnaltered() throws Exception {
        Stream.of(SdkHttpMethod.values())
              .filter(m -> !m.equals(SdkHttpMethod.POST))
              .forEach(this::nonPostRequestsUnaltered);
    }

    @Test
    public void postWithContentIsUnaltered() throws Exception {
        byte[] contentBytes = "hello".getBytes(StandardCharsets.UTF_8);
        ContentStreamProvider contentProvider = () -> new ByteArrayInputStream(contentBytes);

        SdkHttpFullRequest request = requestBuilder.contentStreamProvider(contentProvider).build();

        SdkHttpFullRequest output = (SdkHttpFullRequest) interceptor.modifyHttpRequest(
                new HttpRequestOnlyContext(request, null), executionAttributes);

        assertThat(output.rawQueryParameters()).hasSize(1);
        assertThat(output.headers()).hasSize(0);
        assertThat(IoUtils.toByteArray(output.contentStreamProvider().get().newStream())).isEqualTo(contentBytes);
    }

    @Test
    public void onlyAlterRequestsIfParamsArePresent() throws Exception {
        SdkHttpFullRequest request = requestBuilder.clearQueryParameters().build();

        SdkHttpFullRequest output = (SdkHttpFullRequest) interceptor.modifyHttpRequest(
                new HttpRequestOnlyContext(request, null), executionAttributes);

        assertThat(output.rawQueryParameters()).hasSize(0);
        assertThat(output.headers()).hasSize(0);
        assertThat(output.contentStreamProvider()).isEmpty();
    }

    private void nonPostRequestsUnaltered(SdkHttpMethod method) {

        SdkHttpFullRequest request = requestBuilder.method(method).build();

        SdkHttpFullRequest output = (SdkHttpFullRequest) interceptor.modifyHttpRequest(
                new HttpRequestOnlyContext(request, null), executionAttributes);

        assertThat(output.rawQueryParameters()).hasSize(1);
        assertThat(output.headers()).hasSize(0);
        assertThat(output.contentStreamProvider()).isEmpty();
    }

    public final class HttpRequestOnlyContext implements software.amazon.awssdk.core.interceptor.Context.ModifyHttpRequest {

        private final SdkHttpRequest request;
        private final RequestBody requestBody;

        public HttpRequestOnlyContext(SdkHttpRequest request,
                                      RequestBody requestBody) {
            this.request = request;
            this.requestBody = requestBody;
        }

        @Override
        public SdkRequest request() {
            return null;
        }

        @Override
        public SdkHttpRequest httpRequest() {
            return request;
        }

        @Override
        public Optional<RequestBody> requestBody() {
            return Optional.ofNullable(requestBody);
        }

        @Override
        public Optional<AsyncRequestBody> asyncRequestBody() {
            return Optional.empty();
        }
    }
}
