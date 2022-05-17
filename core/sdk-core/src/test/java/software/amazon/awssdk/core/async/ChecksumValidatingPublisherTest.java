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

package software.amazon.awssdk.core.async;

import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.checksums.Algorithm;
import software.amazon.awssdk.core.checksums.SdkChecksum;
import software.amazon.awssdk.core.internal.async.ChecksumValidatingPublisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit test for ChecksumValidatingPublisher
 */
public class ChecksumValidatingPublisherTest {
    public static final String SHA256_OF_HELLO_WORLD = "ZOyIygCyaOW6GjVnihtTFtIS9PNmskdyMlNKiuyjfzw=";
    private static byte[] testData;

    @BeforeClass
    public static void populateData() {
        testData = "Hello world".getBytes(StandardCharsets.UTF_8);
    }

    @Test
    public void testSinglePacket() {
        final TestPublisher driver = new TestPublisher();
        final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, testData.length));
        final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver,
                SdkChecksum.forAlgorithm(Algorithm.SHA256), SHA256_OF_HELLO_WORLD);
        p.subscribe(s);

        driver.doOnNext(ByteBuffer.wrap(testData));
        driver.doOnComplete();

        assertTrue(s.hasCompleted());
        assertFalse(s.isOnErrorCalled());
    }

    @Test
    public void testTwoPackets() {
        for (int i = 1; i < testData.length - 1; i++) {
            final TestPublisher driver = new TestPublisher();
            final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, testData.length));
            final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, SdkChecksum.forAlgorithm(Algorithm.SHA256), SHA256_OF_HELLO_WORLD);
            p.subscribe(s);

            driver.doOnNext(ByteBuffer.wrap(testData, 0, i));
            driver.doOnNext(ByteBuffer.wrap(testData, i, testData.length - i));
            driver.doOnComplete();

            assertTrue(s.hasCompleted());
            assertFalse(s.isOnErrorCalled());
        }
    }

    @Test
    public void testTinyPackets() {
        for (int packetSize = 1; packetSize < 2; packetSize++) {
            final TestPublisher driver = new TestPublisher();
            final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, testData.length));
            final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, SdkChecksum.forAlgorithm(Algorithm.SHA256),
                    SHA256_OF_HELLO_WORLD);
            p.subscribe(s);
            int currOffset = 0;
            while (currOffset < testData.length) {
                final int toSend = Math.min(packetSize, testData.length - currOffset);
                driver.doOnNext(ByteBuffer.wrap(testData, currOffset, toSend));
                currOffset += toSend;
            }
            driver.doOnComplete();
            assertTrue(s.hasCompleted());
            assertFalse(s.isOnErrorCalled());
        }
    }

    @Test
    public void testUnknownLength() {
        final TestPublisher driver = new TestPublisher();
        final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, testData.length));
        final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, SdkChecksum.forAlgorithm(Algorithm.SHA256), SHA256_OF_HELLO_WORLD);
        p.subscribe(s);
        byte[] randomChecksumData = new byte[testData.length];
        System.arraycopy(testData, 0, randomChecksumData, 0, testData.length);
        for (int i = testData.length; i < randomChecksumData.length; i++) {
            randomChecksumData[i] = (byte) ((testData[i] + 1) & 0x7f);
        }
        driver.doOnNext(ByteBuffer.wrap(randomChecksumData));
        driver.doOnComplete();
        assertTrue(s.hasCompleted());
        assertFalse(s.isOnErrorCalled());
    }

    @Test
    public void checksumValidationFailure_throwsSdkClientException() {
        final TestPublisher driver = new TestPublisher();
        final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, testData.length));
        final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, SdkChecksum.forAlgorithm(Algorithm.SHA256),
                "someInvalidData");
        p.subscribe(s);
        driver.doOnNext(ByteBuffer.wrap(testData));
        driver.doOnComplete();
        assertTrue(s.isOnErrorCalled());
        assertFalse(s.hasCompleted());
    }

    private class TestSubscriber implements Subscriber<ByteBuffer> {
        final byte[] expected;
        final List<ByteBuffer> received;
        boolean completed;
        boolean onErrorCalled;

        TestSubscriber(byte[] expected) {
            this.expected = expected;
            this.received = new ArrayList<>();
            this.completed = false;
        }

        @Override
        public void onSubscribe(Subscription s) {
            fail("This method not expected to be invoked");
            throw new UnsupportedOperationException("!!!TODO: implement this");
        }

        @Override
        public void onNext(ByteBuffer buffer) {
            received.add(buffer);
        }


        @Override
        public void onError(Throwable t) {
            onErrorCalled = true;
        }

        @Override
        public void onComplete() {
            int matchPos = 0;
            for (ByteBuffer buffer : received) {
                byte[] bufferData = new byte[buffer.limit() - buffer.position()];
                buffer.get(bufferData);
                assertArrayEquals(Arrays.copyOfRange(expected, matchPos, matchPos + bufferData.length), bufferData);
                matchPos += bufferData.length;
            }
            assertEquals(expected.length, matchPos);
            completed = true;
        }

        public boolean hasCompleted() {
            return completed;
        }

        public boolean isOnErrorCalled() {
            return onErrorCalled;
        }
    }

    private class TestPublisher implements Publisher<ByteBuffer> {
        Subscriber<? super ByteBuffer> s;

        @Override
        public void subscribe(Subscriber<? super ByteBuffer> s) {
            this.s = s;
        }

        public void doOnNext(ByteBuffer b) {
            s.onNext(b);
        }

        public void doOnComplete() {
            s.onComplete();
        }
    }
}
