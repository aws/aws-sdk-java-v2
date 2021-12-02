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

package software.amazon.awssdk.protocol.tests.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.protocol.tests.util.ClosableStringInputStream;
import software.amazon.awssdk.protocol.tests.util.MockHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.ProtocolRestJsonException;
import software.amazon.awssdk.services.protocolrestjson.model.StreamingOutputOperationResponse;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Tests to verify the correction of connection closure.
 */
public class SyncClientConnectionTest {
    private ProtocolRestJsonClient client;
    private MockHttpClient mockHttpClient;

    @Before
    public void setupClient() {
        mockHttpClient = new MockHttpClient();
        client = ProtocolRestJsonClient.builder()
                                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                       "skid")))
                                       .region(Region.US_EAST_1)
                                       .httpClient(mockHttpClient)
                                       .build();
    }

    @Test
    public void nonStreaming_exception_shouldCloseConnection() throws IOException {
        ClosableStringInputStream inputStream = new ClosableStringInputStream("{\"__type\":\"SomeUnknownType\"}");
        mockHttpClient.stubNextResponse(mockResponse(inputStream, 400));

        assertThatThrownBy(() -> client.allTypes(AllTypesRequest.builder().build()))
            .isExactlyInstanceOf(ProtocolRestJsonException.class);
        assertThat(inputStream.isClosed()).isTrue();
    }

    @Test
    public void nonStreaming_successfulResponse_shouldCloseConnection() {
        ClosableStringInputStream inputStream = new ClosableStringInputStream("{}");
        mockHttpClient.stubNextResponse(mockResponse(inputStream, 200));
        client.allTypes(AllTypesRequest.builder().build());
        assertThat(inputStream.isClosed()).isTrue();
    }

    @Test
    public void streamingOut_successfulResponse_shouldCloseConnection() {
        ClosableStringInputStream inputStream = new ClosableStringInputStream("{}");
        mockHttpClient.stubNextResponse(mockResponse(inputStream, 200));

        client.streamingOutputOperation(b -> b.build(), ResponseTransformer.toBytes());
        assertThat(inputStream.isClosed()).isTrue();
    }

    @Test
    public void streamingOut_errorResponse_shouldCloseConnection() {
        ClosableStringInputStream inputStream = new ClosableStringInputStream("{\"__type\":\"SomeUnknownType\"}");
        mockHttpClient.stubNextResponse(mockResponse(inputStream, 400));

        assertThatThrownBy(() -> client.streamingOutputOperation(b -> b.build(), ResponseTransformer.toBytes()))
            .isExactlyInstanceOf(ProtocolRestJsonException.class);
        assertThat(inputStream.isClosed()).isTrue();
    }

    @Test
    public void streamingOut_connectionLeftOpen_shouldNotCloseStream() {
        ClosableStringInputStream inputStream = new ClosableStringInputStream("{}");
        mockHttpClient.stubNextResponse(mockResponse(inputStream, 200));

        client.streamingOutputOperation(b -> b.build(), new ResponseTransformer<StreamingOutputOperationResponse, Object>() {

            @Override
            public Object transform(StreamingOutputOperationResponse response, AbortableInputStream inputStream) throws Exception {
                return null;
            }

            @Override
            public boolean needsConnectionLeftOpen() {
                return true;
            }
        });
        assertThat(inputStream.isClosed()).isFalse();
    }

    @Test
    public void streamingOut_toInputStream_closeResponseStreamShouldCloseUnderlyingStream() throws IOException {
        ClosableStringInputStream inputStream = new ClosableStringInputStream("{}");
        mockHttpClient.stubNextResponse(mockResponse(inputStream, 200));

        ResponseInputStream<StreamingOutputOperationResponse> responseInputStream =
            client.streamingOutputOperation(b -> b.build(), ResponseTransformer.toInputStream());

        assertThat(inputStream.isClosed()).isFalse();
        responseInputStream.close();
        assertThat(inputStream.isClosed()).isTrue();
    }

    @Test
    public void streamingIn_providedRequestBody_shouldNotCloseRequestStream() {
        ClosableStringInputStream responseStream = new ClosableStringInputStream("{}");
        ClosableStringInputStream requestStream = new ClosableStringInputStream("{}");
        mockHttpClient.stubNextResponse(mockResponse(responseStream, 200));

        RequestBody requestBody = RequestBody.fromInputStream(requestStream, requestStream.getString().getBytes().length);
        client.streamingInputOperation(SdkBuilder::build, requestBody);
        assertThat(responseStream.isClosed()).isTrue();
        assertThat(requestStream.isClosed()).isFalse();
    }

    @Test
    public void closeClient_nonManagedHttpClient_shouldNotCloseUnderlyingHttpClient() {
        client.close();
        assertThat(mockHttpClient.isClosed()).isFalse();
    }

    private HttpExecuteResponse mockResponse(InputStream inputStream, int statusCode) {
        return HttpExecuteResponse.builder()
                                  .response(SdkHttpResponse.builder().statusCode(statusCode).build())
                                  .responseBody(AbortableInputStream.create(inputStream))
                                  .build();
    }
}
