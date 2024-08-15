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

import com.squareup.javapoet.CodeBlock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.utils.MapUtils;

/**
 * Encapsulates the codegen serialization logic for the given value.
 */
public interface CodegenSerializer<T> {
    /**
     * Creates a codegen serialization for the given value using the {@link CodeBlock.Builder}.
     */
    void serialize(T value, CodeBlock.Builder builder);

    /**
     * Codegen for String values.
     */
    class CodegenStringSerializer implements CodegenSerializer<String> {

        @Override
        public void serialize(String value, CodeBlock.Builder builder) {
            builder.add("$S", value);
        }
    }

    /**
     * Codegen for literal values.
     */
    class CodegenLiteralSerializer implements CodegenSerializer<Object> {

        @Override
        public void serialize(Object value, CodeBlock.Builder builder) {
            builder.add("$L", value);
        }
    }

    /**
     * Codegen for List values. Serialized as {@code Arrays.asList(v0, ⋯, vn)}.
     */
    class CodegenListSerializer implements CodegenSerializer<List<?>> {
        private final CodegenSerializerResolver resolver;

        public CodegenListSerializer(CodegenSerializerResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public void serialize(List<?> list, CodeBlock.Builder builder) {
            builder.add("$T.asList(", Arrays.class);
            boolean needsComma = false;
            for (Object value : list) {
                if (needsComma) {
                    builder.add(", ");
                }
                CodegenSerializer<Object> serializer = resolver.serializerFor(value);
                serializer.serialize(value, builder);
                needsComma = true;
            }
            builder.add(")");
        }
    }


    /**
     * Codegen for Map values. Serialized as {@code MapUtils.of(k0, v0, ⋯, kn, vn)}. This only works for a small amount of
     * key-value pairs, up to seven.
     */
    class CodegenMapSerializer implements CodegenSerializer<Map<?, ?>> {
        private final CodegenSerializerResolver resolver;

        public CodegenMapSerializer(CodegenSerializerResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public void serialize(Map<?, ?> map, CodeBlock.Builder builder) {
            builder.add("$T.of(", MapUtils.class);
            boolean needsComma = false;
            for (Map.Entry<?, ?> kvp : map.entrySet()) {
                if (needsComma) {
                    builder.add(", ");
                }
                Object key = kvp.getKey();
                CodegenSerializer<Object> keySerializer = resolver.serializerFor(key);
                keySerializer.serialize(key, builder);
                builder.add(", ");
                Object value = kvp.getValue();
                CodegenSerializer<Object> valueSerializer = resolver.serializerFor(value);
                valueSerializer.serialize(value, builder);
                needsComma = true;
            }
            builder.add(")");
        }
    }
}
