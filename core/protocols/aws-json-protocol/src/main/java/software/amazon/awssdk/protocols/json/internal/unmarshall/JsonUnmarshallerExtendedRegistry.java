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

import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;

/**
 * An extended registry that allows us to reuse the common unmarshallers instead of having to recreate it every time.
 */
@SdkInternalApi
final class JsonUnmarshallerExtendedRegistry implements JsonUnmarshallerRegistryType {

    private final JsonUnmarshallerRegistry delgate;
    private final JsonUnmarshaller<Instant> headerInstantUnmarshaller;
    private final JsonUnmarshaller<Instant> payloadInstantUnmarshaller;


    JsonUnmarshallerExtendedRegistry(
        JsonUnmarshallerRegistry delgate,
        JsonUnmarshaller<Instant> headerInstantUnmarshaller,
        JsonUnmarshaller<Instant> payloadInstantUnmarshaller
    ) {
        this.delgate = delgate;
        this.headerInstantUnmarshaller = headerInstantUnmarshaller;
        this.payloadInstantUnmarshaller = payloadInstantUnmarshaller;
    }

    public <T> JsonUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType) {
        // Instant is special, treating it that way allow us saving on having to recreate the full registry for each
        // context.
        if (marshallingType == MarshallingType.INSTANT) {
            if (marshallLocation == MarshallLocation.PAYLOAD) {
                return (JsonUnmarshaller<Object>) ((Object) payloadInstantUnmarshaller);
            }
            if (marshallLocation == MarshallLocation.HEADER) {
                return (JsonUnmarshaller<Object>) ((Object) headerInstantUnmarshaller);
            }
        }
        return delgate.getUnmarshaller(marshallLocation, marshallingType);
    }

}
