package com.amazonaws.services.dynamodbv2;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.pojos.BinaryAttributeByteBufferClass;
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
    protected static final ProvisionedThroughput DEFAULT_PROVISIONED_THROUGHPUT = new ProvisionedThroughput(50L, 50L);
    private static final LocalDynamoDB LOCAL = new LocalDynamoDB();

    @BeforeClass
    public static void initializeLocalDynamoDb() {
        LOCAL.start();
    }

    @AfterClass
    public static void stopLocalDynamoDb() {
        LOCAL.stop();
    }

    protected static AmazonDynamoDB client() {
        return LOCAL.createClient();
    }

    protected static AmazonDynamoDB client(ClientConfiguration configuration) {
        return LOCAL.createClient(configuration);
    }

    protected static AmazonDynamoDBAsync asyncClient() {
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
