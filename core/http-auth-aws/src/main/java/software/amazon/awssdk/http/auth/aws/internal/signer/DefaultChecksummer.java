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

import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.getBinaryRequestPayloadStream;
import static software.amazon.awssdk.http.auth.aws.util.SignerUtils.hash;
import static software.amazon.awssdk.utils.BinaryUtils.toHex;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.auth.aws.internal.util.DigestComputingSubscriber;
import software.amazon.awssdk.http.auth.aws.signer.Checksummer;
import software.amazon.awssdk.utils.BinaryUtils;

@SdkProtectedApi
public final class DefaultChecksummer implements Checksummer {

    @Override
    public String checksum(ContentStreamProvider payload) {
        InputStream payloadStream = getBinaryRequestPayloadStream(payload);
        return toHex(
            hash(payloadStream)
        );
    }

    @Override
    public CompletableFuture<String> checksum(Publisher<ByteBuffer> payload) {
        DigestComputingSubscriber bodyDigester = DigestComputingSubscriber.forSha256();

        if (payload != null) {
            payload.subscribe(bodyDigester);
        }

        return bodyDigester.digestBytes().thenApply(BinaryUtils::toHex);
    }
}
