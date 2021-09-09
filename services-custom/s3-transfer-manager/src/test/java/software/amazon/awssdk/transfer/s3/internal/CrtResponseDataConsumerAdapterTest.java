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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.amazonaws.s3.model.GetObjectOutput;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RunWith(MockitoJUnitRunner.class)
public class CrtResponseDataConsumerAdapterTest {
    private CrtResponseDataConsumerAdapter<Void> adapter;

    @Mock
    private S3CrtDataPublisher publisher;

    @Mock
    private AsyncResponseTransformer<GetObjectResponse, Void> transformer;

    @Before
    public void setup() {
        ResponseHeadersHandler handler = new ResponseHeadersHandler();
        adapter = new CrtResponseDataConsumerAdapter<>(transformer, publisher, handler);
    }

    @Test
    public void onResponse_noSdkHttpResponse_shouldCreateEmptySdkHttpResponse() {
        adapter.onResponse(GetObjectOutput.builder().build());
        ArgumentCaptor<GetObjectResponse> captor = ArgumentCaptor.forClass(GetObjectResponse.class);
        verify(transformer).onResponse(captor.capture());
        assertThat(captor.getValue().responseMetadata().requestId()).isEqualTo("UNKNOWN");
        assertThat(captor.getValue().sdkHttpResponse()).isNotNull();
    }
}
