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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class DynamoDBMapperConfigTest {
    @Test
    public void noCircularStaticInitializationDependencies() {
        // This seems silly, but we have to statically initialize these classes by referencing them. If we do this and they
        // reference DynamoDBMapperConfig statically, then when we check DynamoDBMapperConfig.DEFAULT's fields, they will be
        // null because DynamoDBMapperConfig.DEFAULT would be initialized BEFORE these classes are.
        assertNotNull(DynamoDBMapperConfig.SaveBehavior.UPDATE);
        assertNotNull(DynamoDBMapperConfig.ConsistentReads.CONSISTENT);
        assertNotNull(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING);
        assertNotNull(DynamoDBMapperConfig.TableNameResolver.class);
        assertNotNull(DynamoDBMapperConfig.BatchWriteRetryStrategy.class);
        assertNotNull(DynamoDBMapperConfig.BatchLoadRetryStrategy.class);
        assertNotNull(DynamoDBTypeConverterFactory.class);
        assertNotNull(ConversionSchema.class);

        assertNotNull(DynamoDBMapperConfig.DEFAULT.getSaveBehavior());
        assertNotNull(DynamoDBMapperConfig.DEFAULT.getConsistentReads());
        assertNotNull(DynamoDBMapperConfig.DEFAULT.getPaginationLoadingStrategy());
        assertNotNull(DynamoDBMapperConfig.DEFAULT.getTableNameResolver());
        assertNotNull(DynamoDBMapperConfig.DEFAULT.getBatchWriteRetryStrategy());
        assertNotNull(DynamoDBMapperConfig.DEFAULT.getBatchLoadRetryStrategy());
        assertNotNull(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory());
        assertNotNull(DynamoDBMapperConfig.DEFAULT.getConversionSchema());
        assertEquals(DynamoDBMapperConfig.ByteBufferReadBehavior.MUTABLE_COPY,
                     DynamoDBMapperConfig.DEFAULT.getByteBufferReadBehavior());
    }

    @Test
    public void byteBufferReadBehaviorCanBeConfiguredAndMerged() {
        DynamoDBMapperConfig readOnly = DynamoDBMapperConfig.builder()
            .withByteBufferReadBehavior(DynamoDBMapperConfig.ByteBufferReadBehavior.READ_ONLY)
            .build();

        assertEquals(DynamoDBMapperConfig.ByteBufferReadBehavior.READ_ONLY,
                     readOnly.getByteBufferReadBehavior());
        assertEquals(DynamoDBMapperConfig.ByteBufferReadBehavior.READ_ONLY,
                     DynamoDBMapperConfig.DEFAULT.merge(readOnly).getByteBufferReadBehavior());
    }

    @Test
    public void toBuilder_withNoChanges_copiesEveryField() throws IllegalAccessException {
        DynamoDBMapperConfig original = fullyPopulatedConfig();

        DynamoDBMapperConfig copy = original.toBuilder().build();

        // Reflective so that a field added to the config later cannot be silently dropped by toBuilder().
        for (Field field : instanceFields()) {
            assertNotNull("fullyPopulatedConfig() must set " + field.getName(), field.get(original));
            assertSame(field.getName(), field.get(original), field.get(copy));
        }
    }

    @Test
    public void toBuilder_withOverride_changesOnlyThatField() {
        DynamoDBMapperConfig original = fullyPopulatedConfig();

        DynamoDBMapperConfig derived = original.toBuilder()
                                               .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.EVENTUAL)
                                               .build();

        assertEquals(DynamoDBMapperConfig.ConsistentReads.EVENTUAL, derived.getConsistentReads());
        assertEquals(DynamoDBMapperConfig.ConsistentReads.CONSISTENT, original.getConsistentReads());
        assertSame(original.getSaveBehavior(), derived.getSaveBehavior());
        assertSame(original.getConversionSchema(), derived.getConversionSchema());
        assertSame(original.getTableNameOverride(), derived.getTableNameOverride());
    }

    @Test
    public void toBuilder_onPartialConfig_leavesUnsetFieldsUnset() throws IllegalAccessException {
        DynamoDBMapperConfig partial = DynamoDBMapperConfig.builder()
                                                           .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
                                                           .build();

        DynamoDBMapperConfig copy = partial.toBuilder().build();

        assertEquals(DynamoDBMapperConfig.SaveBehavior.CLOBBER, copy.getSaveBehavior());
        for (Field field : instanceFields()) {
            if (!"saveBehavior".equals(field.getName())) {
                assertNull("toBuilder() must not fill in " + field.getName() + " from DEFAULT", field.get(copy));
            }
        }
    }

    @Test
    public void toBuilder_onDefault_copiesDefaultValues() {
        DynamoDBMapperConfig copy = DynamoDBMapperConfig.DEFAULT.toBuilder().build();

        assertSame(DynamoDBMapperConfig.DEFAULT.getConversionSchema(), copy.getConversionSchema());
        assertSame(DynamoDBMapperConfig.DEFAULT.getTypeConverterFactory(), copy.getTypeConverterFactory());
        assertSame(DynamoDBMapperConfig.DEFAULT.getTableNameResolver(), copy.getTableNameResolver());
    }

    private static DynamoDBMapperConfig fullyPopulatedConfig() {
        // Every value differs from DEFAULT so a copy that fell back to DEFAULT would be caught.
        return DynamoDBMapperConfig.builder()
            .withSaveBehavior(DynamoDBMapperConfig.SaveBehavior.CLOBBER)
            .withConsistentReads(DynamoDBMapperConfig.ConsistentReads.CONSISTENT)
            .withTableNameOverride(DynamoDBMapperConfig.TableNameOverride.withTableNamePrefix("dev_"))
            .withTableNameResolver((clazz, config) -> "ClassTable")
            .withObjectTableNameResolver((object, config) -> "ObjectTable")
            .withPaginationLoadingStrategy(DynamoDBMapperConfig.PaginationLoadingStrategy.EAGER_LOADING)
            .withConversionSchema(ConversionSchemas.V1)
            .withByteBufferReadBehavior(DynamoDBMapperConfig.ByteBufferReadBehavior.READ_ONLY)
            .withBatchWriteRetryStrategy(new DynamoDBMapperConfig.DefaultBatchWriteRetryStrategy(3))
            .withBatchLoadRetryStrategy(DynamoDBMapperConfig.NoRetryBatchLoadRetryStrategy.INSTANCE)
            .withTypeConverterFactory(DynamoDBTypeConverterFactory.standard().override().build())
            .build();
    }

    private static List<Field> instanceFields() {
        List<Field> fields = new ArrayList<>();
        for (Field field : DynamoDBMapperConfig.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        assertEquals("unexpected number of DynamoDBMapperConfig fields", 11, fields.size());
        return fields;
    }
}