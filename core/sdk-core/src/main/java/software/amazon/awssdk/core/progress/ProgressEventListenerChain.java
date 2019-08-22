/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.progress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;


/**
 * An implementation of ProgressListener interface that dipatch
 * progressEvents to multiple listeners.
 */
@SdkInternalApi
public final class ProgressEventListenerChain implements ProgressEventListener {
    private static final Logger log = Logger.loggerFor(ProgressEventListenerChain.class);

    private final List<ProgressEventListener> listeners;

    public ProgressEventListenerChain(Collection<ProgressEventListener> listeners) {
        this.listeners = new ArrayList<>(Validate.paramNotNull(listeners, "listeners"));
    }

    @Override
    public CompletableFuture<? extends ProgressEventResult> onProgressEvent(software.amazon.awssdk.core.progress.ProgressEvent progressEvent) {
        log.debug(() -> "dispatching event " + progressEvent.eventType());
        CompletableFuture[] completableFutures =
            listeners.stream()
                     .filter(l -> l.isSupported(progressEvent))
                     .map(l -> l.onProgressEvent(progressEvent))
                     .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(completableFutures).thenApply(ignore -> ProgressEventResult.empty());
    }
}
