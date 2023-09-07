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

package software.amazon.awssdk.http.crt.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CRTAsyncInputStreamAdapterTest {
    private CRTAsyncInputStreamAdapter adapter;
    private MockReadObserver observer;

    @BeforeEach
    public void setUp() {
        observer = new MockReadObserver();
        adapter = new CRTAsyncInputStreamAdapter(observer);
    }

    @Test
    public void testSingleByteRead() throws IOException {
        byte[] data = { 42 };
        adapter.onDataReceived(data);

        int readByte = adapter.read();
        assertEquals(42, readByte);
        assertEquals(1, observer.readCount);
    }

    @Test
    public void testMultiByteRead() throws IOException {
        byte[] data = { 1, 2, 3, 4, 5 };
        adapter.onDataReceived(data);

        byte[] buffer = new byte[5];
        int bytesRead = adapter.read(buffer, 0, buffer.length);

        assertEquals(5, bytesRead);
        assertEquals(5, observer.readCount);
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, buffer[i]);
        }
    }

    @Test
    public void testPartialRead() throws IOException {
        byte[] data = { 10, 20, 30, 40 };
        adapter.onDataReceived(data);

        byte[] buffer = new byte[2];
        int bytesRead = adapter.read(buffer, 0, buffer.length);

        assertEquals(2, bytesRead);
        assertEquals(10, buffer[0]);
        assertEquals(20, buffer[1]);
        assertEquals(2, observer.readCount);
    }

    @Test
    public void testClose() throws IOException {
        adapter.close();
        int readByte = adapter.read();
        assertEquals(-1, readByte);
    }

    @Test
    public void testMultipleOnDataReceivedWithSequentialRead() throws IOException {
        byte[] data1 = new byte[10];
        for (int i = 0; i < 10; i++) {
            data1[i] = (byte) i;
        }
        byte[] data2 = new byte[20];
        for (int i = 0; i < 20; i++) {
            data2[i] = (byte) (i + 10);
        }

        adapter.onDataReceived(data1);
        adapter.onDataReceived(data2);

        byte[] buffer1 = new byte[15];
        int bytesRead1 = adapter.read(buffer1, 0, buffer1.length);

        assertEquals(15, bytesRead1);
        assertEquals(15, observer.readCount);

        for (int i = 0; i < 15; i++) {
            assertEquals(i, buffer1[i]);
        }

        // Now read the subsequent bytes
        byte[] buffer2 = new byte[5];
        int bytesRead2 = adapter.read(buffer2, 0, buffer2.length);

        assertEquals(5, bytesRead2);
        assertEquals(20, observer.readCount);  // Total read bytes should now be 20

        for (int i = 0; i < 5; i++) {
            assertEquals(i + 15, buffer2[i]);
        }
    }

    @Test
    public void testCloseDuringRead() throws Exception {
        Thread producer = new Thread(() -> {
            try {
                Thread.sleep(100);  // Delay to simulate some async behavior
                adapter.onDataReceived(new byte[]{1, 2, 3, 4, 5});
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        final int[] queuedReadVal = new int[1];
        Thread consumer = new Thread(() -> {
            try {
                queuedReadVal[0] = adapter.read();
            } catch (IOException e) {
                // Handle exception
            }
        });

        consumer.start();
        Thread.sleep(50);  // Allow consumer to start reading
        adapter.close();
        producer.start();

        consumer.join();
        producer.join();

        assertEquals(-1, queuedReadVal[0]);
        assertEquals(-1, adapter.read()); // Reading after close on an empty stream should return -1
    }

    @Test
    public void testReadWithNullBuffer() {
        assertThrows(NullPointerException.class, () -> {
            adapter.read(null, 0, 5);
        });
    }

    @Test
    public void testReadWithNegativeOffset() {
        byte[] buffer = new byte[5];
        assertThrows(IndexOutOfBoundsException.class, () -> {
            adapter.read(buffer, -1, 5);
        });
    }

    @Test
    public void testReadWithNegativeLength() {
        byte[] buffer = new byte[5];
        assertThrows(IndexOutOfBoundsException.class, () -> {
            adapter.read(buffer, 0, -1);
        });
    }


    // Mock implementation for ReadObserver
    private static class MockReadObserver implements CRTAsyncInputStreamAdapter.ReadObserver {
        private int readCount = 0;

        @Override
        public void bytesRead(int count) {
            readCount += count;
        }
    }
}

