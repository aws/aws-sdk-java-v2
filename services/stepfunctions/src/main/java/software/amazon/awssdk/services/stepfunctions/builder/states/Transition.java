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

package software.amazon.awssdk.services.stepfunctions.builder.states;

import com.fasterxml.jackson.annotation.JsonIgnore;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.stepfunctions.builder.internal.Buildable;

/**
 * Represents a transition in the state machine (i.e to another state or termination
 * of the state machine).
 *
 * <p>This interface should not be implemented outside the SDK.</p>
 */
public interface Transition {

    /**
     * No-op builder that always returns null.
     */
    @SdkInternalApi
    Transition.Builder NULL_BUILDER = new Builder() {
        @Override
        public Transition build() {
            return null;
        }
    };

    /**
     * @return True if this transition represents a terminal transition (i.e. one that would cause the state machine to exit).
     *     False if this is a non terminal transition (i.e. to another state in the state machine).
     */
    @JsonIgnore
    boolean isTerminal();

    /**
     * Builder interface for {@link Transition}s.
     */
    interface Builder extends Buildable<Transition> {
    }
}
