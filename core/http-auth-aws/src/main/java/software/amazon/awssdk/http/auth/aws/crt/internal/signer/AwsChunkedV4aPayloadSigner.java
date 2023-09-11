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

import static software.amazon.awssdk.http.auth.aws.internal.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.internal.util.SignerConstant.STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.util.SignerConstant.STREAMING_UNSIGNED_PAYLOAD_TRAILER;
import static software.amazon.awssdk.http.auth.aws.internal.util.SignerUtils.moveContentLength;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.chunkedencoding.ChunkedEncodedInputStream;
import software.amazon.awssdk.http.auth.aws.internal.chunkedencoding.TrailerProvider;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.utils.StringInputStream;

/**
 * A default implementation of a payload signer that is a no-op, since payloads are most commonly unsigned.
 */
@SdkInternalApi
public class AwsChunkedV4aPayloadSigner implements V4aPayloadSigner {

    private final CredentialScope credentialScope;
    private final int chunkSize;

    public AwsChunkedV4aPayloadSigner(CredentialScope credentialScope, int chunkSize) {
        this.credentialScope = credentialScope;
        this.chunkSize = chunkSize;
    }

    @Override
    public ContentStreamProvider sign(ContentStreamProvider payload, V4aContext v4aContext) {
        SdkHttpRequest.Builder request = v4aContext.getSignedRequest();
        moveContentLength(request);

        InputStream inputStream = payload != null ? payload.newStream() : new StringInputStream("");
        ChunkedEncodedInputStream.Builder chunkedEncodedInputStreamBuilder = ChunkedEncodedInputStream
            .builder()
            .inputStream(inputStream)
            .chunkSize(chunkSize)
            .header(chunk -> Integer.toHexString(chunk.length).getBytes(StandardCharsets.UTF_8));

        switch (v4aContext.getSigningConfig().getSignedBodyValue()) {
            case STREAMING_ECDSA_SIGNED_PAYLOAD: {
                RollingSigner rollingSigner = new RollingSigner(v4aContext.getSignature(), v4aContext.getSigningConfig());
                setupSigExt(chunkedEncodedInputStreamBuilder, rollingSigner);
                break;
            }
            case STREAMING_UNSIGNED_PAYLOAD_TRAILER:
                setupChecksumTrailer(chunkedEncodedInputStreamBuilder);
                break;
            case STREAMING_ECDSA_SIGNED_PAYLOAD_TRAILER: {
                RollingSigner rollingSigner = new RollingSigner(v4aContext.getSignature(), v4aContext.getSigningConfig());
                setupSigExt(chunkedEncodedInputStreamBuilder, rollingSigner);
                setupSigTrailer(chunkedEncodedInputStreamBuilder, rollingSigner);
                setupChecksumTrailer(chunkedEncodedInputStreamBuilder);
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }

        return chunkedEncodedInputStreamBuilder::build;
    }

    private void setupSigExt(ChunkedEncodedInputStream.Builder builder, RollingSigner rollingSigner) {
        builder.addExtension(
            chunk -> Pair.of(
                "chunk-signature".getBytes(StandardCharsets.UTF_8),
                rollingSigner.sign(chunk)
            )
        );
    }

    private void setupSigTrailer(ChunkedEncodedInputStream.Builder builder, RollingSigner rollingSigner) {
        Map<String, List<String>> trailers =
            builder.trailers().stream().map(TrailerProvider::get).collect(Collectors.toMap(Pair::left, Pair::right));

        builder.addTrailer(
            () -> Pair.of(
                "x-amz-trailer-signature",
                Collections.singletonList(new String(rollingSigner.sign(trailers), StandardCharsets.UTF_8))
            )
        );
    }

    private void setupChecksumTrailer(ChunkedEncodedInputStream.Builder builder) {
        // TODO(sra-identity-and-auth): Set up checksumming of chunks and add as a trailer
    }
}
