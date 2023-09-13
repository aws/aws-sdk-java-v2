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

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Pair;

/**
 * A functional interface for defining a trailer, where the trailer is a header pair.
 * <p>
 * A trailer usually depends on the chunk-data itself (checksum, signature, etc.), but is not required to. Per <a
 * href="https://datatracker.ietf.org/doc/html/rfc7230#section-4.1.2">RFC-7230</a>, the chunked trailer section is defined as:
 * <pre>
 *     trailer-part   = *( header-field CRLF )
 * </pre>
 * An implementation of this interface is specifically an element of the {@code trailer-part}. Therefore, all occurrences of
 * {@code TrailerProvider}'s make up the {@code trailer-part}.
 */
@FunctionalInterface
@SdkInternalApi
public interface TrailerProvider extends Resettable {
    Pair<String, List<String>> get();
}
