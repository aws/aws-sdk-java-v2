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

package software.amazon.awssdk.http.auth.spi.signer;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * {@code HttpSigner} properties intended for use only by internal components of the SDK and SDK-provided implementations of
 * this SPI.
 */
@SdkProtectedApi
public final class SdkInternalHttpSignerProperty {

    /**
     * A cache for storing checksums calculated for a payload.
     *
     * <p>Note, checksums may not be relevant to some signers.
     */
    public static final SignerProperty<PayloadChecksumStore> CHECKSUM_CACHE =
        SignerProperty.create(SdkInternalHttpSignerProperty.class, "ChecksumCache");

    private SdkInternalHttpSignerProperty() {
    }
}
