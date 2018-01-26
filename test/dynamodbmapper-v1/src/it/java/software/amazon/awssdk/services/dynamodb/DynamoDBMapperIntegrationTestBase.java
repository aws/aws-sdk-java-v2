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

package software.amazon.awssdk.services.dynamodb;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.pojos.BinaryAttributeByteBufferClass;
import utils.test.util.DynamoDBIntegrationTestBase;
import utils.test.util.DynamoDBTestBase;

public class DynamoDBMapperIntegrationTestBase extends DynamoDBIntegrationTestBase {

    public static void setUpMapperTestBase() {
        DynamoDBTestBase.setUpTestBase();
    }

    /*
     * Utility methods
     */
    protected static BinaryAttributeByteBufferClass getUniqueByteBufferObject(int contentLength) {
        BinaryAttributeByteBufferClass obj = new BinaryAttributeByteBufferClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setBinaryAttribute(ByteBuffer.wrap(generateByteArray(contentLength)));
        Set<ByteBuffer> byteBufferSet = new HashSet<ByteBuffer>();
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength)));
        obj.setBinarySetAttribute(byteBufferSet);
        return obj;
    }
}
