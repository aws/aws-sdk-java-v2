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
    private static final TokenIdentity IDENTITY = TokenIdentity.create("token");

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
                                           .identity(IDENTITY) // Note, this is doable
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
                                                 .identity(IDENTITY) // Note, this is doable
                                                 .putProperty(KEY, VALUE)
                                                 .build());
        assertNotNull(signedRequest);
    }

    /**
     * NoOp Signer that asserts that the input created via builder or Consumer builder pattern are set up correctly.
     * This is similar to what a bearerTokenSigner would look like - which would insert the identity in a Header.

     */
    private static class TestSigner implements HttpSigner<TokenIdentity> {
        @Override
        public SyncSignedHttpRequest sign(SyncHttpSignRequest<? extends TokenIdentity> request) {
            assertEquals(VALUE, request.property(KEY));
            assertEquals(IDENTITY, request.identity());

            return SyncSignedHttpRequest.builder()
                                        .request(addTokenHeader(request))
                                        .payload(request.payload().orElse(null))
                                        .build();
        }

        @Override
        public AsyncSignedHttpRequest signAsync(AsyncHttpSignRequest<? extends TokenIdentity> request) {
            assertEquals(VALUE, request.property(KEY));
            assertEquals(IDENTITY, request.identity());

            return AsyncSignedHttpRequest.builder()
                                         .request(addTokenHeader(request))
                                         .payload(request.payload().orElse(null))
                                         .build();
        }

        private SdkHttpRequest addTokenHeader(HttpSignRequest<?, ? extends TokenIdentity> input) {
            // return input.request().copy(b -> b.putHeader("Token-Header", input.identity().token()));
            return input.request();
        }
    }
}
