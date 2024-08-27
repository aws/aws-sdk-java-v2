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

package software.amazon.awssdk.core.traits;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Enumerates all the know traits. This enum is used to use EnumMap's for fast lookup
 * of traits.
 */
@SdkProtectedApi
public enum KnownTraitType {
    DATA_TYPE_CONVERSION_FAILURE_HANDLING_TRAIT(DataTypeConversionFailureHandlingTrait.class),
    DEFAULT_VALUE_TRAIT(DefaultValueTrait.class),
    JSON_VALUE_TRAIT(JsonValueTrait.class),
    LIST_TRAIT(ListTrait.class),
    LOCATION_TRAIT(LocationTrait.class),
    MAP_TRAIT(MapTrait.class),
    PAYLOAD_TRAIT(PayloadTrait.class),
    REQUIRED_TRAIT(RequiredTrait.class),
    TIMESTAMP_FORMAT_TRAIT(TimestampFormatTrait.class),
    XML_ATTRIBUTE_TRAIT(XmlAttributeTrait.class),
    XML_ATTRIBUTES_TRAIT(XmlAttributesTrait.class),
    ;

    private final Class<? extends Trait> typeClass;

    KnownTraitType(Class<? extends Trait> typeClass) {
        this.typeClass = typeClass;
    }

    /**
     * Returns the known type for the given class.
     */
    public static KnownTraitType from(Class<?> clazz) {
        if (clazz == DataTypeConversionFailureHandlingTrait.class) {
            return DATA_TYPE_CONVERSION_FAILURE_HANDLING_TRAIT;
        }
        if (clazz == DefaultValueTrait.class) {
            return DEFAULT_VALUE_TRAIT;
        }
        if (clazz == JsonValueTrait.class) {
            return JSON_VALUE_TRAIT;
        }
        if (clazz == ListTrait.class) {
            return LIST_TRAIT;
        }
        if (clazz == LocationTrait.class) {
            return LOCATION_TRAIT;
        }
        if (clazz == MapTrait.class) {
            return MAP_TRAIT;
        }
        if (clazz == PayloadTrait.class) {
            return PAYLOAD_TRAIT;
        }
        if (clazz == RequiredTrait.class) {
            return REQUIRED_TRAIT;
        }
        if (clazz == TimestampFormatTrait.class) {
            return TIMESTAMP_FORMAT_TRAIT;
        }
        if (clazz == XmlAttributeTrait.class) {
            return XML_ATTRIBUTE_TRAIT;
        }
        if (clazz == XmlAttributesTrait.class) {
            return XML_ATTRIBUTES_TRAIT;
        }
        return null;
    }
}
