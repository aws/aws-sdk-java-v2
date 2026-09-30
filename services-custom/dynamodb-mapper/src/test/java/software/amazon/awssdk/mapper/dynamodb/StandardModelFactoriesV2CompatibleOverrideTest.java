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
 * Reruns the V2_COMPATIBLE suite through a custom {@link ConversionSchema} built with
 * {@link ConversionSchemas#v2CompatibleBuilder(String)}. A custom schema converts fields that have no type converter
 * through its {@link ItemConverter} rather than the standard rules, so this covers annotation handling on that path
 * (for example {@code @DynamoDBTyped(BOOL)}, which V2_COMPATIBLE does not otherwise honor by type).
 */
public class StandardModelFactoriesV2CompatibleOverrideTest extends StandardModelFactoriesV2CompatibleTest {

    private final DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTypeConverterFactory(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory())
        .withConversionSchema(ConversionSchemas.v2CompatibleBuilder("V2CompatibleOverride").build())
        .build();

    private final DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    private final DynamoDBMapperModelFactory.TableFactory models = factory.getTableFactory(config);

    @Override
    protected <T> AttributeValue convert(Class<T> clazz, Method getter, Object value) {
        final StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(getter, null);
        return models.getTable(clazz).field(map.attributeName()).convert(value);
    }

}
