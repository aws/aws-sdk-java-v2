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

package software.amazon.awssdk.services.signin.internal;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.utils.Pair;

/**
 * An identity representing the public and private keys required to sign a request using DPoP.
 */
@SdkInternalApi
public class DpopIdentity implements Identity {
    private final ECPublicKey publicKey;
    private final ECPrivateKey privateKey;

    private DpopIdentity(ECPublicKey publicKey, ECPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public static DpopIdentity create(ECPublicKey publicKey, ECPrivateKey privateKey) {
        return new DpopIdentity(publicKey, privateKey);
    }

    public static DpopIdentity create(String dpopKeyPem) {
        Pair<ECPrivateKey, ECPublicKey> keys = EcKeyLoader.loadSec1Pem(dpopKeyPem);
        return new DpopIdentity(keys.right(), keys.left());
    }

    public ECPublicKey getPublicKey() {
        return publicKey;
    }

    public ECPrivateKey getPrivateKey() {
        return privateKey;
    }
}
