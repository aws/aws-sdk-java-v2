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

import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.http.ContentStreamProvider;

/**
 * Interface for the process of modifying a request destined for a service so that the service can authenticate the SDK
 * customer’s identity.
 */
@SdkPublicApi
public interface HttpSigner<IdentityT> {

    /**
     * Method that takes in inputs to sign a request with sync payload and returns a signed version of the request.
     *
     * @param identity The identity to use to sign the request.
     * @param request The inputs to sign a request.
     * @return A signed version of the request.
     */
    SignedHttpRequest<ContentStreamProvider> sign(IdentityT identity, HttpSignRequest<ContentStreamProvider> request);

    /**
     * Method that takes in inputs to sign a request with async payload and returns a signed version of the request.
     *
     * @param identity The identity to use to sign the request.
     * @param request The inputs to sign a request.
     * @return A signed version of the request.
     */
    SignedHttpRequest<Publisher<ByteBuffer>> signAsync(IdentityT identity, HttpSignRequest<Publisher<ByteBuffer>> request);
}
