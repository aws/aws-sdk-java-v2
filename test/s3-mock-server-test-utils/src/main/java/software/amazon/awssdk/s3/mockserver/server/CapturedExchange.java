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

package software.amazon.awssdk.s3.mockserver.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CapturedExchange {
    private final CapturedRequest request;
    private final CapturedResponse response;
    private final CapturedRequest manipulatedRequest;
    private final CapturedResponse manipulatedResponse;
    private final long startNanos;
    private final long endNanos;
    private final ExchangeState state;

    public CapturedExchange(CapturedRequest request, CapturedResponse response, CapturedRequest manipulatedRequest,
                            CapturedResponse manipulatedResponse, long startNanos, long endNanos, ExchangeState state) {
        this.request = request;
        this.response = response;
        this.manipulatedRequest = manipulatedRequest;
        this.manipulatedResponse = manipulatedResponse;
        this.startNanos = startNanos;
        this.endNanos = endNanos;
        this.state = state;
    }

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

    public CapturedRequest request() {
        return request;
    }

    public CapturedResponse response() {
        return response;
    }

    public CapturedRequest manipulatedRequest() {
        return manipulatedRequest;
    }

    public CapturedResponse manipulatedResponse() {
        return manipulatedResponse;
    }

    public long startNanos() {
        return startNanos;
    }

    public long endNanos() {
        return endNanos;
    }

    public ExchangeState state() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CapturedExchange that = (CapturedExchange) o;
        return Objects.equals(request, that.request)
                && Objects.equals(response, that.response)
                && Objects.equals(manipulatedRequest, that.manipulatedRequest)
                && Objects.equals(manipulatedResponse, that.manipulatedResponse)
                && startNanos == that.startNanos
                && endNanos == that.endNanos
                && Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(request);
        result = 31 * result + Objects.hashCode(response);
        result = 31 * result + Objects.hashCode(manipulatedRequest);
        result = 31 * result + Objects.hashCode(manipulatedResponse);
        result = 31 * result + Objects.hashCode(startNanos);
        result = 31 * result + Objects.hashCode(endNanos);
        result = 31 * result + Objects.hashCode(state);
        return result;
    }

    @Override
    public String toString() {
        return "CapturedExchange[request=" + request + ", response=" + response
                + ", manipulatedRequest=" + manipulatedRequest + ", manipulatedResponse=" + manipulatedResponse
                + ", startNanos=" + startNanos + ", endNanos=" + endNanos + ", state=" + state + "]";
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
