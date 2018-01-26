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

package software.amazon.awssdk.services.dynamodb.datamodeling.unmarshallers;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals BinarySet values as sets of Java
 * {@code byte[]}s.
 */
public class ByteArraySetUnmarshaller extends BsUnmarshaller {

    private static final ByteArraySetUnmarshaller INSTANCE =
            new ByteArraySetUnmarshaller();

    private ByteArraySetUnmarshaller() {
    }

    public static ByteArraySetUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        Set<byte[]> result = new HashSet<byte[]>();

        for (ByteBuffer buffer : value.bs()) {
            if (buffer.hasArray()) {
                result.add(buffer.array());
            } else {
                byte[] array = new byte[buffer.remaining()];
                buffer.get(array);
                result.add(array);
            }
        }

        return result;
    }
}
