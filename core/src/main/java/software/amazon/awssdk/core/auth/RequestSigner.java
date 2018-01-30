/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.auth;

import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * A strategy for applying cryptographic signatures to a request, proving
 * that the request was made by someone in possession of the given set of
 * credentials without transmitting the secret key over the wire.
 */

public interface RequestSigner {

    /**
     * Sign the given request - modifies the
     * passed-in request to apply the signature.
     *
     * @param request      The request to sign.
     */
    SdkHttpFullRequest sign(SdkHttpFullRequest request);
}
