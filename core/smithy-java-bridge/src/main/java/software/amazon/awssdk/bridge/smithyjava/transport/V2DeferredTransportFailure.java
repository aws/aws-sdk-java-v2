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

package software.amazon.awssdk.bridge.smithyjava.transport;

import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.java.client.core.CallContext;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.http.api.HttpHeaders;
import software.amazon.smithy.java.http.api.HttpResponse;
import software.amazon.smithy.java.http.api.HttpVersion;
import software.amazon.smithy.java.io.datastream.DataStream;

/**
 * Carries a transport failure into smithy-java's retry loop, which otherwise cannot see it (ledger 3.6).
 *
 * <h2>The defect this works around</h2>
 *
 * <p>smithy-java 1.6.1 decides retries inside {@code ClientPipeline.deserialize}. A failure thrown from
 * {@code ClientTransport.send} — a refused connection, a reset, a TLS failure, an attempt timeout — leaves
 * the attempt before deserialization starts, so nothing ever asks the retry strategy about it, and it is
 * never retried, whatever its type or declared retry safety. v2 retries all of those.
 *
 * <h2>The workaround</h2>
 *
 * <p>A bridged transport does not throw. It records the failure in the call context, keyed to the attempt,
 * and returns a marked stand-in response ({@link #MARKER}); deserialization turns that into an error as it
 * would any unsuccessful response, which puts the attempt through {@code modifyBeforeAttemptCompletion} and
 * the retry decision. {@code V2ErrorEnricher}, which runs there, recognizes the marker, replaces the error with
 * the recorded failure, and classifies it as v2 would. Nothing else sees the stand-in: the response-side
 * bridges skip it, and the caller only ever sees the original exception.
 *
 * <p>It is a workaround and reads like one. The fix belongs in {@code ClientPipeline} — make the transport
 * failure part of the attempt the retry strategy is asked about — and this class is the argument that the
 * change is small: it makes the bridged pipeline retry transport failures exactly where v2 does, with the
 * existing retry strategy, by moving one throw.
 */
@SdkProtectedApi
public final class V2DeferredTransportFailure {

    /** Header on the stand-in response; its only purpose is to be recognized. */
    public static final String MARKER = "x-smithy-bridge-deferred-transport-failure";

    /** Whether the client installed the reader of deferred failures; without it, transports throw as before. */
    public static final Context.Key<Boolean> ENABLED = Context.key("v2 deferred transport failures");

    private static final Context.Key<Deferred> KEY = Context.key("v2 deferred transport failure");

    /**
     * Status of the stand-in: an error status, so the protocol treats it as a failed response, and one no
     * service sends, so nothing mistakes it for a real one if the marker is ever lost.
     */
    private static final int STATUS = 599;

    private V2DeferredTransportFailure() {
    }

    /**
     * Records {@code failure} for the current attempt and returns the stand-in, or rethrows when deferral
     * is not enabled on this client.
     */
    static HttpResponse defer(Context context, RuntimeException failure) {
        if (!Boolean.TRUE.equals(context.get(ENABLED))) {
            throw asTransportContract(failure);
        }
        try {
            context.put(KEY, new Deferred(attempt(context), failure));
        } catch (UnsupportedOperationException readOnly) {
            throw asTransportContract(failure);
        }
        return HttpResponse.of(HttpVersion.HTTP_1_1, STATUS,
                               HttpHeaders.of(Map.of(MARKER, List.of("1"))), DataStream.ofEmpty());
    }

    // A transport may only throw CallException subtypes; an attempt timeout is a v2 SdkException.
    private static RuntimeException asTransportContract(RuntimeException failure) {
        return failure instanceof software.amazon.smithy.java.core.error.CallException
               ? failure
               : software.amazon.smithy.java.client.core.ClientTransport.remapExceptions(failure);
    }

    /** Whether a response is the stand-in, for the response-side bridges to leave alone. */
    public static boolean isStandIn(Object response) {
        return response instanceof HttpResponse http && http.headers().hasHeader(MARKER);
    }

    /** The failure the stand-in stands for, if the current attempt produced one. */
    public static RuntimeException deferredFailure(Context context) {
        Deferred deferred = context.get(KEY);
        return deferred != null && deferred.attempt == attempt(context) ? deferred.failure : null;
    }

    private static int attempt(Context context) {
        Integer attempt = context.get(CallContext.RETRY_ATTEMPT);
        return attempt == null ? 0 : attempt;
    }

    private record Deferred(int attempt, RuntimeException failure) {
    }
}
