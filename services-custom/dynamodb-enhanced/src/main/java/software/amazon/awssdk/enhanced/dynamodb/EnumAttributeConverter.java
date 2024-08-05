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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * A converter between an {@link Enum} and {@link AttributeValue}.
 *
 * <p>
 * This stores values in DynamoDB as a string.
 *
 * <p>
 * Use EnumAttributeConverter::create in order to use Enum::toString as the enum identifier
 *
 * <p>
 * Use EnumAttributeConverter::createWithNameAsKeys in order to use Enum::name as the enum identifier
 *
 * <p>
 * This can be created via {@link #create(Class)}.
 */
@SdkPublicApi
public final class EnumAttributeConverter<T extends Enum<T>> implements AttributeConverter<T> {

    private final Class<T> enumClass;
    private final Map<String, T> enumValueMap;

    private final Function<T, String> keyExtractor;

    private EnumAttributeConverter(Class<T> enumClass, Function<T, String> keyExtractor) {
        this.enumClass = enumClass;
        this.keyExtractor = keyExtractor;

        Map<String, T> mutableEnumValueMap = new LinkedHashMap<>();
        Arrays.stream(enumClass.getEnumConstants())
              .forEach(enumConstant -> mutableEnumValueMap.put(keyExtractor.apply(enumConstant), enumConstant));

        this.enumValueMap = Collections.unmodifiableMap(mutableEnumValueMap);
    }

    /**
     * Creates an EnumAttributeConverter for an {@link Enum}.
     *
     * <p>
     * Uses Enum::toString as the enum identifier.
     *
     * @param enumClass The enum class to be used
     * @return an EnumAttributeConverter
     * @param <T> the enum subclass
     */
    public static <T extends Enum<T>> EnumAttributeConverter<T> create(Class<T> enumClass) {
        return new EnumAttributeConverter<>(enumClass, Enum::toString);
    }

    /**
     * Creates an EnumAttributeConverter for an {@link Enum}.
     *
     * <p>
     * Uses Enum::name as the enum identifier.
     *
     * @param enumClass The enum class to be used
     * @return an EnumAttributeConverter
     * @param <T> the enum subclass
     */
    public static <T extends Enum<T>> EnumAttributeConverter<T> createWithNameAsKeys(Class<T> enumClass) {
        return new EnumAttributeConverter<>(enumClass, Enum::name);
    }

    /**
     * Returns the proper {@link AttributeValue} for the given enum type.
     *
     * @param input the enum type to be converted
     * @return AttributeValue
     */
    @Override
    public AttributeValue transformFrom(T input) {
        return AttributeValue.builder().s(keyExtractor.apply(input)).build();
    }

    /**
     * Returns the proper enum type for the given {@link AttributeValue} input.
     *
     * @param input the AttributeValue to be converted
     * @return an enum type
     */
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

    /**
     * Returns the {@link EnhancedType} of the converter.
     *
     * @return EnhancedType
     */
    @Override
    public EnhancedType<T> type() {
        return EnhancedType.of(enumClass);
    }

    /**
     * Returns the {@link AttributeValueType} of the converter.
     *
     * @return AttributeValueType
     */
    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
