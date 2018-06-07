/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.kinesis.model;

/**
 * Base interface for all event types of the SubscribeToShard API.
 */
public interface SubscribeToShardBaseEvent {

    /**
     * Special type of {@link SubscribeToShardBaseEvent} for unknown types of events that this version of the SDK does
     * not know about.
     */
    SubscribeToShardBaseEvent UNKNOWN = new SubscribeToShardBaseEvent() {
        @Override
        public void visit(Visitor visitor) {
            visitor.visitDefault(this);
        }
    };

    /**
     * Calls the appropriate visit method depending on the subtype of {@link SubscribeToShardBaseEvent}.
     *
     * @param visitor Visitor to invoke.
     */
    void visit(Visitor visitor);

    /**
     * Visitor for subtypes of {@link SubscribeToShardBaseEvent}.
     */
    abstract class Visitor {

        /**
         * Invoked when a {@link SubscribeToShardEvent} is encountered. If this is not overridden, the event will
         * be given to {@link #visitDefault(SubscribeToShardBaseEvent)}.
         *
         * @param event Event being visited.
         */
        public void visit(SubscribeToShardEvent event) {
            visitDefault(event);
        }

        /**
         * A required "else" or "default" block, invoked with no other more-specific "visit" method is appropriate. This is
         * invoked under two circumstances:
         * <ol>
         * <li>The event encountered is newer than the current version of the SDK, so no other more-specific "visit" method
         * could be called. In this case, the provided event will be a generic {@link SubscribeToShardBaseEvent}. These events
         * can be processed by upgrading the SDK.</li>
         * <li>The event is known by the SDK, but the "visit" was not overridden above. In this case, the provided event will
         * be a specific type of {@link SubscribeToShardBaseEvent}.</li>
         * </ol>
         *
         * @param event The event that was not handled by a more-specific "visit" method.
         */
        public void visitDefault(SubscribeToShardBaseEvent event) {
        }

    }
}
