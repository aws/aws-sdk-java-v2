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

package software.amazon.awssdk.http.auth.aws.crt.internal.signer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkExtensionProvider;
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class SigV4aChunkExtensionProvider implements ChunkExtensionProvider {

    private final RollingSigner signer;
    private final CredentialScope credentialScope;

    public SigV4aChunkExtensionProvider(RollingSigner signer, CredentialScope credentialScope) {
        this.signer = signer;
        this.credentialScope = credentialScope;
    }

    @Override
    public void reset() {
        signer.reset();
    }

    @Override
    public Pair<byte[], byte[]> get(ByteBuffer chunk) {
        byte[] chunkSig = signer.sign(chunk);
        return Pair.of("chunk-signature".getBytes(StandardCharsets.UTF_8), chunkSig);
    }
}
