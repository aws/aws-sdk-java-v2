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

package software.amazon.awssdk.core.internal.progress.listener;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.core.progress.listener.ProgressListener;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * ProgressListenerInvoker class exposes thread safe methods that facilitate invocation of all ProgressListener methods
 * corresponding to different stages of the request lifecycle
 */
@SdkInternalApi
@ThreadSafe
public class ProgressListenerInvoker implements ProgressListener {

    private static final Logger log = Logger.loggerFor(ProgressListenerInvoker.class);

    private final List<ProgressListener> listeners;
    private final AtomicBoolean prepared = new AtomicBoolean();
    private final AtomicBoolean headerSent = new AtomicBoolean();
    private final AtomicBoolean headerReceived = new AtomicBoolean();
    private final AtomicBoolean complete = new AtomicBoolean();

    public ProgressListenerInvoker(List<ProgressListener> listeners) {
        this.listeners = Validate.paramNotNull(listeners, "listeners");
    }

    @Override
    public void requestPrepared(Context.RequestPrepared context) {
        if (!prepared.getAndSet(true)) {
            forEach(listener -> listener.requestPrepared(context));
        }
    }

    @Override
    public void requestHeaderSent(Context.RequestHeaderSent context) {
        if (!headerSent.getAndSet(true)) {
            forEach(listener -> listener.requestHeaderSent(context));
        }
    }

    @Override
    public void requestBytesSent(Context.RequestBytesSent context) {
        forEach(listener -> listener.requestBytesSent(context));
    }

    @Override
    public void responseHeaderReceived(Context.ResponseHeaderReceived context) {
        if (!headerReceived.getAndSet(true)) {
            forEach(listener -> listener.responseHeaderReceived(context));
        }
    }

    @Override
    public void responseBytesReceived(Context.ResponseBytesReceived context) {
        forEach(listener -> listener.responseBytesReceived(context));
    }

    @Override
    public void attemptFailure(Context.AttemptFailure context) {
        forEach(listener -> listener.attemptFailure(context));
    }

    @Override
    public void attemptFailureResponseBytesReceived(Context.AttemptFailureResponseBytesReceived context) {
        forEach(listener -> listener.attemptFailureResponseBytesReceived(context));
    }

    @Override
    public void executionFailure(Context.ExecutionFailure context) {
        if (!complete.getAndSet(true)) {
            forEach(listener -> listener.executionFailure(context));
        }
    }

    @Override
    public void executionSuccess(Context.ExecutionSuccess context) {
        if (!complete.getAndSet(true)) {
            forEach(listener -> listener.executionSuccess(context));
        }
    }

    private void forEach(Consumer<ProgressListener> action) {
        for (ProgressListener listener : listeners) {
            runAndLogError(log.logger(), "Exception thrown in ProgressListener, ignoring",
                           () -> action.accept(listener));
        }
    }
}
