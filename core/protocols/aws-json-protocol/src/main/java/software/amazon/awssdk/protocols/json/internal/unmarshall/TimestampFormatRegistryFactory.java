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
import software.amazon.awssdk.core.traits.TimestampFormatTrait;

/**
 * Interface used to create an {@link DefaultJsonUnmarshallerRegistry} that will unmarshall using the given default formats for
 * each of the locations in the formats map.
 */
@SdkInternalApi
public interface TimestampFormatRegistryFactory {

    /**
     * Returns a unmarshaller registry that uses the default timestamp formats passed in.
     */
    JsonUnmarshallerRegistry get(Map<MarshallLocation, TimestampFormatTrait.Format> formats);
}
