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

package software.amazon.awssdk.bridge.smithyjava.error;

import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.smithy.java.core.error.CallException;
import software.amazon.smithy.java.core.error.ErrorFault;
import software.amazon.smithy.java.retries.api.RetrySafety;

/**
 * Carries a failure that must not be retried because the request body cannot be sent twice.
 *
 * <p>The mirror image of {@link V2RetryableError}. smithy-java already knows not to retry a call whose
 * body is a one-shot stream — {@code ClientCall.isRetryDisallowed} checks the body's
 * {@code isReplayable()} — but it only looks at the operation's <em>modeled</em> input stream member. The
 * bridge cannot put the body there, because v2's generated shapes have no member for it, so it travels in
 * the call context instead ({@code V2StreamingBridge.REQUEST_BODY}) where that check never looks. Left
 * alone, a retryable failure on a one-shot body is retried, the body is subscribed a second time, and the
 * caller gets an error about multiple subscribers instead of the failure that actually happened
 * ({@code compatability_issues.md} section 14.3).
 *
 * <p>{@link V2ErrorEnricher} substitutes this for the attempt's error when the body is not replayable,
 * which makes the retry strategy decline; the client boundary unwraps {@link #original()}, so the caller
 * sees the real failure.
 */
@SdkProtectedApi
public final class V2NonReplayableError extends CallException {

    private final transient RuntimeException original;

    /**
     * @param original the attempt's failure, as it would otherwise have been handed to the retry strategy.
     */
    public V2NonReplayableError(RuntimeException original) {
        super(original.getMessage(), original, ErrorFault.CLIENT);
        this.original = original;
        isRetrySafe(RetrySafety.NO);
    }

    /** The failure this was substituted for; what a v2 caller should ultimately see. */
    public RuntimeException original() {
        return original;
    }

    @Override
    public RetrySafety isRetrySafe() {
        // A constant, for the same reason V2RetryableError's is: the answer must not depend on a field some
        // later hook could set.
        return RetrySafety.NO;
    }
}
