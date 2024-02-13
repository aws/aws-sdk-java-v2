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

package software.amazon.awssdk.awscore.client.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.StringInputStream;

public class AwsClientHandlerUtilsTest {

    @Test
    void nonNullPayload_shouldEncodeToEmptyMessage() {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(URI.create("http://localhost"))
                                                       .contentStreamProvider(() -> new StringInputStream("test"))
                                                       .build();
        ByteBuffer buffer = AwsClientHandlerUtils.encodeEventStreamRequestToByteBuffer(request);
        assertThat(buffer).isNotNull();
    }

    @Test
    void nullPayload_shouldEncodeToEmptyMessage() {
        SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                       .method(SdkHttpMethod.GET)
                                                       .uri(URI.create("http://localhost")).build();
        ByteBuffer buffer = AwsClientHandlerUtils.encodeEventStreamRequestToByteBuffer(request);
        assertThat(buffer).isNotNull();
    }
}
