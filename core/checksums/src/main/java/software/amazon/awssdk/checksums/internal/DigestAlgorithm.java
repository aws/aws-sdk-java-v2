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

package software.amazon.awssdk.checksums.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public enum DigestAlgorithm {

    SHA1("SHA-1"),
    MD5("MD5"),
    SHA256("SHA-256")
    ;

    private final String algorithmName;
    private final DigestThreadLocal digestReference;

    DigestAlgorithm(String algorithmName) {
        this.algorithmName = algorithmName;
        digestReference = new DigestThreadLocal(algorithmName);
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Returns the thread local reference for the {@link MessageDigest} algorithm
     */
    public MessageDigest getDigest() {
        MessageDigest digest = digestReference.get();
        digest.reset();
        return digest;
    }

    private static class DigestThreadLocal extends ThreadLocal<MessageDigest> {
        private final String algorithmName;

        DigestThreadLocal(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        @Override
        protected MessageDigest initialValue() {
            try {
                return MessageDigest.getInstance(algorithmName);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Unable to fetch message digest instance for Algorithm "
                                           + algorithmName + ": " + e.getMessage(), e);
            }
        }
    }
}
