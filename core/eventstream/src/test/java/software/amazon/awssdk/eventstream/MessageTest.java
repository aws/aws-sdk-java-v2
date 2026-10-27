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
package software.amazon.awssdk.eventstream;

import com.sun.management.ThreadMXBean;

import java.io.ByteArrayOutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageTest {
    private static final ThreadMXBean threadMXBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final MethodHandle GET_THREAD_ALLOCATED_BYTES;

    static {
        MethodHandle handle;
        try {
            handle = MethodHandles.lookup().findVirtual(ThreadMXBean.class, "getCurrentThreadAllocatedBytes",
                MethodType.methodType(long.class));
        } catch (Throwable t) {
            handle = null;
        }
        GET_THREAD_ALLOCATED_BYTES = handle;
    }

    private static void assumeGetCurrentThreadAllocatedBytesExists() {
        Assumptions.assumeTrue(GET_THREAD_ALLOCATED_BYTES != null,
                               "JVM does not have ThreadMXBean::getCurrentThreadAllocatedBytes");
    }

    private static long getCurrentThreadAllocatedBytes() {
        try {
            return (long) GET_THREAD_ALLOCATED_BYTES.invoke(threadMXBean);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Test
    public void emptyVector() {
        Message message = new Message(emptyMap(), new byte[]{});
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.encode(baos);
        byte[] bytes = baos.toByteArray();

        byte[] expected = new byte[]{
            0, 0, 0, 16,       // total_length
            0, 0, 0, 0,        // headers_length
            5, -62, 72, -21,   // prelude_crc
            125, -104, -56, -1 // message_crc
        };

        assertArrayEquals(expected, bytes);

        assertEquals(message, Message.decode(ByteBuffer.wrap(expected)));
    }

    @Test
    public void appdataVector() {
        byte[] bytes = new byte[]{
            0, 0, 0, 0x1d,                              // total_length
            0, 0, 0, 0,                                 // headers_length
            (byte) 0xfd, 0x52, (byte) 0x8c, 0x5a,       // prelude_crc
            0x7b, 0x27, 0x66, 0x6f, 0x6f, 0x27, 0x3a,   // payload
            0x27, 0x62, 0x61, 0x72, 0x27, 0x7d,
            // 0xc3 65 39 36
            (byte) 0xc3, 0x65, 0x39, 0x36               // message_crc
        };

        Message message = new Message(emptyMap(), "{'foo':'bar'}".getBytes(UTF_8));

        assertEquals(message, Message.decode(ByteBuffer.wrap(bytes)));
    }

    @Test
    public void roundTripTests() {
        roundTrip(new Message(emptyMap(), new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));

        Map<String, HeaderValue> headers = new HashMap<>();
        headers.put(":content-type", HeaderValue.fromString("application/json"));
        headers.put("content-encoding", HeaderValue.fromString("gzip"));
        headers.put("request-id", HeaderValue.fromByteArray(new byte[]{ 1, 2, 3, 4, 5 }));
        headers.put("more-stuff", HeaderValue.fromInteger(27));
        roundTrip(new Message(headers, new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9 }));
    }

    @Test
    public void generativeTest() throws Exception {
        long SEED = 8912374098123423L;
        TestUtils utils = new TestUtils(SEED);
        Random rand = new Random(SEED);
        for (int i = 0; i < 10_000; i++) {
            byte[] padding = new byte[rand.nextInt(128 * 1024)];
            rand.nextBytes(padding);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(padding);
            Message message = utils.randomMessage();
            message.encode(baos);

            int arraylen = 256 * 1024;
            int arrayoffset = rand.nextInt(4 * 1024);
            ByteBuffer buf = ((ByteBuffer) ByteBuffer.allocate(arraylen).position(arrayoffset)).slice();
            buf.put(baos.toByteArray());
            buf.flip();

            buf.get(padding);
            Message decoded = Message.decode(buf);

            assertEquals(message, decoded);

            // toByteBuffer path
            Message decodedViaToByteBuffer = Message.decode(message.toByteBuffer());
            assertEquals(message, decodedViaToByteBuffer);
        }
    }

    @Test
    public void headerOrder() {
        Map<String, HeaderValue> expectedHeaders = new LinkedHashMap<>();
        for (int i = 0; i < 255; i++) {
            expectedHeaders.put(String.valueOf(i), HeaderValue.fromInteger(i));
        }
        Message expected = new Message(expectedHeaders, new byte[]{});

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.encode(baos);
        Message actual = Message.decode(ByteBuffer.wrap(baos.toByteArray()));

        Map<String, HeaderValue> actualHeaders = actual.getHeaders();
        Iterator<Entry<String, HeaderValue>> expectedIter = expectedHeaders.entrySet().iterator();
        Iterator<Entry<String, HeaderValue>> actualIter = actualHeaders.entrySet().iterator();
        for (int i = 0; i < 255; i++) {
            assertEquals(expectedIter.next(), actualIter.next());
        }
    }

    /**
     * How much the allocation overhead needs to change before we fail the 'test'.
     * The tests are really benchmarks which notify on significant deviation.
     * If it regresses, we want to check if the regression is avoidable.
     */
    static final double ALLOCATION_RATE_EPSILON = 0.5;

    @Test
    public void allocationRateEncodeSmallPayload() {
        assumeGetCurrentThreadAllocatedBytesExists();
        TestUtils utils = new TestUtils(8912374098123423L);
        long encodedBytes = 0;
        List<Message> messages = utils.randomMessages(5_000, 1_000);
        long initialAllocatedBytes = getCurrentThreadAllocatedBytes();

        for (Message message : messages) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.encode(baos);
            encodedBytes += baos.size();
        }

        long totalAllocatedBytes = getCurrentThreadAllocatedBytes() - initialAllocatedBytes;
        double overhead = ((double) totalAllocatedBytes / encodedBytes) - 1;

        double expectedOverhead = 6.7;
        assertTrue((overhead - expectedOverhead) < ALLOCATION_RATE_EPSILON,
                   "Allocation overhead should be close to " + expectedOverhead + ", but was " + overhead);
    }

    @Test
    public void allocationRateToByteBufferSmallPayload() {
        assumeGetCurrentThreadAllocatedBytesExists();
        TestUtils utils = new TestUtils(8912374098123423L);
        long encodedBytes = 0;
        List<Message> messages = utils.randomMessages(5_000, 1_000);
        long initialAllocatedBytes = getCurrentThreadAllocatedBytes();

        for (Message message : messages) {
            encodedBytes += message.toByteBuffer().remaining();
        }

        long totalAllocatedBytes = getCurrentThreadAllocatedBytes() - initialAllocatedBytes;
        double overhead = ((double) totalAllocatedBytes / encodedBytes) - 1;

        double expectedOverhead = 4.5;
        assertTrue((overhead - expectedOverhead) < ALLOCATION_RATE_EPSILON,
                   "Allocation overhead should be close to " + expectedOverhead + ", but was " + overhead);
    }

    @Test
    public void allocationRateEncodeLargePayload() {
        assumeGetCurrentThreadAllocatedBytesExists();
        TestUtils utils = new TestUtils(8912374098123423L);
        long encodedBytes = 0;
        List<Message> messages = utils.randomMessages(5_000, 50_000);
        long initialAllocatedBytes = getCurrentThreadAllocatedBytes();

        for (Message message : messages) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.encode(baos);
            encodedBytes += baos.size();
        }

        long totalAllocatedBytes = getCurrentThreadAllocatedBytes() - initialAllocatedBytes;
        double overhead = ((double) totalAllocatedBytes / encodedBytes) - 1;

        double expectedOverhead = 2.1;
        assertTrue((overhead - expectedOverhead) < ALLOCATION_RATE_EPSILON,
            "Allocation overhead should be close to " + expectedOverhead + ", but was " + overhead);
    }

    @Test
    public void allocationRateToByteBufferLargePayload() {
        assumeGetCurrentThreadAllocatedBytesExists();
        TestUtils utils = new TestUtils(8912374098123423L);
        long encodedBytes = 0;
        List<Message> messages = utils.randomMessages(5_000, 50_000);
        long initialAllocatedBytes = getCurrentThreadAllocatedBytes();

        for (Message message : messages) {
            encodedBytes += message.toByteBuffer().remaining();
        }

        long totalAllocatedBytes = getCurrentThreadAllocatedBytes() - initialAllocatedBytes;
        double overhead = ((double) totalAllocatedBytes / encodedBytes) - 1;

        double expectedOverhead = 0.1;
        assertTrue((overhead - expectedOverhead) < ALLOCATION_RATE_EPSILON,
            "Allocation overhead should be close to " + expectedOverhead + ", but was " + overhead);
    }

    static void roundTrip(Message expected) {
        // Using the encode path
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        expected.encode(baos);
        Message actual = Message.decode(ByteBuffer.wrap(baos.toByteArray()));
        assertEquals(expected, actual);

        // Using the toByteBuffer path
        actual = Message.decode(expected.toByteBuffer());
        assertEquals(expected, actual);
    }
}
