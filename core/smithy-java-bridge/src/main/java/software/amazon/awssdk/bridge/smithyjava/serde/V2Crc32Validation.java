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

package software.amazon.awssdk.bridge.smithyjava.serde;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.http.Crc32Validation;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.smithy.java.client.core.interceptors.ClientInterceptor;
import software.amazon.smithy.java.client.core.interceptors.ResponseHook;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * v2's legacy {@code x-amz-crc32} response validation (ledger 6.1).
 *
 * <p>DynamoDB returns a CRC32 of the response body in {@code x-amz-crc32}, and stock v2 fails the call with
 * {@code Crc32MismatchException} on a mismatch, without retrying — on the sync client through
 * {@code AwsSyncClientHandler}'s response-handler wrapper, and on the async client too, as
 * {@code DynamoDbBehaviorProbe} measured against published 2.46.10. It is not a trait and not an interceptor,
 * so neither the schema layer nor the interceptor bridge carried it, and the bridge accepted a corrupted
 * response silently.
 *
 * <p>This runs v2's own {@link Crc32Validation#validate} over the response before smithy deserializes it,
 * which wraps the body in v2's validating stream — including v2's handling of a gzip-encoded body and the
 * client's {@code CRC32_FROM_COMPRESSED_DATA_ENABLED} setting. A response without the header passes through
 * untouched, so the interceptor is safe to install on every sync client.
 */
@SdkProtectedApi
public final class V2Crc32Validation implements ClientInterceptor {

    private final boolean calculateFromCompressedData;

    public V2Crc32Validation(boolean calculateFromCompressedData) {
        this.calculateFromCompressedData = calculateFromCompressedData;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <ResponseT> ResponseT modifyBeforeDeserialization(ResponseHook<?, ?, ?, ResponseT> hook) {
        if (!(hook.response() instanceof HttpResponse response) || !response.headers().hasHeader("x-amz-crc32")) {
            return hook.response();
        }
        DataStream body = response.body();
        SdkHttpFullResponse v2 = SdkHttpFullResponse.builder()
                                                    .statusCode(response.statusCode())
                                                    .headers(response.headers().map())
                                                    .content(body == null ? null
                                                                          : AbortableInputStream.create(body.asInputStream()))
                                                    .build();
        SdkHttpFullResponse validated = Crc32Validation.validate(calculateFromCompressedData, v2);

        // validate() may also decompress, which changes the headers v2 hands on; mirror both.
        DataStream validatedBody = validated.content()
                                            .map(in -> DataStream.ofInputStream(in, body == null ? null : body.contentType(), -1))
                                            .orElse(DataStream.ofEmpty());
        return (ResponseT) HttpResponse.of(response.httpVersion(), validated.statusCode(),
                                           HttpHeaders.of(validated.headers()), validatedBody);
    }
}
