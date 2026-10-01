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

import java.lang.reflect.Method;
import software.amazon.awssdk.mapper.dynamodb.internal.DynamoDBMapperModelFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Reruns the V1 suite through a custom {@link ConversionSchema} built with {@link ConversionSchemas#v1Builder(String)},
 * so properties without a type converter go through the schema's {@link ItemConverter}. With the V1 marshallers,
 * booleans default to a DynamoDB number, which makes this the case where honoring {@code @DynamoDBTyped(BOOL)} changes
 * the stored type.
 */
public class StandardModelFactoriesV1OverrideTest extends StandardModelFactoriesV1Test {

    private final DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTypeConverterFactory(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory())
        .withConversionSchema(ConversionSchemas.v1Builder("V1Override").build())
        .build();

    private final DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    private final DynamoDBMapperModelFactory.TableFactory models = factory.getTableFactory(config);

    @Override
    protected <T> AttributeValue convert(Class<T> clazz, Method getter, Object value) {
        final StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(getter, null);
        return models.getTable(clazz).field(map.attributeName()).convert(value);
    }

}
