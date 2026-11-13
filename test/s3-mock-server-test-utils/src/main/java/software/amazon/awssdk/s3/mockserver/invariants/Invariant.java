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

package software.amazon.awssdk.s3.mockserver.invariants;

public enum Invariant {

    STORED_CONTENT_MATCHES_SOURCE(1, "the stored object equals the source the test supplied"),

    ATTEMPTS_CARRY_THE_SAME_BYTES(2, "every attempt of one request carries the same body bytes"),

    BODY_LENGTH_MATCHES_DECLARED(3, "the body on the wire is as long as the length the request declared"),

    COMPLETED_PARTS_ARE_CONTIGUOUS(4,
            "CompleteMultipartUpload lists parts 1..N once each, each the part sent under that number"),

    SUBSCRIBER_SIGNALS_ARE_SERIAL(5, "a subscriber's signals do not overlap (Reactive Streams 1.3)"),

    ONE_READER_PER_STREAM(6, "at most one thread at a time is inside a caller-supplied InputStream"),

    DESTINATION_HOLDS_ONE_VERSION(7, "a download destination holds a contiguous range of one served version"),

    CALLER_FUTURE_COMPLETES_WITH_A_VALUE(8,
            "every future the client returned to the test is complete when the test returns, unchanged since it"
                    + " completed, and a normal completion carries a non-null value"),

    NO_REQUEST_AFTER_THE_CALLER_FUTURE(9,
            "no request other than AbortMultipartUpload starts after the last future the client returned to the"
                    + " test completed"),

    CLIENT_THREADS_END_AT_CLOSE(10, "once the client is closed its threads end and no request starts");

    private final int number;

    private final String statement;

    Invariant(int number, String statement) {
        this.number = number;
        this.statement = statement;
    }

    public int number() {
        return number;
    }

    public String statement() {
        return statement;
    }

    public String label() {
        return "invariant " + number + " (" + statement + ")";
    }
}
