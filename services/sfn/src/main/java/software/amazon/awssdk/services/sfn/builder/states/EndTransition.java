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

package software.amazon.awssdk.services.sfn.builder.states;

import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.sfn.builder.internal.PropertyName;

/**
 * Terminal transition that indicates the state machine should terminate.
 *
 * @see <a href="https://states-language.net/spec.html#transition">https://states-language.net/spec.html#transition</a>
 */
@SdkPublicApi
public final class EndTransition implements Transition {

    // Required for proper serialization
    @JsonProperty(PropertyName.END)
    private final boolean end = true;

    private EndTransition() {
    }

    /**
     * @return Builder instance to construct a {@link EndTransition}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    /**
     * Builder for {@link EndTransition}
     */
    public static final class Builder implements Transition.Builder {

        private Builder() {
        }

        /**
         * @return An immutable {@link EndTransition} object.
         */
        public EndTransition build() {
            return new EndTransition();
        }
    }
}
