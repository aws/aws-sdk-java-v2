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

package software.amazon.awssdk.services.sharedeventstream.model;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkPojo;

/**
 * Base interface for all event types in EventStream.
 */
@Generated("software.amazon.awssdk:codegen")
@SdkPublicApi
public interface EventStream extends SdkPojo {
    /**
     * Special type of {@link EventStream} for unknown types of events that this version of the SDK does not know about
     */
    EventStream UNKNOWN = new EventStream() {
        @Override
        public List<SdkField<?>> sdkFields() {
            return Collections.emptyList();
        }

        @Override
        public void accept(StreamBirthsResponseHandler.Visitor visitor) {
            visitor.visitDefault(this);
        }

        @Override
        public void accept(StreamDeathsResponseHandler.Visitor visitor) {
            visitor.visitDefault(this);
        }
    };

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventStream}.
     *
     * @param visitor Visitor to invoke.
     */
    void accept(StreamBirthsResponseHandler.Visitor visitor);

    /**
     * Calls the appropriate visit method depending on the subtype of {@link EventStream}.
     *
     * @param visitor Visitor to invoke.
     */
    void accept(StreamDeathsResponseHandler.Visitor visitor);
}
