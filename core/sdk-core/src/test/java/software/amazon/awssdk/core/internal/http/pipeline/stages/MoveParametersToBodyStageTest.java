/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.Test;
import software.amazon.awssdk.core.http.ExecutionContext;
import software.amazon.awssdk.core.http.NoopTestRequest;
import software.amazon.awssdk.core.internal.http.RequestExecutionContext;
import software.amazon.awssdk.core.internal.http.timers.ClientExecutionAndRequestTimerTestUtils;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.IoUtils;
import utils.ValidSdkObjects;

public class MoveParametersToBodyStageTest {

    private final MoveParametersToBodyStage sut = new MoveParametersToBodyStage();

    @Test
    public void postRequestsWithNoBodyHaveTheirParametersMovedToTheBody() throws Exception {
        SdkHttpFullRequest.Builder mutableRequest = ValidSdkObjects.sdkHttpFullRequest()
                                                                   .method(SdkHttpMethod.POST)
                                                                   .putRawQueryParameter("key", singletonList("value"));

        SdkHttpFullRequest output = sut.execute(mutableRequest, requestContext(mutableRequest)).build();

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
        SdkHttpFullRequest.Builder mutableRequest = ValidSdkObjects.sdkHttpFullRequest()
                                                                   .contentStreamProvider(contentProvider)
                                                                   .method(SdkHttpMethod.POST)
                                                                   .clearHeaders()
                                                                   .putRawQueryParameter("key", singletonList("value"));

        SdkHttpFullRequest output = sut.execute(mutableRequest, requestContext(mutableRequest)).build();

        assertThat(output.rawQueryParameters()).hasSize(1);
        assertThat(output.headers()).hasSize(0);
        assertThat(IoUtils.toByteArray(output.contentStreamProvider().get().newStream())).isEqualTo(contentBytes);
    }

    @Test
    public void onlyAlterRequestsIfParamsArePresent() throws Exception {
        SdkHttpFullRequest.Builder mutableRequest = ValidSdkObjects.sdkHttpFullRequest()
                                                                   .clearHeaders()
                                                                   .method(SdkHttpMethod.POST);

        SdkHttpFullRequest output = sut.execute(mutableRequest, requestContext(mutableRequest)).build();

        assertThat(output.rawQueryParameters()).hasSize(0);
        assertThat(output.headers()).hasSize(0);
        assertThat(output.contentStreamProvider()).isEmpty();
    }

    private void nonPostRequestsUnaltered(SdkHttpMethod method) {
        SdkHttpFullRequest.Builder mutableRequest = ValidSdkObjects.sdkHttpFullRequest()
                                                                   .method(method)
                                                                   .clearHeaders()
                                                                   .putRawQueryParameter("key", singletonList("value"));

        SdkHttpFullRequest output = sut.execute(mutableRequest, requestContext(mutableRequest)).build();

        assertThat(output.rawQueryParameters()).hasSize(1);
        assertThat(output.headers()).hasSize(0);
        assertThat(output.contentStreamProvider()).isEmpty();
    }

    private RequestExecutionContext requestContext(SdkHttpFullRequest.Builder mutableRequest) {
        ExecutionContext executionContext = ClientExecutionAndRequestTimerTestUtils.executionContext(mutableRequest.build());
        return RequestExecutionContext.builder()
                                      .executionContext(executionContext)
                                      .originalRequest(NoopTestRequest.builder().build())
                                      .build();
    }
}
