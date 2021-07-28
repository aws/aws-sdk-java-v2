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

package software.amazon.awssdk.core.internal.batchutilities;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class CancellableFlush implements Runnable {

    /**
     * isManual is a field used to distinguish a manual flush from a periodically scheduled one. It is set upon calling a
     * manual flush
     */
    private boolean isManual;
    private final Object lock = new Object();
    private final Runnable flushBuffer;
    private volatile boolean isCancelled = false;
    private volatile boolean hasExecuted = false;

    public CancellableFlush(Runnable flushBuffer, boolean isManual) {
        this.flushBuffer = flushBuffer;
        this.isManual = isManual;
    }

    @Override
    public void run() {
        synchronized (this.lock) {
            if (isCancelled) {
                return;
            }
            hasExecuted = true;
            flushBuffer.run();
        }
    }

    public void cancel() {
        synchronized (this.lock) {
            isCancelled = true;
        }
    }

    public boolean hasExecuted() {
        synchronized (this.lock) {
            return hasExecuted;
        }
    }

    public boolean isManual() {
        return isManual;
    }

    public void reset() {
        synchronized (this.lock) {
            isCancelled = false;
            isManual = false;
        }
    }
}
