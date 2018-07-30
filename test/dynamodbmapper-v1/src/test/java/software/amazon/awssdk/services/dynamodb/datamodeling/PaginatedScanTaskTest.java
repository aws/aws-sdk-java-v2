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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughputExceededException;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

@RunWith(MockitoJUnitRunner.class)
public class PaginatedScanTaskTest {

    private static final String TABLE_NAME = "FooTable";

    private static final int TOTAL_SEGMENTS = 5;

    private ParallelScanTask parallelScanTask;

    private ExecutorService executorService;

    @Mock
    private DynamoDbClient dynamoDB;

    /**
     * Custom matcher to match argument based on it's segment number
     *
     * @param segmentNumber Segment number to match for this stub.
     * @return Stubbed argument matcher
     */
    private static ScanRequest isSegmentNumber(int segmentNumber) {
        return argThat(new SegmentArgumentMatcher(segmentNumber));
    }

    @Before
    public void setup() {
        executorService = Executors.newSingleThreadExecutor();
        parallelScanTask = new ParallelScanTask(dynamoDB, createScanRequests(), executorService);
    }

    /**
     * A failed segment makes the scan task unusable and will always rethrow the same exception. In
     * this case it makes sense to shutdown the executor so that applications can shutdown faster. A
     * future enhancement could be to either retry failed segments, explicitly resume a failed scan,
     * or include metadata in the thrown exception about the state of the scan at the time it was
     * aborted. See <a href="https://github.com/aws/aws-sdk-java/pull/624">PR #624</a> and <a
     * href="https://github.com/aws/aws-sdk-java/issues/624">Issue #624</a> for more details.
     */
    @Test
    public void segmentFailsToScan_ExecutorServiceIsShutdown() throws InterruptedException {
        stubsuccessfulScan(0);
        stubsuccessfulScan(1);
        when(dynamoDB.scan(isSegmentNumber(2)))
                .thenThrow(ProvisionedThroughputExceededException.builder().message("Slow Down!").build());
        stubsuccessfulScan(3);
        stubsuccessfulScan(4);

        try {
            parallelScanTask.nextBatchOfScanResponses();
            fail("Expected ProvisionedThroughputExceededException");
        } catch (ProvisionedThroughputExceededException expected) {
            // Ignored or expected.
        }

        executorService.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(executorService.isShutdown());
    }

    /**
     * Stub a successful scan of a segment with a precanned item to return.
     *
     * @param segmentNumber Segment to stub.
     */
    private void stubsuccessfulScan(int segmentNumber) {
        when(dynamoDB.scan(isSegmentNumber(segmentNumber)))
                .thenReturn(ScanResponse.builder().items(generateItems()).build());
    }

    private Map<String, AttributeValue> generateItems() {
        final int numItems = 10;
        Map<String, AttributeValue> items = new HashMap<String, AttributeValue>(numItems);
        for (int i = 0; i < numItems; i++) {
            items.put(UUID.randomUUID().toString(), AttributeValue.builder().s("foo").build());
        }
        return items;
    }

    private List<ScanRequest> createScanRequests() {
        final List<ScanRequest> scanRequests = new ArrayList<ScanRequest>(TOTAL_SEGMENTS);
        for (int i = 0; i < TOTAL_SEGMENTS; i++) {
            scanRequests.add(createScanRequest(i));
        }
        return scanRequests;
    }

    private ScanRequest createScanRequest(int segmentNumber) {
        return ScanRequest.builder()
                .tableName(TABLE_NAME)
                .segment(segmentNumber)
                .totalSegments(TOTAL_SEGMENTS)
                .build();
    }

    /**
     * Custom argument matcher to match a {@link ScanRequest} on the segment number.
     */
    private static class SegmentArgumentMatcher extends ArgumentMatcher<ScanRequest> {

        private final int matchingSegmentNumber;

        private SegmentArgumentMatcher(int matchingSegmentNumber) {
            this.matchingSegmentNumber = matchingSegmentNumber;
        }

        @Override
        public boolean matches(Object argument) {
            if (!(argument instanceof ScanRequest)) {
                return false;
            }
            return matchingSegmentNumber == ((ScanRequest) argument).segment();
        }
    }
}
