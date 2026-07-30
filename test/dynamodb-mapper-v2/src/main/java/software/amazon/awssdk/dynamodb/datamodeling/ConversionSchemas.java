/*
 * Copyright 2016-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.dynamodb.datamodeling;

import software.amazon.awssdk.dynamodb.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType;
import software.amazon.awssdk.dynamodb.datamodeling.StandardModelFactories.Rule;
import software.amazon.awssdk.dynamodb.datamodeling.StandardModelFactories.RuleFactory;

/**
 * Pre-defined strategies for mapping between Java types and DynamoDB types.
 */
public final class ConversionSchemas {

    /**
     * The V1 schema mapping, which retains strict backwards compatibility with
     * the original DynamoDB data model. This is compatible with the
     * {@link DynamoDBTyped} annotation.
     */
    public static final ConversionSchema V1 = new NamedConversionSchema("V1ConversionSchema");

    /**
     * A V2 compatible conversion schema which is the default. Supports
     * both V1 and V2 style annotations.
     */
    public static final ConversionSchema V2_COMPATIBLE = new NamedConversionSchema("V2CompatibleConversionSchema");

    /**
     * The V2 schema mapping, which removes support for some legacy types.
     */
    public static final ConversionSchema V2 = new NamedConversionSchema("V2ConversionSchema");

    static final ConversionSchema DEFAULT = V2_COMPATIBLE;

    private static final class NamedConversionSchema implements ConversionSchema {
        private final String name;
        private NamedConversionSchema(String name) {
            this.name = name;
        }
        @Override
        public ItemConverter getConverter(Dependencies depends) {
            throw new UnsupportedOperationException(
                "Legacy ItemConverter not supported; use StandardModelFactories rules");
        }
        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Rule factory that wraps the standard type converter rules.
     * For built-in schemas (V1, V2_COMPATIBLE, V2), delegates directly to the
     * wrapped rules. Custom schemas are not supported in this port.
     */
    static class ItemConverterRuleFactory<V> implements RuleFactory<V> {
        private final RuleFactory<V> typeConverters;

        ItemConverterRuleFactory(DynamoDBMapperConfig config, RuleFactory<V> typeConverters) {
            this.typeConverters = typeConverters;
        }

        @Override
        public Rule<V> getRule(ConvertibleType<V> type) {
            return typeConverters.getRule(type);
        }
    }

    ConversionSchemas() {
        throw new UnsupportedOperationException();
    }
}
