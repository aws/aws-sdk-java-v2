/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.auth;

import java.util.Date;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.interceptor.Context;
import software.amazon.awssdk.interceptor.ExecutionAttributes;

/**
 * A request signer that has special-case logic to presign requests, generating
 * a URL which embeds the signature suitable for hyperlinking.
 */
public interface Presigner {
    /**
     * Signs the request by adding the signature to the URL rather than as a
     * header. This method is expected to modify the passed-in request to
     * add the signature.
     *
     * @param signingRequest The data required for performing the signing
     * @param expiration    The time when this presigned URL will expire.
     */
    SdkHttpFullRequest presign(Context.BeforeTransmission execution,
                               ExecutionAttributes executionAttributes,
                               Date expiration);
}
