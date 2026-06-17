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

package software.amazon.awssdk.core.internal.crac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.utils.IoUtils;
import utils.ValidSdkObjects;

class CannedResponseHttpClientTest {

    private static final byte[] PAYLOAD = "{\"StringMember\":\"warmup\"}".getBytes(StandardCharsets.UTF_8);

    private static HttpExecuteRequest request() {
        return HttpExecuteRequest.builder()
                                 .request(ValidSdkObjects.sdkHttpFullRequest().build())
                                 .build();
    }

    private static byte[] readBody(HttpExecuteResponse response) throws IOException {
        return IoUtils.toByteArray(response.responseBody().orElseThrow(() -> new AssertionError("expected a body")));
    }

    @Test
    void call_whenNoStatusGiven_returnsDefault200() throws IOException {
        CannedResponseHttpClient client = CannedResponseHttpClient.builder().responseBody(PAYLOAD).build();

        HttpExecuteResponse response = client.prepareRequest(request()).call();

        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
    }

    @Test
    void call_whenStatusGiven_returnsConfiguredStatus() throws IOException {
        CannedResponseHttpClient client = CannedResponseHttpClient.builder().responseBody(PAYLOAD).statusCode(404).build();

        HttpExecuteResponse response = client.prepareRequest(request()).call();

        assertThat(response.httpResponse().statusCode()).isEqualTo(404);
    }

    @Test
    void call_whenInvoked_returnsFullyDrainableBody() throws IOException {
        CannedResponseHttpClient client = CannedResponseHttpClient.builder().responseBody(PAYLOAD).build();

        byte[] body = readBody(client.prepareRequest(request()).call());

        assertThat(body).isEqualTo(PAYLOAD);
    }

    @Test
    void call_whenInvokedRepeatedly_eachCallGetsFreshReadableStream() throws IOException {
        CannedResponseHttpClient client = CannedResponseHttpClient.builder().responseBody(PAYLOAD).build();

        byte[] first = readBody(client.prepareRequest(request()).call());
        byte[] second = readBody(client.prepareRequest(request()).call());

        assertThat(first).isEqualTo(PAYLOAD);
        assertThat(second).isEqualTo(PAYLOAD);
    }

    @Test
    void closeAndAbort_areSafeNoOps() {
        CannedResponseHttpClient client = CannedResponseHttpClient.builder().responseBody(PAYLOAD).build();
        ExecutableHttpRequest executableRequest = client.prepareRequest(request());

        assertThatCode(executableRequest::abort).doesNotThrowAnyException();
        assertThatCode(client::close).doesNotThrowAnyException();
    }

    @Test
    void call_whenNoBodyGiven_returnsDefault200AndEmptyBody() throws IOException {
        CannedResponseHttpClient client = CannedResponseHttpClient.builder().build();

        HttpExecuteResponse response = client.prepareRequest(request()).call();

        assertThat(response.httpResponse().statusCode()).isEqualTo(200);
        assertThat(readBody(response)).isEmpty();
    }

}
