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

package software.amazon.awssdk.services.s3.internal.crt;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public interface S3CrtBorrowedBufferStreamHandler {
    void onBorrowedStreamStart(Runnable cancellationAction);

    /** Returns whether the stream accepted the buffer. Rejected buffers are discarded. */
    boolean onBorrowedBuffer(S3CrtBorrowedBufferLease buffer);

    void onBorrowedStreamComplete();

    /** Reports an error after the reader drains queued buffers. */
    void onBorrowedStreamError(Throwable error);

    /** Discards queued buffers and closes the stream. */
    void onBorrowedStreamAbort();

    /** Discards queued buffers and causes later reads to fail with the error. */
    void onBorrowedStreamAbort(Throwable error);
}
