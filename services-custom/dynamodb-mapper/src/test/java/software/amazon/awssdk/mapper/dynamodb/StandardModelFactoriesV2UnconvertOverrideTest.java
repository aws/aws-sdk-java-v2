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

import com.amazonaws.auth.AWSCredentialsProvider;
import java.lang.reflect.Method;
import software.amazon.awssdk.mapper.dynamodb.internal.DynamoDBMapperModelFactory;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Reruns the V2 unconvert suite through a custom {@link ConversionSchema} built with
 * {@link ConversionSchemas#v2Builder(String)}, so properties without a type converter are read by the schema's
 * {@link ItemConverter} using the getter and setter resolved from {@link StandardBeanProperties.Bean}.
 */
public class StandardModelFactoriesV2UnconvertOverrideTest extends StandardModelFactoriesV2UnconvertTest {

    private final DynamoDBMapperConfig config = new DynamoDBMapperConfig.Builder()
        .withTypeConverterFactory(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory())
        .withConversionSchema(ConversionSchemas.v2Builder("V2UnconvertOverride").build())
        .build();

    private final DynamoDBMapperModelFactory factory =
        StandardModelFactories.of(new S3Link.Factory(new S3ClientCache((AWSCredentialsProvider) null)));
    private final DynamoDBMapperModelFactory.TableFactory models = factory.getTableFactory(config);

    @Override
    protected <T> Object unconvert(Class<T> clazz, Method getter, Method setter, AttributeValue value) {
        final StandardAnnotationMaps.FieldMap<Object> map = StandardAnnotationMaps.of(getter, null);
        return models.getTable(clazz).field(map.attributeName()).unconvert(value);
    }

}
