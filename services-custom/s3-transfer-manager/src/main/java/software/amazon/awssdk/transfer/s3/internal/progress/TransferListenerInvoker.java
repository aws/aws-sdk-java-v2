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

package software.amazon.awssdk.transfer.s3.internal.progress;

import static software.amazon.awssdk.utils.FunctionalUtils.runAndLogError;

import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An SDK-internal helper class that composes multiple provided {@link TransferListener}s together into a single logical chain.
 * Invocations on {@link TransferListenerInvoker} will be delegated to the underlying chain, while suppressing (and logging) any
 * exceptions that are thrown.
 */
@SdkInternalApi
public class TransferListenerInvoker implements TransferListener {
    private static final Logger log = Logger.loggerFor(TransferListener.class);
    private final List<TransferListener> listeners;

    public TransferListenerInvoker(List<TransferListener> listeners) {
        this.listeners = Validate.paramNotNull(listeners, "listeners");
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
        forEach(listener -> listener.transferInitiated(context));
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        forEach(listener -> listener.bytesTransferred(context));
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
        forEach(listener -> listener.transferComplete(context));
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
        forEach(listener -> listener.transferFailed(context));
    }

    private void forEach(Consumer<TransferListener> action) {
        for (TransferListener listener : listeners) {
            runAndLogError(log.logger(), "Exception thrown in TransferListener, ignoring",
                           () -> action.accept(listener));
        }
    }
}
