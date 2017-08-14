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

import java.util.HashMap;
import java.util.Map;

/**
 * A strategy for mapping between Java types and DynamoDB types. Serves as a
 * factory for {@code ItemConverter} instances that implement this mapping.
 * Standard implementations are available in the {@link ConversionSchemas}
 * class.
 */
public interface ConversionSchema {

    /**
     * Creates an {@code ItemConverter}, injecting dependencies from the
     * {@code DynamoDBMapper} that needs it.
     *
     * @param dependencies the dependencies to inject
     * @return a new ItemConverter
     */
    ItemConverter getConverter(Dependencies dependencies);

    /**
     * Dependency injection for the {@code ItemConverter}s that this
     * {@code ConversionSchema} generates.
     */
    class Dependencies {

        private final Map<Class<?>, Object> values;

        public Dependencies() {
            values = new HashMap<Class<?>, Object>();
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Class<T> clazz) {
            return (T) values.get(clazz);
        }

        public <T> Dependencies with(Class<T> clazz, T value) {
            values.put(clazz, value);
            return this;
        }

        @Override
        public String toString() {
            return values.toString();
        }
    }
}
