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

package software.amazon.awssdk.mapper.dynamodb;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import software.amazon.awssdk.annotations.SdkPreviewApi;

/**
 * A simple JSON converter that uses the Jackson JSON processor.
 *
 * <p>It shares all limitations of that library. For more information about
 * Jackson, see: http://wiki.fasterxml.com/JacksonHome</p>
 *
 * <p>jackson-databind is not a dependency of the mapper. Applications that use this annotation must add
 * {@code com.fasterxml.jackson.core:jackson-databind} 2.x to their classpath. Without it, the mapper throws a
 * {@link DynamoDBMappingException} when it builds the model for a class that uses this annotation.</p>
 *
 * {@snippet :
 * @DynamoDBTypeConvertedJson
 * public Currency getCurrency() { return currency; }
 * }
 *
 * <p>Where,</p>
 * {@snippet :
 * public class Currency {
 *     private Double amount;
 *     private String unit;
 *
 *     public Double getAmount() { return amount; }
 *     public void setAmount(Double amount) { this.amount = amount; }
 *
 *     public String getUnit() { return unit; }
 *     public void setUnit(String unit) { this.unit = unit; }
 * }
 * }
 *
 * <p>Would write the following value to DynamoDB given,</p>
 * <ul>
 *     <li><code>Currency(79.99,"USD")</code> = <code> "{\"amount\":79.99,\"unit\":\"USD\"}"</code></li>
 * </ul>
 *
 * @see software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConverted
 */
@SdkPreviewApi
@DynamoDBTypeConverted(converter=DynamoDBTypeConvertedJson.Converter.class)
@DynamoDBTyped(DynamoDBMapperFieldModel.DynamoDBAttributeType.S)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDBTypeConvertedJson {

    /**
     * The value type to use when calling the JSON mapper's {@code readValue};
     * a value of {@code Void.class} indicates to use the getter's type.
     */
    Class<? extends Object> targetType() default void.class;

    /**
     * JSON type converter.
     */
    final class Converter<T> implements DynamoDBTypeConverter<String,T> {
        private final Class<T> targetType;

        public Converter(Class<T> targetType, DynamoDBTypeConvertedJson annotation) {
            this.targetType = annotation.targetType() == void.class ? targetType : (Class<T>)annotation.targetType();
            JacksonDatabindSupport.initialize();
        }

        @Override
        public final String convert(final T object) {
            try {
                return JacksonDatabindSupport.writeValueAsString(object);
            } catch (final Exception e) {
                throw new DynamoDBMappingException("Unable to write object to JSON", e);
            }
        }

        @Override
        public final T unconvert(final String object) {
            try {
                return JacksonDatabindSupport.readValue(object, targetType);
            } catch (final Exception e) {
                throw new DynamoDBMappingException("Unable to read JSON string", e);
            }
        }
    }

}
