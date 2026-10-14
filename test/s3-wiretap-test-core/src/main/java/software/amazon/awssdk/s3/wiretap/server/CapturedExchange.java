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

package software.amazon.awssdk.s3.wiretap.server;

import java.util.ArrayList;
import java.util.List;

public record CapturedExchange(
        CapturedRequest request,
        CapturedResponse response,
        CapturedRequest manipulatedRequest,
        CapturedResponse manipulatedResponse,
        long startNanos,
        long endNanos,
        ExchangeState state) {

    public CapturedExchange(CapturedRequest request, CapturedResponse response,
                            long startNanos, long endNanos) {
        this(request, response, null, null, startNanos, endNanos, ExchangeState.COMPLETE);
    }

    public CapturedExchange(CapturedRequest request, CapturedResponse response,
                            CapturedRequest manipulatedRequest, CapturedResponse manipulatedResponse,
                            long startNanos, long endNanos) {
        this(request, response, manipulatedRequest, manipulatedResponse, startNanos, endNanos,
                ExchangeState.COMPLETE);
    }

    public boolean overlaps(CapturedExchange other) {
        return startNanos < other.endNanos && other.startNanos < endNanos;
    }

    public static int maxConcurrency(List<CapturedExchange> exchanges) {
        List<long[]> events = new ArrayList<>();
        for (CapturedExchange e : exchanges) {
            events.add(new long[]{e.startNanos(), 1});
            events.add(new long[]{e.endNanos(), -1});
        }
        events.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        int open = 0;
        int max = 0;
        for (long[] event : events) {
            open += (int) event[1];
            max = Math.max(max, open);
        }
        return max;
    }

    public boolean settled() {
        return state.terminal();
    }

    public boolean inFlight() {
        return !state.terminal();
    }

    public boolean responseTruncated() {
        return state == ExchangeState.RESPONSE_TRUNCATED;
    }

    public boolean requestAborted() {
        return state == ExchangeState.REQUEST_ABORTED;
    }
}
