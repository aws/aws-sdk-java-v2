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

package software.amazon.awssdk.bridge.smithyjava.client;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.interceptor.ExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;

/**
 * The per-operation model metadata that v2 keeps outside the shape, carried on the generated operation.
 *
 * <h2>Why operation traits need a carrier of their own</h2>
 *
 * <p>C2J records a handful of operation-level traits that decide wire behavior — {@code httpChecksum}
 * (which checksum, whether it is required, which member names the algorithm, whether the response is
 * validated), {@code httpChecksumRequired}, and {@code requestCompression}. Stock v2 does not put any of
 * them on a shape. The generated client method writes them into the call's {@link ExecutionAttributes}
 * ({@code SdkInternalExecutionAttribute.HTTP_CHECKSUM} and friends) as it builds
 * {@code ClientExecutionParams}, reading request members where the trait names one, and the pipeline
 * stages and the signer read them back from there.
 *
 * <p>The smithy path skipped that method body, so none of it reached anything: the generated operation
 * schema carried only {@code @http}. The obvious fix, putting the smithy equivalents on the operation
 * {@code Schema}, does not work in smithy-java 1.6.1 for the one that matters: its only checksum consumer,
 * {@code HttpChecksumPlugin}, implements {@code @httpChecksumRequired} as {@code Content-MD5} and nothing
 * else — no flexible algorithms, no trailers, no response validation. And the rest of v2's behavior lives
 * in v2's signer, which reads v2's attributes.
 *
 * <p>So generated operations implement this interface and write <em>exactly</em> what the stock client
 * method writes, using the same code generators ({@code HttpChecksumTrait}, {@code HttpChecksumRequiredTrait},
 * {@code RequestCompressionTrait}). {@code V2EndpointResolverBridge} calls it once per attempt, into the
 * same attributes it hands the v2 rules engine, and {@code V2SignerBridge} signs with v2's signer from
 * there. The metadata is v2's, the logic that reads it is v2's, and the only bridge-owned part is the call.
 *
 * @param <I> the operation's input shape.
 */
@SdkProtectedApi
public interface V2OperationMetadata<I> {

    /**
     * Writes this operation's model-derived execution attributes for one call.
     *
     * @param input      the call's input; some traits read a member of it (the checksum algorithm, the
     *                   response validation mode).
     * @param attributes where to write them.
     */
    void putExecutionAttributes(I input, Attributes attributes);

    /**
     * A fluent writer with the method name the v2 code generators emit.
     *
     * <p>The generators produce {@code .putExecutionAttribute(KEY, value)} chains meant for
     * {@code ClientExecutionParams}; this lets the same generated fragment be appended to an
     * {@code attributes} expression unchanged, so the bridge cannot drift from what stock v2 writes.
     */
    final class Attributes {
        private final ExecutionAttributes target;

        public Attributes(ExecutionAttributes target) {
            this.target = target;
        }

        public <T> Attributes putExecutionAttribute(ExecutionAttribute<T> key, T value) {
            target.putAttribute(key, value);
            return this;
        }
    }
}
