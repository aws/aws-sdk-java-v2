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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

public class DefaultChecksummerTest {

    ContentStreamProvider payload = () -> new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8));
    Publisher<ByteBuffer> payloadAsync = Flowable.just(ByteBuffer.wrap("foo".getBytes(StandardCharsets.UTF_8)));


    SdkHttpRequest.Builder request = SdkHttpRequest.builder()
                                           .uri(URI.create("https://localhost"))
                                           .method(SdkHttpMethod.GET);

    @Test
    public void checksummer_shouldAddSha256ChecksumToAmzContentSha256Header() {
        DefaultChecksummer checksummer = new DefaultChecksummer();
        SdkHttpRequest expectedRequest = request
            .putHeader("x-amz-content-sha256", "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae")
            .build();

        checksummer.checksum(payload, request);

        assertEquals(expectedRequest.toString(), request.build().toString());
    }

    @Test
    public void checksummerAsync_shouldAddSha256ChecksumToAmzContentSha256Header() {
        DefaultChecksummer checksummer = new DefaultChecksummer();
        SdkHttpRequest expectedRequest = request
            .putHeader("x-amz-content-sha256", "2c26b46b68ffc68ff99b453c1d30413413422d706483bfa0f98a5e886266e7ae")
            .build();

        checksummer.checksum(payloadAsync, request);

        assertEquals(expectedRequest.toString(), request.build().toString());
    }
}
