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

package software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * A functional interface for defining a header of a chunk.
 * <p>
 * The header usually depends on the chunk-data itself (hex-size), but is not required to. In <a
 * href="https://datatracker.ietf.org/doc/html/rfc7230#section-4.1">RFC-7230</a>, the chunk-header is specifically the
 * {@code chunk-size}, but this interface can give us greater flexibility.
 */
@FunctionalInterface
@SdkInternalApi
public interface ChunkHeaderProvider extends Resettable {
    byte[] get(byte[] chunk);
}
