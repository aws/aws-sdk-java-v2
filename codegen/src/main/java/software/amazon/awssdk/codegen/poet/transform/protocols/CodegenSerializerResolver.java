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

package software.amazon.awssdk.codegen.poet.transform.protocols;

import java.util.List;
import java.util.Map;

/**
 * Resolves {@link CodegenSerializer} for a given instance.
 */
public interface CodegenSerializerResolver {

    CodegenSerializerResolver DEFAULT = new CodegenSerializerResolver() {
        @Override
        @SuppressWarnings("unchecked")
        public <T> CodegenSerializer<T> serializerFor(T value) {
            if (value == null) {
                throw new NullPointerException("value");
            }
            Class<?> xclass = value.getClass();
            if (xclass == String.class) {
                return (CodegenSerializer<T>) new CodegenSerializer.CodegenStringSerializer();
            }
            if (List.class.isAssignableFrom(xclass)) {
                return (CodegenSerializer<T>) new CodegenSerializer.CodegenListSerializer(this);
            }
            if (Map.class.isAssignableFrom(xclass)) {
                return (CodegenSerializer<T>) new CodegenSerializer.CodegenMapSerializer(this);
            }
            return (CodegenSerializer<T>) new CodegenSerializer.CodegenLiteralSerializer();
        }
    };

    static CodegenSerializerResolver getDefault() {
        return DEFAULT;
    }

    /**
     * Returns a proper codegen serializer for the given value.
     */
    <T> CodegenSerializer<T> serializerFor(T value);
}
