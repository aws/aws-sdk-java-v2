/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocol.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.protocol.asserts.marshalling.MarshallingAssertion;
import software.amazon.awssdk.protocol.asserts.marshalling.SerializedAs;
import software.amazon.awssdk.protocol.asserts.unmarshalling.UnmarshalledResultAssertion;
import software.amazon.awssdk.protocol.asserts.unmarshalling.UnmarshallingAssertion;

public class Then {

    private final MarshallingAssertion serializedAs;
    private final UnmarshallingAssertion deserializedAs;

    @JsonCreator
    public Then(@JsonProperty("serializedAs") SerializedAs serializedAs,
                @JsonProperty("deserializedAs") JsonNode deserializedAs) {
        this.serializedAs = serializedAs;
        this.deserializedAs = new UnmarshalledResultAssertion(deserializedAs);
    }

    /**
     * @return The assertion object to use for marshalling tests
     */
    public MarshallingAssertion getMarshallingAssertion() {
        return serializedAs;
    }

    /**
     * @return The assertion object to use for unmarshalling tests
     */
    public UnmarshallingAssertion getUnmarshallingAssertion() {
        return deserializedAs;
    }

}
