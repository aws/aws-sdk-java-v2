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

import static software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils.computeSignature;
import static software.amazon.awssdk.utils.BinaryUtils.toHex;

import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A class which calculates a rolling signature of arbitrary data using HMAC-SHA256. Each time a signature is calculated, the
 * prior calculation is incorporated, hence "rolling".
 */
@SdkInternalApi
public final class RollingSigner {

    private final byte[] signingKey;
    private final String seedSignature;
    private String previousSignature;

    public RollingSigner(byte[] signingKey, String seedSignature) {
        this.seedSignature = seedSignature;
        this.previousSignature = seedSignature;
        this.signingKey = signingKey.clone();
    }

    /**
     * Using a template that incorporates the previous calculated signature, sign the string and return it.
     */
    public String sign(Function<String, String> stringToSignTemplate) {
        String stringToSign = stringToSignTemplate.apply(previousSignature);
        byte[] bytes = computeSignature(stringToSign, signingKey);
        previousSignature = toHex(bytes);
        return previousSignature;
    }

    public void reset() {
        previousSignature = seedSignature;
    }
}
