/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling.marshallers;

import java.nio.ByteBuffer;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.datamodeling.ArgumentMarshaller.BinaryAttributeMarshaller;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * A marshaller that marshals Java {@code ByteBuffer}s into DynamoDB Binary
 * attributes.
 */
public class ByteBufferToBinaryMarshaller implements BinaryAttributeMarshaller {

    private static final ByteBufferToBinaryMarshaller INSTANCE =
            new ByteBufferToBinaryMarshaller();

    private ByteBufferToBinaryMarshaller() {
    }

    public static ByteBufferToBinaryMarshaller instance() {
        return INSTANCE;
    }

    @Override
    public AttributeValue marshall(Object obj) {
        return AttributeValue.builder().b(SdkBytes.fromByteBuffer((ByteBuffer) obj)).build();
    }
}
