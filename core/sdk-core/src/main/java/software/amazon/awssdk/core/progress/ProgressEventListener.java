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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.progress.AbstractProgressEventListenerBuilder.DefaultProgressEventListenerBuilder;
import software.amazon.awssdk.core.progress.ProgressEventHandler.ByteCountEventHandler;
import software.amazon.awssdk.core.progress.ProgressEventHandler.RequestLifeCycleEventHandler;

/**
 * Listener interface to listen to the progress events.
 *
 * <T> Type of progress event
 *
 * @see ProgressEvent the progress event
 */
@SdkPublicApi
public interface ProgressEventListener extends ProgressEventHandler, ByteCountEventHandler, RequestLifeCycleEventHandler {

    Set<Class<? extends ProgressEvent>> SUPPORTED_TYPES =
        Collections.unmodifiableSet(new HashSet<>(Arrays.asList(RequestCycleEvent.class, ByteCountEvent.class)));

    /**
     * Check if the provided event is supported by this listener or not.
     *
     * <p>
     * Sub classes can override {@link #extendedTypes()} to provided additional supported {@link ProgressEvent}.
     *
     * @param event the event
     * @return true if the event is supported by this listener, otherwise false.
     */
    default boolean isSupported(ProgressEvent event) {
        return SUPPORTED_TYPES.contains(event.getClass()) || extendedTypes().contains(event.getClass());
    }

    /**
     * Data notification when a {@link ProgressEvent} is available.
     *
     * @param progressEvent a progress event
     * @return the future containing the {@link ProgressEventResult}
     */
    default CompletableFuture<? extends ProgressEventResult> onProgressEvent(ProgressEvent progressEvent) {

        ProgressEventType progressEventType = progressEvent.eventType();

        if (progressEventType instanceof ByteCountEventType) {
            return onByteCountEvent((ByteCountEvent) progressEvent);
        }

        if (progressEventType instanceof RequestCycleEventType) {
            return onRequestLifeCycleEvent((RequestCycleEvent) progressEvent);
        }

        return onExtendedEvents(progressEvent);
    }

    @Override
    default CompletableFuture<? extends ProgressEventResult> onRequestLifeCycleEvent(RequestCycleEvent progressEvent) {
        return onDefault(progressEvent);
    }

    @Override
    default CompletableFuture<? extends ProgressEventResult> onByteCountEvent(ByteCountEvent progressEvent) {
        return onDefault(progressEvent);
    }

    @Override
    default CompletableFuture<? extends ProgressEventResult> onDefault(ProgressEvent progressEvent) {
        return CompletableFuture.completedFuture(ProgressEventResult.empty());
    }

    /**
     * Can be overridden by sub interfaces to provide additional supported types
     *
     * @return the future containing the progress event result.
     */
    default Set<Class<? extends ProgressEvent>> extendedTypes() {
        return Collections.EMPTY_SET;
    }

    /**
     * Can be overridden by sub interfaces to provide notifications on extended events
     *
     * @return the future containing the progress event result.
     */
    default CompletableFuture<? extends ProgressEventResult> onExtendedEvents(ProgressEvent progressEvent) {
        return CompletableFuture.completedFuture(ProgressEventResult.empty());
    }

    static Builder builder() {
        return new DefaultProgressEventListenerBuilder();
    }

    interface Builder<B extends Builder> {
        B onDefault(ProgressEventHandler progressEvent);

        B onByteCountEvent(ByteCountEventHandler transferEventHandler);

        B onRequestLifeCycleEvent(RequestLifeCycleEventHandler requestLifeCycleEventVisitor);

        ProgressEventListener build();
    }
}
