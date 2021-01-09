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
import software.amazon.awssdk.services.kinesis.model.ListStreamsRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamsResponse;
import software.amazon.awssdk.services.kinesis.model.MergeShardsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.Record;
import software.amazon.awssdk.services.kinesis.model.ResourceNotFoundException;
import software.amazon.awssdk.services.kinesis.model.SequenceNumberRange;
import software.amazon.awssdk.services.kinesis.model.Shard;
import software.amazon.awssdk.services.kinesis.model.ShardIteratorType;
import software.amazon.awssdk.services.kinesis.model.SplitShardRequest;
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
    public void testKinesisOperations() throws Exception {
        String streamName = "java-test-stream-" + System.currentTimeMillis();
        boolean created = false;

        try {

            // Create a stream with one shard.
            System.out.println("Creating Stream...");
            client.createStream(CreateStreamRequest.builder().streamName(streamName).shardCount(1).build());
            System.out.println("  OK");
            created = true;

            // Verify that it shows up in a list call.
            findStreamInList(streamName);

            // Wait for it to become ACTIVE.
            System.out.println("Waiting for stream to become active...");
            List<Shard> shards = waitForStream(streamName);
            System.out.println("  OK");

            Assert.assertEquals(1, shards.size());
            Shard shard = shards.get(0);

            // Just to be really sure in case of eventual consistency...
            Thread.sleep(5000);

            testPuts(streamName, shard);

            // Wait a bit to make sure the records propagate.
            Thread.sleep(5000);

            System.out.println("Reading...");
            testGets(streamName, shard);
            System.out.println("  OK");

        } finally {
            if (created) {
                client.deleteStream(DeleteStreamRequest.builder().streamName(streamName).build());
            }
        }
    }

    private void testGets(final String streamName, final Shard shard) {
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

        System.out.println("  [Succeeded after " + tries + " tries]");
        Assert.assertEquals(1, records.size());
        validateRecord(records.get(0), "See No Evil");

        // Read the second record from the first shard.
        result = client.getRecords(GetRecordsRequest.builder()
                                                    .shardIterator(result.nextShardIterator())
                                                    .build());
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.records());
        Assert.assertNotNull(result.nextShardIterator());

        records = result.records();
        Assert.assertEquals(1, records.size());
        validateRecord(records.get(0), "See No Evil");

        // Try to read some more, get EOF.
        result = client.getRecords(GetRecordsRequest.builder()
                                                    .shardIterator(result.nextShardIterator())
                                                    .build());
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.records());
        Assert.assertTrue(result.records().isEmpty());
        Assert.assertNull(result.nextShardIterator());
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

    private void testPuts(final String streamName, final Shard shard)
            throws InterruptedException {

        // Put a record into the shard.
        System.out.println("Putting two records...");
        PutRecordResponse r1 = putRecord(streamName, "See No Evil");
        Assert.assertEquals(shard.shardId(), r1.shardId());

        // Check that it's sequence number is sane.
        BigInteger startingSQN = new BigInteger(
                shard.sequenceNumberRange().startingSequenceNumber()
        );
        BigInteger sqn1 = new BigInteger(r1.sequenceNumber());
        Assert.assertTrue(sqn1.compareTo(startingSQN) >= 0);

        // Put another record, which should show up later in the same shard.
        PutRecordResponse r2 = putRecord(streamName, "See No Evil");
        Assert.assertEquals(shard.shardId(), r2.shardId());
        BigInteger sqn2 = new BigInteger(r2.sequenceNumber());
        System.out.println("  OK");

        // Not guaranteed an order unless we explicitly ask for one, but
        // it has to at least be larger than the starting sqn.
        Assert.assertTrue(sqn2.compareTo(startingSQN) >= 0);

        // Split the shard in two: [0-1000) and [1000-*]
        System.out.println("Splitting the shard...");
        List<Shard> shards = splitShard(streamName, shard, 1000);
        System.out.println("  OK");

        // Sleep a bit for eventual consistency.
        Thread.sleep(5000);


        // Put records into the two new shards, one after another.
        System.out.println("Putting some more...");
        PutRecordResponse r3 = putRecordExplicit(streamName, "999");
        PutRecordResponse r4 = putRecordExplicit(streamName,
                                               "1000",
                                               r3.sequenceNumber());

        BigInteger sqn3 = new BigInteger(r3.sequenceNumber());
        BigInteger sqn4 = new BigInteger(r4.sequenceNumber());
        Assert.assertTrue(sqn4.compareTo(sqn3) >= 0);
        System.out.println("  OK");

        // Merge the two shards back together.
        System.out.println("Merging the shards back together...");
        mergeShards(streamName,
                    shards.get(1).shardId(),
                    shards.get(2).shardId());
        System.out.println("  OK");
    }


    private List<Shard> splitShard(final String streamName,
                                   final Shard shard,
                                   final long splitHashKey)
            throws InterruptedException {

        client.splitShard(SplitShardRequest.builder()
                                           .streamName(streamName)
                                           .shardToSplit(shard.shardId())
                                           .newStartingHashKey(Long.toString(splitHashKey))
                                           .build());

        List<Shard> shards = waitForStream(streamName);

        Assert.assertEquals(3, shards.size());

        Shard old = shards.get(0);
        Assert.assertEquals(shard.shardId(), old.shardId());
        Assert.assertNotNull(
                old.sequenceNumberRange().endingSequenceNumber()
                            );

        Shard new1 = shards.get(1);
        Assert.assertEquals(shard.shardId(), new1.parentShardId());
        validateHashKeyRange(new1.hashKeyRange(), 0L, splitHashKey - 1);

        Shard new2 = shards.get(2);
        Assert.assertEquals(shard.shardId(), new2.parentShardId());
        validateHashKeyRange(new2.hashKeyRange(), splitHashKey, null);
        Assert.assertEquals(old.hashKeyRange().endingHashKey(),
                            new2.hashKeyRange().endingHashKey());

        return shards;
    }

    private List<Shard> mergeShards(final String streamName,
                                    final String shard1,
                                    final String shard2)
            throws InterruptedException {

        client.mergeShards(MergeShardsRequest.builder()
                                             .streamName(streamName)
                                             .shardToMerge(shard1)
                                             .adjacentShardToMerge(shard2)
                                             .build());

        List<Shard> shards = waitForStream(streamName);

        Assert.assertEquals(4, shards.size());
        Shard merged = shards.get(3);

        BigInteger start =
                new BigInteger(merged.hashKeyRange().startingHashKey());
        BigInteger end =
                new BigInteger(merged.hashKeyRange().endingHashKey());

        Assert.assertEquals(BigInteger.valueOf(0), start);
        Assert.assertTrue(end.compareTo(BigInteger.valueOf(1000)) >= 0);

        return shards;
    }

    private void validateHashKeyRange(final HashKeyRange range,
                                      final Long start,
                                      final Long end) {
        if (start != null) {
            Assert.assertEquals(BigInteger.valueOf(start),
                                new BigInteger(range.startingHashKey()));
        }
        if (end != null) {
            Assert.assertEquals(BigInteger.valueOf(end),
                                new BigInteger(range.endingHashKey()));
        }
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

    private PutRecordResponse putRecordExplicit(final String streamName,
                                              final String hashKey) {

        PutRecordResponse result = client.putRecord(PutRecordRequest.builder()
                                                                  .streamName(streamName)
                                                                  .partitionKey("foobar")
                                                                  .explicitHashKey(hashKey)
                                                                  .data(SdkBytes.fromUtf8String("Speak No Evil"))
                                                                  .build());
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.shardId());
        Assert.assertNotNull(result.sequenceNumber());

        return result;
    }

    private PutRecordResponse putRecordExplicit(final String streamName,
                                              final String hashKey,
                                              final String minSQN) {

        PutRecordResponse result = client.putRecord(PutRecordRequest.builder()
                                                                  .streamName(streamName)
                                                                  .partitionKey("foobar")
                                                                  .explicitHashKey(hashKey)
                                                                  .sequenceNumberForOrdering(minSQN)
                                                                  .data(SdkBytes.fromUtf8String("Hear No Evil"))
                                                                  .build());
        Assert.assertNotNull(result);

        Assert.assertNotNull(result.shardId());
        Assert.assertNotNull(result.sequenceNumber());

        return result;
    }

    private void findStreamInList(final String streamName) {
        boolean found = false;

        String start = null;
        while (true) {

            ListStreamsResponse result = client.listStreams(ListStreamsRequest.builder().exclusiveStartStreamName(start).build());

            Assert.assertNotNull(result);

            List<String> names = result.streamNames();
            Assert.assertNotNull(names);

            if (names.size() > 0) {
                if (names.contains(streamName)) {
                    found = true;
                }

                start = names.get(names.size() - 1);
            }

            if (!result.hasMoreStreams()) {
                break;
            }

        }

        Assert.assertTrue(found);
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
