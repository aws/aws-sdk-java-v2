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
package software.amazon.awssdk.mapper.dynamodb.marshallers;

import software.amazon.awssdk.annotations.SdkInternalApi;
import java.nio.ByteBuffer;

import software.amazon.awssdk.mapper.dynamodb.ArgumentMarshaller.BinaryAttributeMarshaller;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals Java {@code byte[]}s into DynamoDB Binary
 * attributes.
 */
@SdkInternalApi
public class ByteArrayToBinaryMarshaller implements BinaryAttributeMarshaller {

    private static final ByteArrayToBinaryMarshaller INSTANCE =
            new ByteArrayToBinaryMarshaller();

    public static ByteArrayToBinaryMarshaller instance() {
        return INSTANCE;
    }

    private ByteArrayToBinaryMarshaller() {
    }

    @Override
    public AttributeValue marshall(Object obj) {
        return AttributeValue.builder().b(SdkBytes.fromByteArray((byte[]) obj)).build();
    }
}
