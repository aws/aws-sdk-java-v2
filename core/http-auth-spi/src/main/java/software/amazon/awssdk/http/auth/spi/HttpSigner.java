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
 * customer’s identity
 */
@SdkPublicApi
public interface HttpSigner {

    /**
     * Method that takes in a request and returns a signed version of the request.
     *
     * @param request The request to sign, with sync payload
     * @return A signed version of the input request
     */
    SignedHttpRequest<? extends ContentStreamProvider> sign(HttpSignRequest<? extends ContentStreamProvider> request);

    /**
     * Method that takes in a request and returns a signed version of the request.
     *
     * @param request The request to sign, with async payload
     * @return A signed version of the input request
     */
    SignedHttpRequest<? extends Publisher<? extends ByteBuffer>> signAsync(
            HttpSignRequest<? extends Publisher<? extends ByteBuffer>> request);
}
