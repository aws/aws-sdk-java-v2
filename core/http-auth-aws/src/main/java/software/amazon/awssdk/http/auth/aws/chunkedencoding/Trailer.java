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

package software.amazon.awssdk.http.auth.aws.chunkedencoding;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Pair;

/**
 * A functional interface for defining a trailer, where the trailer is a key-value pair.
 * <p>
 * The trailer usually depends on the chunk-data itself (checksum, signature, etc.), but is not required to.
 * <p>
 * Trailers are optionally included in the final chunk of a chunk-encoded stream.
 */
@FunctionalInterface
@SdkProtectedApi
public interface Trailer {
    Pair<byte[], byte[]> get(byte[] chunk);
}
