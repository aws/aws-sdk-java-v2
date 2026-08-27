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
package software.amazon.awssdk.mapper.dynamodb.internal.marshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.BinarySetAttributeMarshaller;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals sets of Java {@code byte[]}s into DynamoDB
 * BinarySet attributes.
 */
@SdkInternalApi
public class ByteArraySetToBinarySetMarshaller
        implements BinarySetAttributeMarshaller {

    private static final ByteArraySetToBinarySetMarshaller INSTANCE =
            new ByteArraySetToBinarySetMarshaller();

    public static ByteArraySetToBinarySetMarshaller instance() {
        return INSTANCE;
    }

    private ByteArraySetToBinarySetMarshaller() {
    }

    @Override
    public AttributeValue marshall(Object obj) {
        @SuppressWarnings("unchecked")
        Set<byte[]> buffers = (Set<byte[]>) obj;
        List<SdkBytes> attributes = new ArrayList<SdkBytes>(buffers.size());

        for (byte[] b : buffers) {
            attributes.add(SdkBytes.fromByteArray(b));
        }

        return AttributeValue.builder().bs(attributes).build();
    }
}
