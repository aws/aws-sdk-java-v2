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

package software.amazon.awssdk.messagemanager.sns.internal;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * The canonical bytes and decoded signature of one message, ready to be verified against any number of public keys.
 * Obtained from {@link SignatureValidator#prepare}.
 */
@SdkInternalApi
final class PreparedSignature {
    private final byte[] canonicalMessage;
    private final byte[] signature;
    private final String algorithm;

    PreparedSignature(byte[] canonicalMessage, byte[] signature, String algorithm) {
        this.canonicalMessage = canonicalMessage;
        this.signature = signature;
        this.algorithm = algorithm;
    }

    /**
     * Returns whether the signature verifies against {@code publicKey}. A key the JCA rejects simply does not verify.
     */
    boolean verifies(PublicKey publicKey) {
        try {
            return doVerify(publicKey);
        } catch (InvalidKeyException | SignatureException e) {
            return false;
        }
    }

    /**
     * Verifies the signature against {@code publicKey}, throwing {@link SdkClientException} when it does not verify.
     */
    void verify(PublicKey publicKey) {
        boolean verified;
        try {
            verified = doVerify(publicKey);
        } catch (InvalidKeyException e) {
            throw SdkClientException.create("The public key is invalid", e);
        } catch (SignatureException e) {
            throw SdkClientException.create("The signature is invalid", e);
        }

        if (!verified) {
            throw SdkClientException.create("The computed signature did not match the expected signature");
        }
    }

    private boolean doVerify(PublicKey publicKey) throws InvalidKeyException, SignatureException {
        Signature verifier = newVerifier();
        verifier.initVerify(publicKey);
        verifier.update(canonicalMessage);
        return verifier.verify(signature);
    }

    private Signature newVerifier() {
        try {
            return Signature.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to create Signature for " + algorithm, e);
        }
    }
}
