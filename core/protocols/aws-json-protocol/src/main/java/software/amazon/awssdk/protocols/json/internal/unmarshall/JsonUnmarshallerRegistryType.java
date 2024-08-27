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

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;

/**
 * Represents an unmarshalling registry that resolves an unmarshaller for the given location and marshalling type.
 */
@SdkInternalApi
public interface JsonUnmarshallerRegistryType {

    /**
     * Returns an unmarshaller for the given location and marshalling type.
     */
    <T> JsonUnmarshaller<Object> getUnmarshaller(MarshallLocation marshallLocation, MarshallingType<T> marshallingType);
}
