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

package software.amazon.awssdk.mapper.dynamodb.internal.unmarshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.mapper.dynamodb.internal.MapperBinaryUtils;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals BinarySet values as sets of Java
 * {@code ByteBuffer}s.
 */
@SdkInternalApi
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
            result.add(MapperBinaryUtils.toWritableByteBuffer(sdkBytes));
        }
        return result;
    }
}
