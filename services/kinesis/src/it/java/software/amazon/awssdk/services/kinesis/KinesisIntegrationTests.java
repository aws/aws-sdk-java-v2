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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
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
            fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testDeleteBogusStream() {
        try {
            client.deleteStream(DeleteStreamRequest.builder().streamName("bogus-stream-name").build());
            fail("Expected ResourceNotFoundException");
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
            fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testGetFromNullIterator() {
        try {
            client.getRecords(GetRecordsRequest.builder().build());
            fail("Expected InvalidArgumentException");
        } catch (SdkServiceException exception) {
            // Ignored or expected.
        }
    }

    @Test
    public void testGetFromBogusIterator() {
        try {
            client.getRecords(GetRecordsRequest.builder().shardIterator("bogusmonkeys").build());
            fail("Expected InvalidArgumentException");
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
            Thread.sleep(1000);

            assertThat(shards.size()).isEqualTo(1);
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

    private void testGets(String streamName, Shard shard) {
        // Wait for the shard to be in an active state
        // Get an iterator for the first shard.
        GetShardIteratorResponse iteratorResult = client.getShardIterator(
            GetShardIteratorRequest.builder()
                                   .streamName(streamName)
                                   .shardId(shard.shardId())
                                   .shardIteratorType(ShardIteratorType.AT_SEQUENCE_NUMBER)
                                   .startingSequenceNumber(shard.sequenceNumberRange().startingSequenceNumber())
                                   .build());
        assertThat(iteratorResult).isNotNull();

        String iterator = iteratorResult.shardIterator();
        assertThat(iterator).isNotNull();

        GetRecordsResponse result = getOneRecord(iterator);
        validateRecord(result.records().get(0), "See No Evil");

        result = getOneRecord(result.nextShardIterator());
        validateRecord(result.records().get(0), "Hear No Evil");

        result = client.getRecords(GetRecordsRequest.builder()
                                                    .shardIterator(result.nextShardIterator())
                                                    .build());
        assertThat(result.records()).isEmpty();
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
                fail("Failed to read any records after 100 seconds");
            }

            result = client.getRecords(GetRecordsRequest.builder()
                                                        .shardIterator(iterator)
                                                        .limit(1)
                                                        .build());
            assertThat(result).isNotNull();
            assertThat(result.records()).isNotNull();
            assertThat(result.nextShardIterator()).isNotNull();

            records = result.records();
            if (records.size() > 0) {
                long arrivalTime = records.get(0).approximateArrivalTimestamp().toEpochMilli();
                Long delta = Math.abs(Instant.now().minusMillis(arrivalTime).toEpochMilli());
                // Assert that the arrival date is within 5 minutes of the current date to make sure it unmarshalled correctly.
                assertThat(delta).isLessThan(60 * 5000L);
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

    private void validateRecord(Record record, String data) {
        assertThat(record).isNotNull();
        assertThat(record.sequenceNumber()).isNotNull();

        new BigInteger(record.sequenceNumber());

        String value = record.data() == null ? null : record.data().asUtf8String();
        assertThat(value).isEqualTo(data);

        assertThat(record.partitionKey()).isNotNull();

        // The timestamp should be relatively recent
        assertThat(Duration.between(record.approximateArrivalTimestamp(), Instant.now()).toMinutes()).isLessThan(5);
    }




    private PutRecordResponse putRecord(String streamName, String data) {

        PutRecordResponse result = client.putRecord(
            PutRecordRequest.builder()
                            .streamName(streamName)
                            .partitionKey("foobar")
                            .data(SdkBytes.fromUtf8String(data))
                            .build());
        assertThat(result).isNotNull();
        assertThat(result.shardId()).isNotNull();
        assertThat(result.sequenceNumber()).isNotNull();

        return result;
    }


    private List<Shard> waitForStream(String streamName) throws InterruptedException {

        while (true) {
            DescribeStreamResponse result = client.describeStream(DescribeStreamRequest.builder().streamName(streamName).build());
            assertThat(result).isNotNull();

            StreamDescription description = result.streamDescription();
            assertThat(description).isNotNull();
            assertThat(description.streamName()).isEqualTo(streamName);
            assertThat(description.streamARN()).isNotNull();
            assertThat(description.hasMoreShards()).isFalse();

            StreamStatus status = description.streamStatus();
            assertThat(status).isNotNull();

            if (status == StreamStatus.ACTIVE) {
                List<Shard> shards = description.shards();
                validateShards(shards);

                return shards;
            }

            if (!(status == StreamStatus.CREATING
                  || status == StreamStatus.UPDATING)) {

                fail("Unexpected status '" + status + "'");
            }

            Thread.sleep(1000);
        }
    }

    private void validateShards(List<Shard> shards) {
        assertThat(shards).isNotNull().isNotEmpty();

        for (Shard shard : shards) {
            assertThat(shard).isNotNull();
            assertThat(shard.shardId()).isNotNull();

            validateHashKeyRange(shard.hashKeyRange());
            validateSQNRange(shard.sequenceNumberRange());
        }
    }

    private void validateHashKeyRange(HashKeyRange range) {
        assertThat(range).isNotNull();
        assertThat(range.startingHashKey()).isNotNull();
        assertThat(range.endingHashKey()).isNotNull();

        BigInteger start = new BigInteger(range.startingHashKey());
        BigInteger end = new BigInteger(range.endingHashKey());
        assertThat(start).isLessThanOrEqualTo(end);
    }

    private void validateSQNRange(SequenceNumberRange range) {
        assertThat(range).isNotNull();
        assertThat(range.startingSequenceNumber()).isNotNull();

        BigInteger start = new BigInteger(range.startingSequenceNumber());

        if (range.endingSequenceNumber() != null) {
            BigInteger end = new BigInteger(range.endingSequenceNumber());
            assertThat(start).isLessThanOrEqualTo(end);
        }
    }
}
