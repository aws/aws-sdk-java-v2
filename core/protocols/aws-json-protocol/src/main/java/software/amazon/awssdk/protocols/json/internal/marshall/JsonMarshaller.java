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

package software.amazon.awssdk.protocols.json.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.protocols.core.Marshaller;

/**
 * Interface to marshall data according to the JSON protocol specification.
 *
 * @param <T> Type to marshall.
 */
@FunctionalInterface
@SdkInternalApi
public interface JsonMarshaller<T> extends Marshaller<T> {

    JsonMarshaller<Void> NULL = (val, context, paramName, sdkField) -> { };

    /**
     * Marshall the data into the request.
     *
     * @param val       Data to marshall (may be null).
     * @param context   Dependencies needed for marshalling.
     * @param paramName Optional param/field name. May be null in certain situations.
     */
    void marshall(T val, JsonMarshallerContext context, String paramName, SdkField<T> sdkField);

}
