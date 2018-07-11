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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class ParallelScanTask {

    /**
     * The list of hard copies of ScanRequest with different segment number.
     */
    private final List<ScanRequest> parallelScanRequests;

    private final int totalSegments;

    /**
     * Cache all the future tasks, so that we can extract the exception when
     * we see failed segment scan.
     */
    private final List<Future<ScanResponse>> segmentScanFutureTasks;

    /**
     * Cache all the most recent ScanResponse on each segment.
     */
    private final List<ScanResponse> segmentScanResponses;

    /**
     * The current state of the scan on each segment.
     * Used as the monitor for synchronization.
     */
    private final List<SegmentScanstate> segmentScanstates;
    private final DynamoDbClient dynamo;
    private ExecutorService executorService;

    @Deprecated
    public ParallelScanTask(DynamoDbMapper mapper, DynamoDbClient dynamo, List<ScanRequest> parallelScanRequests) {
        this(dynamo, parallelScanRequests);
    }

    ParallelScanTask(DynamoDbClient dynamo, List<ScanRequest> parallelScanRequests) {
        this(dynamo, parallelScanRequests, Executors.newCachedThreadPool());
    }

    @SdkTestInternalApi
    ParallelScanTask(DynamoDbClient dynamo, List<ScanRequest> parallelScanRequests,
                     ExecutorService executorService) {
        this.dynamo = dynamo;
        this.parallelScanRequests = parallelScanRequests;
        this.totalSegments = parallelScanRequests.size();
        this.executorService = executorService;

        // Create synchronized views of the list to guarantee any changes are visible across all threads.
        segmentScanFutureTasks = Collections
                .synchronizedList(new ArrayList<Future<ScanResponse>>(totalSegments));
        segmentScanResponses = Collections.synchronizedList(new ArrayList<ScanResponse>(totalSegments));
        segmentScanstates = Collections
                .synchronizedList(new ArrayList<SegmentScanstate>(totalSegments));

        initSegmentScanstates();
    }

    String getTableName() {
        return parallelScanRequests.get(0).tableName();
    }

    public boolean isAllSegmentScanFinished() {
        synchronized (segmentScanstates) {
            for (int segment = 0; segment < totalSegments; segment++) {
                if (segmentScanstates.get(segment) != SegmentScanstate.SegmentScanCompleted) {
                    return false;
                }
            }
            // Shut down if all data have been scanned and loaded.
            executorService.shutdown();
            return true;
        }
    }

    public List<ScanResponse> nextBatchOfScanResponses() throws SdkClientException {
        /**
         * Kick-off all the parallel scan tasks.
         */
        startScanNextPages();
        /**
         * Wait till all the tasks have finished.
         */
        synchronized (segmentScanstates) {
            while (segmentScanstates.contains(SegmentScanstate.Waiting)
                   || segmentScanstates.contains(SegmentScanstate.Scanning)) {
                try {
                    segmentScanstates.wait();
                } catch (InterruptedException ie) {
                    throw SdkClientException.builder()
                                            .message("Parallel scan interrupted by other thread.")
                                            .cause(ie)
                                            .build();
                }
            }
            /**
             *  Keep the lock on segmentScanstates until all the cached results are marshaled and returned.
             */
            return marshalParallelScanResponses();
        }

    }

    private void startScanNextPages() {
        for (int segment = 0; segment < totalSegments; segment++) {
            final int currentSegment = segment;
            final SegmentScanstate currentSegmentState = segmentScanstates.get(currentSegment);
            /**
             * Assert: Should never see any task in state of "Scanning" when starting a new batch.
             */
            if (currentSegmentState == SegmentScanstate.Scanning) {

                throw SdkClientException.builder()
                                        .message("Should never see a 'Scanning' state when starting parallel scans.")
                                        .build();

            } else if (currentSegmentState == SegmentScanstate.Failed ||
                     currentSegmentState == SegmentScanstate.SegmentScanCompleted) {
                /**
                 * Skip any failed or completed segment, and clear the corresponding cached result.
                 */
                segmentScanResponses.set(currentSegment, null);
                continue;
            } else {
                /**
                 * Otherwise, submit a new future task and save it in segmentScanFutureTasks.
                 */
                // Update the state to "Scanning" and notify any waiting thread.
                synchronized (segmentScanstates) {
                    segmentScanstates.set(currentSegment, SegmentScanstate.Scanning);
                    segmentScanstates.notifyAll();
                }
                Future<ScanResponse> futureTask = executorService.submit(() -> {
                    try {
                        if (currentSegmentState == SegmentScanstate.HasNextPage) {
                            return scanNextPageOfSegment(currentSegment, true);
                        } else if (currentSegmentState == SegmentScanstate.Waiting) {
                            return scanNextPageOfSegment(currentSegment, false);
                        } else {
                            throw SdkClientException.builder().message("Should not start a new future task").build();
                        }
                    } catch (Exception e) {
                        synchronized (segmentScanstates) {
                            segmentScanstates.set(currentSegment, SegmentScanstate.Failed);
                            segmentScanstates.notifyAll();
                            executorService.shutdown();
                        }
                        throw e;
                    }
                });
                // Cache the future task (for getting the Exceptions in the working thread).
                segmentScanFutureTasks.set(currentSegment, futureTask);
            }
        }
    }

    private List<ScanResponse> marshalParallelScanResponses() {
        List<ScanResponse> scanResults = new LinkedList<ScanResponse>();
        for (int segment = 0; segment < totalSegments; segment++) {
            SegmentScanstate currentSegmentState = segmentScanstates.get(segment);
            /**
             * Rethrow the exception from any failed segment scan.
             */
            if (currentSegmentState == SegmentScanstate.Failed) {
                try {
                    segmentScanFutureTasks.get(segment).get();
                    throw SdkClientException.builder().message("No Exception found in the failed scan task.").build();
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    if (cause instanceof SdkServiceException) {
                        throw (SdkServiceException) cause;
                    } else {
                        throw SdkClientException.builder()
                                                .message("Internal error during the scan on segment #" + segment + ".")
                                                .build();
                    }
                } catch (Exception e) {
                    throw SdkClientException.builder()
                                            .message("Error during the scan on segment #" + segment + ".")
                                            .cause(e)
                                            .build();
                }
            } else if (currentSegmentState == SegmentScanstate.HasNextPage ||
                       currentSegmentState == SegmentScanstate.SegmentScanCompleted) {
                /**
                 * Get the ScanResponse from cache if the segment scan has finished.
                 */
                ScanResponse scanResult = segmentScanResponses.get(segment);
                scanResults.add(scanResult);
            } else if (currentSegmentState == SegmentScanstate.Waiting
                       || currentSegmentState == SegmentScanstate.Scanning) {
                throw SdkClientException.builder()
                                        .message("Should never see a 'Scanning' or 'Waiting' state when marshalling parallel " +
                                                 "scan results.")
                                        .build();
            }
        }
        return scanResults;
    }

    private ScanResponse scanNextPageOfSegment(int currentSegment, boolean checkLastEvaluatedKey) {
        ScanRequest segmentScanRequest = parallelScanRequests.get(currentSegment);
        if (checkLastEvaluatedKey) {
            ScanResponse lastScanResult = segmentScanResponses.get(currentSegment);
            segmentScanRequest = segmentScanRequest.toBuilder().exclusiveStartKey(lastScanResult.lastEvaluatedKey()).build();
        } else {
            segmentScanRequest = segmentScanRequest.toBuilder().exclusiveStartKey(null).build();
        }
        ScanResponse scanResult = dynamo.scan(DynamoDbMapper.applyUserAgent(segmentScanRequest));

        /**
         * Cache the scan result in segmentScanResponses.
         * We should never try to get these scan results by calling get() on the cached future tasks.
         */
        segmentScanResponses.set(currentSegment, scanResult);

        /**
         * Update the state and notify any waiting thread.
         */
        synchronized (segmentScanstates) {
            if (null == scanResult.lastEvaluatedKey()) {
                segmentScanstates.set(currentSegment, SegmentScanstate.SegmentScanCompleted);
            } else {
                segmentScanstates.set(currentSegment, SegmentScanstate.HasNextPage);
            }
            segmentScanstates.notifyAll();
        }
        return scanResult;
    }

    private void initSegmentScanstates() {
        for (int segment = 0; segment < totalSegments; segment++) {
            segmentScanFutureTasks.add(null);
            segmentScanResponses.add(null);
            segmentScanstates.add(SegmentScanstate.Waiting);
        }
    }

    /**
     * Enumeration of the possible states of the scan on a segment.
     */
    private enum SegmentScanstate {
        /** The scan on the segment is waiting for resources to execute and has not started yet. */
        Waiting,

        /** The scan is in process, and hasn't finished yet. */
        Scanning,

        /** The scan has already failed. */
        Failed,

        /** The scan on the current page has finished, but there are more pages in the segment to be scanned. */
        HasNextPage,

        /** The scan on the whole segment has completed. */
        SegmentScanCompleted,
    }
}
