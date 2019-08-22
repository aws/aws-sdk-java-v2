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

import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Generic handler to handle {@link ProgressEvent}.
 */
@SdkPublicApi
@FunctionalInterface
public interface ProgressEventHandler {

    /**
     * Handles default {@link ProgressEvent}
     *
     * @param progressEvent the progress event
     * @return the future containing the {@link ProgressEventResult}
     */
    CompletableFuture<? extends ProgressEventResult> onDefault(ProgressEvent progressEvent);


    /**
     * Event handler to handle {@link ByteCountEvent}.
     */
    @FunctionalInterface
    interface ByteCountEventHandler {

        /**
         * Handles a {@link ByteCountEvent}
         *
         * @param progressEvent the progress event
         * @return the future containing the {@link ProgressEventResult}
         */
        CompletableFuture<? extends ProgressEventResult> onByteCountEvent(ByteCountEvent progressEvent);
    }

    /**
     * Event handler to handle {@link RequestCycleEvent}.
     */
    @FunctionalInterface
    interface RequestLifeCycleEventHandler {

        /**
         * Handles a {@link RequestCycleEvent}
         *
         * @param progressEvent the progress event
         * @return the future containing the {@link ProgressEventResult}
         */
        CompletableFuture<? extends ProgressEventResult> onRequestLifeCycleEvent(RequestCycleEvent progressEvent);
    }
}
