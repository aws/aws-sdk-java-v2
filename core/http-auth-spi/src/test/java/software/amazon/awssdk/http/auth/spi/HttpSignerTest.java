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

package software.amazon.awssdk.http.auth.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.identity.spi.TokenIdentity;

public class HttpSignerTest {

    private static final SignerProperty<String> KEY = SignerProperty.create(String.class, "key");
    private static final String VALUE = "value";
    private TokenIdentity IDENTITY = TokenIdentity.create("token");

    final HttpSigner<TokenIdentity> signer = new TestSigner();

    @Test
    public void sign_usingConsumerBuilder_works() {
        SyncSignedHttpRequest signedRequest = signer.sign(r -> r.request(mock(SdkHttpRequest.class))
                                                                .identity(IDENTITY)
                                                                .putProperty(KEY, VALUE));
        assertNotNull(signedRequest);
    }

    @Test
    public void sign_usingRequest_works() {
        SyncSignedHttpRequest signedRequest =
            signer.sign(SyncHttpSignRequest.builder(IDENTITY)
                                           .request(mock(SdkHttpRequest.class))
                                           //.identity(x) // Note, this is doable
                                           .putProperty(KEY, VALUE)
                                           .build());
        assertNotNull(signedRequest);
    }

    @Test
    public void signAsync_usingConsumerBuilder_works() {
        Publisher<ByteBuffer> payload = subscriber -> {};
        AsyncSignedHttpRequest signedRequest = signer.signAsync(r -> r.request(mock(SdkHttpRequest.class))
                                                                      .payload(payload)
                                                                      .identity(IDENTITY)
                                                                      .putProperty(KEY, VALUE));
        assertNotNull(signedRequest);
    }

    @Test
    public void signAsync_usingRequest_works() {
        Publisher<ByteBuffer> payload = subscriber -> {};
        AsyncSignedHttpRequest signedRequest =
            signer.signAsync(AsyncHttpSignRequest.builder(IDENTITY)
                                                 .request(mock(SdkHttpRequest.class))
                                                 .payload(payload)
                                                 //.identity(x) // Note, this is doable
                                                 .putProperty(KEY, VALUE)
                                                 .build());
        assertNotNull(signedRequest);
    }

    /**
     * NoOp Signer that asserts that the HttpSignRequest created via builder or Consumer builder pattern are set up correctly,
     * e.g., have the correct payloadType, and makes sure that payloadType works with SignedHttpRequest without runtime
     * exceptions.
     */
    private class TestSigner implements HttpSigner<TokenIdentity> {
        @Override
        public SyncSignedHttpRequest sign(SyncHttpSignRequest<TokenIdentity> request) {
            assertEquals(VALUE, request.property(KEY));
            assertEquals(IDENTITY, request.identity());

            return SyncSignedHttpRequest.builder()
                                    .request(request.request())
                                    .payload(request.payload().orElse(null))
                                    .build();
        }

        @Override
        public AsyncSignedHttpRequest signAsync(AsyncHttpSignRequest<TokenIdentity> request) {
            assertEquals(VALUE, request.property(KEY));
            assertEquals(IDENTITY, request.identity());

            return AsyncSignedHttpRequest.builder()
                                    .request(request.request())
                                    .payload(request.payload().orElse(null))
                                    .build();
        }
    }
}
