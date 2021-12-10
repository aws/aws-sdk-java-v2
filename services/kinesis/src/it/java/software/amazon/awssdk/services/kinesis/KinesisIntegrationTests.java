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

package software.amazon.awssdk.services.kinesis;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.HashKeyRange;
import software.amazon.awssdk.services.kinesis.model.InvalidArgumentException;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesis.model.SequenceNumberRange;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;

public class KinesisIntegrationTests extends AbstractTestCase {

    @Test
    public void testDescribeBogusStream() {
        try {
            client.describeStream(DescribeStreamRequest.builder().streamName("bogus-stream-name").build());
            Assert.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testDeleteBogusStream() {
        try {
            client.deleteStream(DeleteStreamRequest.builder().streamName("bogus-stream-name").build());
            Assert.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testGetIteratorForBogusStream() {
        try {
            client.getShardIterator(GetShardIteratorRequest.builder()
                                                           .streamName("bogus-stream-name")
                                                           .shardId("bogus-shard-id")
                                                           .shardIteratorType(ShardIteratorType.LATEST)
                                                           .build());
            Assert.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testGetFromNullIterator() {
        try {
            client.getRecords(GetRecordsRequest.builder().build());
            Assert.fail("Expected InvalidArgumentException");
        } catch (SdkServiceException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testGetFromBogusIterator() {
        try {
            client.getRecords(GetRecordsRequest.builder().shardIterator("bogusmonkeys").build());
            Assert.fail("Expected InvalidArgumentException");
        } catch (InvalidArgumentException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testCreatePutGetDelete() throws Exception {
        String streamName = "java-test-stream-" + System.currentTimeMillis();
        boolean created = false;

        try {

            // Create a stream with one shard.
            client.createStream(CreateStreamRequest.builder().streamName(streamName).shardCount(1).build());
            created = true;

            // Wait for it to become ACTIVE.
            List<Shard> shards = waitForStream(streamName);

            Assert.assertEquals(1, shards.size());
            Shard shard = shards.get(0);

            putRecord(streamName, "See No Evil");
            putRecord(streamName, "Hear No Evil");

            testGets(streamName, shard);

        } finally {
            if (created) {
                client.deleteStream(DeleteStreamRequest.builder().streamName(streamName).build());
            }
        }
    }

    private void testGets(final String streamName, final Shard shard) throws InterruptedException {
        // Wait for the shard to be in an active state
        // Get an iterator for the first shard.
        GetShardIteratorResponse iteratorResult = client.getShardIterator(
                GetShardIteratorRequest.builder()
                                       .streamName(streamName)
                                       .shardId(shard.shardId())
                                       .shardIteratorType(ShardIteratorType.AT_SEQUENCE_NUMBER)
                                       .startingSequenceNumber(shard.sequenceNumberRange().startingSequenceNumber())
                                       .build());
        Assert.assertNotNull(iteratorResult);

        String iterator = iteratorResult.shardIterator();
        Assert.assertNotNull(iterator);

        GetRecordsResponse result = getOneRecord(iterator);
        validateRecord(result.records().get(0), "See No Evil");

        result = getOneRecord(result.nextShardIterator());
        validateRecord(result.records().get(0), "Hear No Evil");

        result = client.getRecords(GetRecordsRequest.builder()
                                                    .shardIterator(result.nextShardIterator())
                                                    .build());
        assertTrue(result.records().isEmpty());
    }

    private GetRecordsResponse getOneRecord(String iterator) {
        int tries = 0;
        GetRecordsResponse result;
        List<Record> records;

        // Read the first record from the first shard (looping until it's
        // available).
        while (true) {
            tries += 1;
            if (tries > 100) {
                Assert.fail("Failed to read any records after 100 seconds");
            }

            result = client.getRecords(GetRecordsRequest.builder()
                                                        .shardIterator(iterator)
                                                        .limit(1)
                                                        .build());
            Assert.assertNotNull(result);
            Assert.assertNotNull(result.records());
            Assert.assertNotNull(result.nextShardIterator());

            records = result.records();
            if (records.size() > 0) {
                long arrivalTime = records.get(0).approximateArrivalTimestamp().toEpochMilli();
                Long delta = Math.abs(Instant.now().minusMillis(arrivalTime).toEpochMilli());
                // Assert that the arrival date is within 5 minutes of the current date to make sure it unmarshalled correctly.
                assertThat(delta, Matchers.lessThan(60 * 5000L));
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }

            iterator = result.nextShardIterator();
        }
        return result;
    }

    private void validateRecord(final Record record, String data) {
        Assert.assertNotNull(record);

        Assert.assertNotNull(record.sequenceNumber());
        new BigInteger(record.sequenceNumber());

        String value = record.data() == null ? null : record.data().asUtf8String();
        Assert.assertEquals(data, value);

        Assert.assertNotNull(record.partitionKey());

        // The timestamp should be relatively recent
        Assert.assertTrue(Duration.between(record.approximateArrivalTimestamp(), Instant.now()).toMinutes() < 5);
    }




    private PutRecordResponse putRecord(final String streamName,
                                      final String data) {

        PutRecordResponse result = client.putRecord(
                PutRecordRequest.builder()
                                .streamName(streamName)
                                .partitionKey("foobar")
                                .data(SdkBytes.fromUtf8String(data))
                                .build());
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.shardId());
        Assert.assertNotNull(result.sequenceNumber());

        return result;
    }


    private List<Shard> waitForStream(final String streamName)
            throws InterruptedException {

        while (true) {
            DescribeStreamResponse result = client.describeStream(DescribeStreamRequest.builder().streamName(streamName).build());
            Assert.assertNotNull(result);

            StreamDescription description = result.streamDescription();
            Assert.assertNotNull(description);

            Assert.assertEquals(streamName, description.streamName());
            Assert.assertNotNull(description.streamARN());
            Assert.assertFalse(description.hasMoreShards());

            StreamStatus status = description.streamStatus();
            Assert.assertNotNull(status);

            if (status == StreamStatus.ACTIVE) {
                List<Shard> shards = description.shards();
                validateShards(shards);

                return shards;
            }

            if (!(status == StreamStatus.CREATING
                  || status == StreamStatus.UPDATING)) {

                Assert.fail("Unexpected status '" + status + "'");
            }

            Thread.sleep(1000);
        }
    }

    private void validateShards(final List<Shard> shards) {
        Assert.assertNotNull(shards);
        Assert.assertFalse(shards.isEmpty());

        for (Shard shard : shards) {
            Assert.assertNotNull(shard);
            Assert.assertNotNull(shard.shardId());

            validateHashKeyRange(shard.hashKeyRange());
            validateSQNRange(shard.sequenceNumberRange());
        }

    }

    private void validateHashKeyRange(final HashKeyRange range) {

        Assert.assertNotNull(range);
        Assert.assertNotNull(range.startingHashKey());
        Assert.assertNotNull(range.endingHashKey());

        BigInteger start = new BigInteger(range.startingHashKey());
        BigInteger end = new BigInteger(range.endingHashKey());
        Assert.assertTrue(start.compareTo(end) <= 0);
    }

    private void validateSQNRange(final SequenceNumberRange range) {
        Assert.assertNotNull(range);
        Assert.assertNotNull(range.startingSequenceNumber());

        BigInteger start = new BigInteger(range.startingSequenceNumber());

        if (range.endingSequenceNumber() != null) {
            BigInteger end = new BigInteger(range.endingSequenceNumber());

            Assert.assertTrue(start.compareTo(end) <= 0);
        }
    }

}
