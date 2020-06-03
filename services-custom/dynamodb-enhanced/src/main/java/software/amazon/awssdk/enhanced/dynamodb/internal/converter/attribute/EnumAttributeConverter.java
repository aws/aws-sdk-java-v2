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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between an {@link Enum} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * This can be created via {@link #create(Class)}.
 */
@SdkInternalApi
public class EnumAttributeConverter<T extends Enum<T>> implements AttributeConverter<T> {

    private final Class<T> enumClass;
    private final Map<String, T> enumValueMap;

    private EnumAttributeConverter(Class<T> enumClass) {
        this.enumClass = enumClass;

        Map<String, T> mutableEnumValueMap = new LinkedHashMap<>();
        Arrays.stream(enumClass.getEnumConstants())
              .forEach(enumConstant -> mutableEnumValueMap.put(enumConstant.toString(), enumConstant));

        this.enumValueMap = Collections.unmodifiableMap(mutableEnumValueMap);
    }

    public static <T extends Enum<T>> EnumAttributeConverter<T> create(Class<T> enumClass) {
        return new EnumAttributeConverter<>(enumClass);
    }

    @Override
    public AttributeValue transformFrom(T input) {
        return AttributeValue.builder().s(input.toString()).build();
    }

    @Override
    public T transformTo(AttributeValue input) {
        Validate.isTrue(input.s() != null, "Cannot convert non-string value to enum.");
        T returnValue = enumValueMap.get(input.s());

        if (returnValue == null) {
            throw new IllegalArgumentException(String.format("Unable to convert string value '%s' to enum type '%s'",
                                                             input.s(), enumClass));
        }

        return returnValue;
    }

    @Override
    public EnhancedType<T> type() {
        return EnhancedType.of(enumClass);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
