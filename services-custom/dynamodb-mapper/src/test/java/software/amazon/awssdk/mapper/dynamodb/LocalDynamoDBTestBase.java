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

package software.amazon.awssdk.mapper.dynamodb;

import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.mapper.dynamodb.pojos.BinaryAttributeByteBufferClass;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class LocalDynamoDBTestBase {
    protected static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT =
        ProvisionedThroughput.builder().readCapacityUnits(50L).writeCapacityUnits(50L).build();
    private static final LocalDynamoDB LOCAL = new LocalDynamoDB();

    @BeforeClass
    public static void initializeLocalDynamoDb() {
        LOCAL.start();
    }

    @AfterClass
    public static void stopLocalDynamoDb() {
        LOCAL.stop();
    }

    protected static DynamoDbClient client() {
        return LOCAL.createClient();
    }

    protected static DynamoDbClient client(ClientOverrideConfiguration configuration) {
        return LOCAL.createClient(configuration);
    }

    protected static DynamoDbAsyncClient asyncClient() {
        return LOCAL.createAsyncClient();
    }

    protected Map<String, AttributeValue> getMapKey(String attributeName, AttributeValue value) {
        HashMap<String, AttributeValue> map = new HashMap();
        map.put(attributeName, value);
        return map;
    }

    protected static byte[] generateByteArray(int length) {
        byte[] bytes = new byte[length];

        for(int i = 0; i < length; ++i) {
            bytes[i] = (byte)(i % 127);
        }

        return bytes;
    }

    protected static <T extends Object> Set<T> toSet(T... array) {
        Set<T> set = new HashSet<T>();
        for ( T t : array ) {
            set.add(t);
        }
        return set;
    }

    protected static <T extends Object> Set<T> toSet(Collection<T> collection) {
        Set<T> set = new HashSet<T>();
        for ( T t : collection ) {
            set.add(t);
        }
        return set;
    }

    protected static BinaryAttributeByteBufferClass getUniqueByteBufferObject(int contentLength) {
        BinaryAttributeByteBufferClass obj = new BinaryAttributeByteBufferClass();
        obj.setKey(UUID.randomUUID().toString());
        obj.setBinaryAttribute(ByteBuffer.wrap(generateByteArray(contentLength)));
        Set<ByteBuffer> byteBufferSet = new HashSet<ByteBuffer>();
        byteBufferSet.add(ByteBuffer.wrap(generateByteArray(contentLength)));
        obj.setBinarySetAttribute(byteBufferSet);
        return obj;
    }
}
