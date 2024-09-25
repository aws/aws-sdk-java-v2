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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.core.traits.TimestampFormatTrait;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.protocols.jsoncore.JsonValueNodeFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;

@SdkInternalApi
public interface ProtocolUnmarshallDependencies {

    /**
     * Used for unmarshalling. This registry is used to lookup an unmarshaller for a given location and marshalling type.
     * @see JsonUnmarshaller
     * @see JsonUnmarshallerContext#getUnmarshaller(MarshallLocation, MarshallingType)
     */
    JsonUnmarshallerRegistry jsonUnmarshallerRegistry();

    /**
     * Used for parsing. This factory knows how to convert the state of the parser into {@link JsonNode} instances that are
     * used during unmarshalling.
     */
    JsonValueNodeFactory nodeValueFactory();

    /**
     * Used to expose this data through the interface.
     */
    Map<MarshallLocation, TimestampFormatTrait.Format> timestampFormats();

    /**
     * Used to parse JSON using Jackson.
     */
    JsonFactory jsonFactory();
}
