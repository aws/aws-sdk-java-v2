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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.services.dynamodb.datamodeling.StandardBeanProperties.Bean;

/**
 * Reflection assistant for {@link DynamoDbMapper}
 *
 * @deprecated Replaced by {@link StandardBeanProperties}/{@link StandardModelFactories}
 */
@Deprecated
@SdkInternalApi
final class DynamoDbMappingsRegistry {

    /**
     * The default instance.
     */
    private static final DynamoDbMappingsRegistry INSTANCE = new DynamoDbMappingsRegistry();
    /**
     * The cache of class to mapping definition.
     */
    private final ConcurrentMap<Class<?>, Mappings> mappings = new ConcurrentHashMap<Class<?>, Mappings>();

    /**
     * Gets the default instance.
     * @return The default instance.
     */
    static final DynamoDbMappingsRegistry instance() {
        return INSTANCE;
    }

    /**
     * Gets the mapping definition for a given class.
     * @param clazz The class.
     * @return The mapping definition.
     */
    final Mappings mappingsOf(final Class<?> clazz) {
        if (!mappings.containsKey(clazz)) {
            mappings.putIfAbsent(clazz, new Mappings(clazz));
        }
        return mappings.get(clazz);
    }

    /**
     * Holds the properties for mapping an object.
     */
    static final class Mappings {
        private final Map<String, Mapping> byNames = new HashMap<String, Mapping>();

        private Mappings(final Class<?> clazz) {
            for (final Map.Entry<String, Bean<Object, Object>> bean :
                    StandardBeanProperties.of((Class<Object>) clazz).map().entrySet()) {
                final Mapping mapping = new Mapping(bean.getValue());
                byNames.put(mapping.getAttributeName(), mapping);
            }
        }

        final Collection<Mapping> mappings() {
            return byNames.values();
        }
    }

    /**
     * Holds the properties for mapping an object attribute.
     */
    static final class Mapping {
        private final Bean<Object, Object> bean;

        private Mapping(final Bean<Object, Object> bean) {
            this.bean = bean;
        }

        final Method getter() {
            return bean.type().getter();
        }

        final boolean isPrimaryKey() {
            return bean.properties().keyType() != null;
        }

        final boolean isVersion() {
            return bean.properties().versioned();
        }

        final String getAttributeName() {
            return bean.properties().attributeName();
        }
    }

}
