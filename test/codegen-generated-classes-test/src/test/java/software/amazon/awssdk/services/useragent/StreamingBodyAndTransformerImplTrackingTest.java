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

package software.amazon.awssdk.services.useragent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigAsyncClient;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.ProtocolRestJsonWithConfigClient;
import software.amazon.awssdk.services.protocolrestjsonwithconfig.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.testutils.RandomTempFile;

public class StreamingBodyAndTransformerImplTrackingTest {

    private CapturingInterceptor interceptor;

    @BeforeEach
    public void setup() {
        this.interceptor = new CapturingInterceptor();
    }

    @Test
    public void streamingInputOperation_syncClient_stringBody_recordsMetadata() {
        callStreamingInputOperation(syncClient(), RequestBody.fromString("body"));
        assertThat(interceptor.userAgent()).contains("md/rb#b");
    }

    @Test
    public void streamingInputOperation_syncClient_fileBody_recordsMetadata() throws IOException {
        callStreamingInputOperation(syncClient(), RequestBody.fromFile(new RandomTempFile(64)));
        assertThat(interceptor.userAgent()).contains("md/rb#f");
    }

    @Test
    public void streamingInputOperation_syncClient_streamBody_recordsMetadata() throws IOException {
        callStreamingInputOperation(
            syncClient(),
            RequestBody.fromInputStream(new ByteArrayInputStream(new byte[64]), 64));
        assertThat(interceptor.userAgent()).contains("md/rb#s");
    }

    @Test
    public void streamingInputOperation_asyncClient_stringBody_recordsMetadata() {
        callStreamingInputOperation(asyncClient(), AsyncRequestBody.fromString("body"));
        assertThat(interceptor.userAgent()).contains("md/rb#b");
    }

    @Test
    public void streamingInputOperation_asyncClient_fileBody_recordsMetadata() throws IOException {
        callStreamingInputOperation(asyncClient(), AsyncRequestBody.fromFile(new RandomTempFile(64)));
        assertThat(interceptor.userAgent()).contains("md/rb#f");
    }

    @Test
    public void streamingInputOperation_asyncClient_streamBody_recordsMetadata() throws IOException {
        callStreamingInputOperation(
            asyncClient(),
            AsyncRequestBody.fromInputStream(new ByteArrayInputStream(new byte[64]), 64L, Executors.newSingleThreadExecutor())
        );
        assertThat(interceptor.userAgent()).contains("md/rb#s");
    }

    @Test
    public void streamingOutputOperation_syncClient_bytes_recordsMetadata() {
        callStreamingOutputOperation(syncClient(), ResponseTransformer.toBytes());
        assertThat(interceptor.userAgent()).contains("md/rt#b");
    }

    @Test
    public void streamingOutputOperation_syncClient_file_recordsMetadata() throws IOException {
        callStreamingOutputOperation(syncClient(), ResponseTransformer.toFile(new RandomTempFile(0)));
        assertThat(interceptor.userAgent()).contains("md/rt#f");
    }

    private ProtocolRestJsonWithConfigClient syncClient() {
        return ProtocolRestJsonWithConfigClient
            .builder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .build();
    }

    private static void callStreamingInputOperation(ProtocolRestJsonWithConfigClient client, RequestBody requestBody) {
        assertThatThrownBy(() -> client.streamingInputOperation(r -> {}, requestBody))
            .hasMessageContaining("stop");
    }

    private void callStreamingOutputOperation(
        ProtocolRestJsonWithConfigClient client, ResponseTransformer<StreamingOutputOperationResponse, ?> transformer) {
        assertThatThrownBy(() -> client.streamingOutputOperation(r -> {}, transformer))
            .hasMessageContaining("stop");
    }

    private ProtocolRestJsonWithConfigAsyncClient asyncClient() {
        return ProtocolRestJsonWithConfigAsyncClient
            .builder()
            .overrideConfiguration(c -> c.addExecutionInterceptor(interceptor))
            .build();
    }

    private static void callStreamingInputOperation(ProtocolRestJsonWithConfigAsyncClient client, AsyncRequestBody requestBody) {
        assertThatThrownBy(() -> {
            client.streamingInputOperation(
                r -> {
                    r.overrideConfiguration(
                        c -> c.putHeader("x-amz-content-sha256", "value"));
                },
                requestBody).join();
        }).hasMessageContaining("stop");
    }

    public static class CapturingInterceptor implements ExecutionInterceptor {
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw new RuntimeException("stop");
        }

        public String userAgent() {
            return context.httpRequest().headers().get("User-Agent").get(0);
        }
    }
}
