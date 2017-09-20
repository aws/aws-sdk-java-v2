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
import java.util.Set;

/**
 * Annotation to mark a property as using a custom marshaller. This is required
 * when storing anything other than {@link String}s, {@link Number}s, and
 * {@link Set}s of the same to DynamoDB. Any object that can be converted into a
 * String representation and vice versa can be saved in this manner. This
 * annotation can be applied to either the getter method or the class field for
 * the specific property. If the annotation is applied directly to the class
 * field, the corresponding getter and setter must be declared in the same
 * class.
 *
 * @see DynamoDbMarshaller
 * @see JsonMarshaller
 *
 * @deprecated Replaced by {@link DynamoDbTypeConverted}
 *
 * <p>A {@link DynamoDbTypeConverted} with {@link String} as source would
 * perform the same conversion. Please consider, if your marshaller is thread
 * safe before replacing. In the new implementation, a single instance of
 * {@link DynamoDbTypeConverted} is created per field/attribute. In the old,
 * an new instance of the marshaller was created for each call to
 * {@code marshall} and {@code unmarshall}. If your marshaller/converter is not
 * thread safe, it is recomended to specify a converter which will instantiate
 * a new marshaller per call.</p>
 *
 * <pre class="brush: java">
 * public class CustomConverter&lt;T&gt; implements DynamoDBTypeConverter&lt;String,T&gt; {
 *     &#064;Override
 *     public final String convert(final T object) {
 *         return ...
 *     }
 *     &#064;Override
 *     public final T unconvert(final String object) {
 *         return ...
 *     }
 * }
 * </pre>
 */
@Deprecated
@DynamoDb
@DynamoDbTypeConverted(converter = DynamoDbMarshalling.Converter.class)
@DynamoDbTyped(DynamoDbMapperFieldModel.DynamoDbAttributeType.S)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface DynamoDbMarshalling {

    /**
     * The class of the Marshaller that converts this property to and from a
     * String.
     */
    Class<? extends DynamoDbMarshaller<? extends Object>> marshallerClass();

    /**
     * Marshalling type converter.
     */
    final class Converter<T> implements DynamoDbTypeConverter<String, T> {
        private final Class<DynamoDbMarshaller<T>> marshallerClass;
        private final Class<T> targetType;

        Converter(final Class<T> targetType, final DynamoDbMarshalling annotation) {
            this.marshallerClass = (Class<DynamoDbMarshaller<T>>) annotation.marshallerClass();
            this.targetType = targetType;
        }

        @Override
        public String convert(final T object) {
            return marshaller().marshall(object);
        }

        @Override
        public T unconvert(final String object) {
            return marshaller().unmarshall(targetType, object);
        }

        private DynamoDbMarshaller<T> marshaller() {
            try {
                return marshallerClass.newInstance();
            } catch (final Exception e) {
                throw new DynamoDbMappingException("Unable to instantiate marshaller " + marshallerClass, e);
            }
        }
    }

}
