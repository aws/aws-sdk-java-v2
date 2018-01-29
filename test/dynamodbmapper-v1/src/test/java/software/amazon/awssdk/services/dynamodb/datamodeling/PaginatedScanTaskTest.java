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
import static org.mockito.Mockito.*;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.dynamodb.DynamoDBClient;
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
    private DynamoDBClient dynamoDB;

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
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                ScanRequest req = (ScanRequest) args[0];
                if (req.segment().intValue() == 2) {
                    throw ProvisionedThroughputExceededException.builder().message("Slow Down!").build();
                }
                return mock(ScanResponse.class);
            }})
        .when(dynamoDB).scan(any(ScanRequest.class));

        try {
            parallelScanTask.nextBatchOfScanResponses();
            fail("Expected ProvisionedThroughputExceededException");
        } catch (ProvisionedThroughputExceededException expected) {
            // Ignored or expected.
        }

        executorService.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(executorService.isShutdown());
        verify(dynamoDB, atLeast(3)).scan(any(ScanRequest.class));
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

}
