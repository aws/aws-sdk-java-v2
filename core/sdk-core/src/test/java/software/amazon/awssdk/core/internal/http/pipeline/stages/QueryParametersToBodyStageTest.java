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

package software.amazon.awssdk.core.internal.http.pipeline.stages;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.Protocol;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.IoUtils;

public class QueryParametersToBodyStageTest {
    public static final URI HTTP_LOCALHOST = URI.create("http://localhost:8080");
    private QueryParametersToBodyStage stage;
    private RequestExecutionContext context;
    private SdkHttpFullRequest.Builder requestBuilder;

    @BeforeEach
    public void setup() {
        stage = new QueryParametersToBodyStage();
        context = RequestExecutionContext.builder()
                                         .originalRequest(NoopTestRequest.builder().build())
                                         .executionContext(ExecutionContext.builder().build())
                                         .build();
        requestBuilder = SdkHttpFullRequest.builder()
                                           .protocol(Protocol.HTTPS.toString())
                                           .method(SdkHttpMethod.POST)
                                           .putRawQueryParameter("key", singletonList("value"))
                                           .uri(HTTP_LOCALHOST);
    }

    @Test
    public void postRequestsWithNoBodyHaveTheirParametersMovedToTheBody() throws Exception {
        SdkHttpFullRequest output = stage.execute(requestBuilder, context).build();

        assertThat(output.rawQueryParameters()).hasSize(0);
        assertThat(output.headers())
            .containsKey("Content-Length")
            .containsEntry("Content-Type", singletonList("application/x-www-form-urlencoded; charset=utf-8"));
        assertThat(output.contentStreamProvider()).isNotEmpty();
    }

    @Test
    public void nonPostRequestsWithNoBodyAreUnaltered() {
        Stream.of(SdkHttpMethod.values())
              .filter(m -> !m.equals(SdkHttpMethod.POST))
              .forEach(method -> {
                  try {
                      nonPostRequestsUnaltered(method);
                  } catch (Exception e) {
                      fail("Exception thrown during stage execution");
                  }
              });
    }

    @Test
    public void postWithContentIsUnaltered() throws Exception {
        byte[] contentBytes = "hello".getBytes(StandardCharsets.UTF_8);
        ContentStreamProvider contentProvider = () -> new ByteArrayInputStream(contentBytes);

        requestBuilder = requestBuilder.contentStreamProvider(contentProvider);

        SdkHttpFullRequest output = stage.execute(requestBuilder, context).build();

        assertThat(output.rawQueryParameters()).hasSize(1);
        assertThat(output.headers()).hasSize(0);
        assertThat(IoUtils.toByteArray(output.contentStreamProvider().get().newStream())).isEqualTo(contentBytes);
    }

    @Test
    public void onlyAlterRequestsIfParamsArePresent() throws Exception {
        requestBuilder = requestBuilder.clearQueryParameters();

        SdkHttpFullRequest output = stage.execute(requestBuilder, context).build();

        assertThat(output.rawQueryParameters()).hasSize(0);
        assertThat(output.headers()).hasSize(0);
        assertThat(output.contentStreamProvider()).isEmpty();
    }

    private void nonPostRequestsUnaltered(SdkHttpMethod method) throws Exception {
        requestBuilder = requestBuilder.method(method);

        SdkHttpFullRequest output = stage.execute(requestBuilder, context).build();
        assertThat(output.rawQueryParameters()).hasSize(1);
        assertThat(output.headers()).hasSize(0);
        assertThat(output.contentStreamProvider()).isEmpty();
    }
}
