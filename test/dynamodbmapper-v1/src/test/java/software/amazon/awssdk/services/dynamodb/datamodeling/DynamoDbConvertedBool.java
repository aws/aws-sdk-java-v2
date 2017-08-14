/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to convert a {@link Boolean} to the DynamoDB {@code S} type.
 *
 * <pre class="brush: java">
 * &#064;DynamoDBConvertedBool(DynamoDBConvertedBool.Format.Y_N)
 * public boolean isTesting()
 * </pre>
 *
 * <p>The standard V1 and V2 compatible conversion schemas will, by default,
 * serialize booleans using the DynamoDB {@code N} type, with a value of '1'
 * representing 'true' and a value of '0' representing 'false'. To force the
 * {@code N} conversion in other schemas,
 * <pre class="brush: java">
 * &#064;DynamoDBTyped(DynamoDBAttributeType.N)
 * public boolean isTesting()
 * </pre>
 *
 * <p>The standard V2 conversion schema will by default serialize booleans
 * natively using the DynamoDB {@code BOOL} type. To force the native
 * {@code BOOL} conversion in other schemas,
 * <pre class="brush: java">
 * &#064;DynamoDBTyped(DynamoDBAttributeType.BOOL)
 * public boolean isTesting()
 * </pre>
 *
 * <p>May be used as a meta-annotation.</p>
 */
@DynamoDb
@DynamoDbTypeConverted(converter = DynamoDbConvertedBool.Converter.class)
@DynamoDbTyped(DynamoDbMapperFieldModel.DynamoDbAttributeType.S)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface DynamoDbConvertedBool {

    /**
     * The format type for converting to and from {@link String}.
     */
    Format value();



    /**
     * Enumeration of the supported format options.
     */
    enum Format {
        true_false, T_F, Y_N
    }

    /**
     * Boolean type converter.
     */
    final class Converter implements DynamoDbTypeConverter<String, Boolean> {
        private final String valueTrue;
        private final String valueFalse;

        Converter(Class<Boolean> targetType, DynamoDbConvertedBool annotation) {
            this.valueTrue = annotation.value().name().split("_")[0];
            this.valueFalse = annotation.value().name().split("_")[1];
        }

        @Override
        public String convert(final Boolean object) {
            return Boolean.TRUE.equals(object) ? valueTrue : valueFalse;
        }

        @Override
        public Boolean unconvert(final String object) {
            return valueTrue.equals(object) ? Boolean.TRUE : Boolean.FALSE;
        }
    }

}
