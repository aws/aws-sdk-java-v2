/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.converter.attribute;

import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.utils.Validate;

@SdkPublicApi
@Immutable
@ThreadSafe
public class UnionAttributeConverter<T> {
    private AttributeConverter<T> attributeConverter;
    private SubtypeAttributeConverter<? super T> subtypeAttributeConverter;

    private UnionAttributeConverter(AttributeConverter<T> attributeConverter,
                                    SubtypeAttributeConverter<? super T> subtypeAttributeConverter) {
        Validate.isTrue(attributeConverter == null || subtypeAttributeConverter == null,
                        "Only one converter may be specified.");
        Validate.isTrue(attributeConverter != null || subtypeAttributeConverter != null,
                        "At least one converter must be specified.");
        this.attributeConverter = attributeConverter;
        this.subtypeAttributeConverter = subtypeAttributeConverter;
    }

    public static <T> UnionAttributeConverter<T> create(AttributeConverter<T> converter) {
        return new UnionAttributeConverter<>(converter, null);
    }

    public static <T> UnionAttributeConverter<T> create(SubtypeAttributeConverter<? super T> converter) {
        return new UnionAttributeConverter<>(null, converter);
    }

    public <O> O visit(Visitor<T, O> visitor) {
        if (attributeConverter != null) {
            return visitor.visit(attributeConverter);
        } else {
            return visitor.visit(subtypeAttributeConverter);
        }
    }

    public interface Visitor<T, O> {
        default O visit(AttributeConverter<T> converter) {
            throw new UnsupportedOperationException();
        }

        default O visit(SubtypeAttributeConverter<? super T> converter) {
            throw new UnsupportedOperationException();
        }
    }
}
