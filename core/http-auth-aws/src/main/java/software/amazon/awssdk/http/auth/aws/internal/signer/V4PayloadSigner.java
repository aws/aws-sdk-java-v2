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

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * An interface for defining how to sign a payload via SigV4.
 */
@SdkInternalApi
public interface V4PayloadSigner {
    /**
     * Get a default implementation of a SigV4 payload signer.
     */
    static V4PayloadSigner create() {
        return new DefaultV4PayloadSigner();
    }

    /**
     * Given a payload and v4-context, sign the payload via the SigV4 process.
     */
    ContentStreamProvider sign(ContentStreamProvider payload, V4Context v4Context);

    /**
     * Given a payload and v4-context, sign the payload via the SigV4 process.
     */
    Publisher<ByteBuffer> signAsync(Publisher<ByteBuffer> payload, V4Context v4Context);

    /**
     * Modify a request before it is signed, such as changing headers or query-parameters.
     */
    default void beforeSigning(SdkHttpRequest.Builder request, ContentStreamProvider payload) {
    }
}
