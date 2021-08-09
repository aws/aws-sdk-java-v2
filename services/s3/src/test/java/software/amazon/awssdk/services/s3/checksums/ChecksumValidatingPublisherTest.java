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

package software.amazon.awssdk.services.s3.checksums;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.checksums.Md5Checksum;

/**
 * Unit test for ChecksumValidatingPublisher
 */
public class ChecksumValidatingPublisherTest {
  private static int TEST_DATA_SIZE = 32;  // size of the test data, in bytes
  private static final int CHECKSUM_SIZE = 16;
  private static byte[] testData;

  @BeforeClass
  public static void populateData() {
    testData = new byte[TEST_DATA_SIZE + CHECKSUM_SIZE];
    for (int i = 0; i < TEST_DATA_SIZE; i++) {
      testData[i] = (byte)(i & 0x7f);
    }
    final Md5Checksum checksum = new Md5Checksum();
    checksum.update(testData, 0, TEST_DATA_SIZE);
    byte[] checksumBytes = checksum.getChecksumBytes();
    for (int i = 0; i < CHECKSUM_SIZE; i++) {
      testData[TEST_DATA_SIZE + i] = checksumBytes[i];
    }
  }

  @Test
  public void testSinglePacket() {
    final TestPublisher driver = new TestPublisher();
    final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, TEST_DATA_SIZE));
    final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
    p.subscribe(s);

    driver.doOnNext(ByteBuffer.wrap(testData));
    driver.doOnComplete();

    assertTrue(s.hasCompleted());
    assertFalse(s.isOnErrorCalled());
  }

  @Test
  public void testTwoPackets() {
    for (int i = 1; i < TEST_DATA_SIZE + CHECKSUM_SIZE - 1; i++) {
      final TestPublisher driver = new TestPublisher();
      final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, TEST_DATA_SIZE));
      final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
      p.subscribe(s);

      driver.doOnNext(ByteBuffer.wrap(testData, 0, i));
      driver.doOnNext(ByteBuffer.wrap(testData, i, TEST_DATA_SIZE + CHECKSUM_SIZE - i));
      driver.doOnComplete();

      assertTrue(s.hasCompleted());
      assertFalse(s.isOnErrorCalled());
    }
  }

  @Test
  public void testTinyPackets() {
    for (int packetSize = 1; packetSize < CHECKSUM_SIZE; packetSize++) {
      final TestPublisher driver = new TestPublisher();
      final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, TEST_DATA_SIZE));
      final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
      p.subscribe(s);
      int currOffset = 0;
      while (currOffset < TEST_DATA_SIZE + CHECKSUM_SIZE) {
        final int toSend = Math.min(packetSize, TEST_DATA_SIZE + CHECKSUM_SIZE - currOffset);
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
    // When the length is unknown, the last 16 bytes are treated as a checksum, but are later ignored when completing
    final TestPublisher driver = new TestPublisher();
    final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(testData, 0, TEST_DATA_SIZE));
    final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, new Md5Checksum(), 0);
    p.subscribe(s);

    byte[] randomChecksumData = new byte[testData.length];
    System.arraycopy(testData, 0, randomChecksumData, 0, TEST_DATA_SIZE);
    for (int i = TEST_DATA_SIZE; i < randomChecksumData.length; i++) {
      randomChecksumData[i] = (byte)((testData[i] + 1) & 0x7f);
    }

    driver.doOnNext(ByteBuffer.wrap(randomChecksumData));
    driver.doOnComplete();

    assertTrue(s.hasCompleted());
    assertFalse(s.isOnErrorCalled());
  }

  @Test
  public void checksumValidationFailure_throwsSdkClientException_NotNPE() {
    final byte[] incorrectData = new byte[0];
    final TestPublisher driver = new TestPublisher();
    final TestSubscriber s = new TestSubscriber(Arrays.copyOfRange(incorrectData, 0, TEST_DATA_SIZE));
    final ChecksumValidatingPublisher p = new ChecksumValidatingPublisher(driver, new Md5Checksum(), TEST_DATA_SIZE + CHECKSUM_SIZE);
    p.subscribe(s);

    driver.doOnNext(ByteBuffer.wrap(incorrectData));
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
