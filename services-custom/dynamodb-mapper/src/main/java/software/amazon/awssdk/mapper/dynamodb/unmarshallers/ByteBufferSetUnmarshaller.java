/*
 * Copyright 2014-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.mapper.dynamodb.unmarshallers;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals BinarySet values as sets of Java
 * {@code ByteBuffer}s.
 */
public class ByteBufferSetUnmarshaller extends BSUnmarshaller {

    private static final ByteBufferSetUnmarshaller INSTANCE =
            new ByteBufferSetUnmarshaller();

    public static ByteBufferSetUnmarshaller instance() {
        return INSTANCE;
    }

    private ByteBufferSetUnmarshaller() {
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        Set<ByteBuffer> result = new HashSet<ByteBuffer>();
        for (SdkBytes sdkBytes : value.bs()) {
            // Writable copy for v1 parity; SdkBytes.asByteBuffer() would be read-only.
            result.add(ByteBuffer.wrap(sdkBytes.asByteArray()));
        }
        return result;
    }
}
