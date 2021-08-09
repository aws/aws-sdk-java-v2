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
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.protocols.json.internal.dom.SdkJsonNode;

/**
 * Interface for unmarshalling a field from a JSON based service.
 *
 * @param <T> Type to unmarshall into.
 */
@SdkInternalApi
public interface JsonUnmarshaller<T> {

    /**
     * @param context Context containing dependencies and unmarshaller registry.
     * @param jsonContent Parsed JSON content of body. May be null for REST JSON based services that don't have payload members.
     * @param field {@link SdkField} of member being unmarshalled.
     * @return Unmarshalled value.
     */
    T unmarshall(JsonUnmarshallerContext context,
                 SdkJsonNode jsonContent,
                 SdkField<T> field);

}
