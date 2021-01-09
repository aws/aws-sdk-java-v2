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

package software.amazon.awssdk.core;

import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Interface to provide the list of {@link SdkField}s in a POJO. {@link SdkField} contains
 * metadata about how a field should be marshalled/unmarshalled and allows for generic
 * accessing/setting/creating of that field on an object.
 */
@SdkProtectedApi
public interface SdkPojo {

    /**
     * @return List of {@link SdkField} in this POJO. May be empty list but should never be null.
     */
    List<SdkField<?>> sdkFields();

    /**
     * Indicates whether some other object is "equal to" this one by SDK fields.
     * An SDK field is a modeled, non-inherited field in an {@link SdkPojo} class,
     * and is generated based on a service model.
     *
     * <p>
     * If an {@link SdkPojo} class does not have any inherited fields, {@code equalsBySdkFields}
     * and {@code equals} are essentially the same.
     *
     * @param other the object to be compared with
     * @return true if the other object equals to this object by sdk fields, false otherwise.
     */
    default boolean equalsBySdkFields(Object other) {
        throw new UnsupportedOperationException();
    }
}
