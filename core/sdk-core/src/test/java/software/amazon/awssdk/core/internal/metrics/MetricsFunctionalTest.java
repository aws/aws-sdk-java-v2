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

package software.amazon.awssdk.core.internal.metrics;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.time.Instant;
import java.util.Iterator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * FAQ:
 * Q. Can multiple metrics of the same type (SdkMetricType) be recorded within the same SdkMetricCollector.
 * A. Yes, they will have different timestamps and so will be distinct from one-another.
 *
 * Q. Why doesn't SdkCollection.query(...) search into children?
 * A. The results of query is a flat-list and there is no easy or convenient way to represent the hierarchy within those
 *     results. Instead, the publisher can implement a tree search algorithm if they desire and query within children
 *     that are returned in a top-level query.
 *
 * Q. Why don't we use a static global variable to track the current parent metrics object and avoid the need to have
 *     the code pass it around as arguments to method calls?
 * A. The static global model works well with a single threaded application, but runs into difficulty when multiple
 *     threads are in play. It can be solved with ThreadLocal usage and additional methods to override it but even then
 *     its easy to misuse and I've been burned on other services that follow this pattern. Keeping it explicit avoids
 *     any misunderstandings.
 *
 * Q. Why publish child blocks independently when they are also being published as part of the nested parent block?
 * A. They need to be published as part of the parent block regardless to allow pre-publishing aggregation to occur
 *     across the nested data (e.g. AVERAGE_ATTEMPT_DURATION). So then the question becomes 'should the child blocks
 *     be published independently?'. I chose to do this primarily because I wanted the metrics to be published as soon
 *     as possible, only publishing the parent block could cause excessive delay in publishing any metrics at all and
 *     risk losing them if the process is killed or something. The publisher can choose not to publish the nested metrics
 *     knowing that they will have already been published anyway, but as already mentioned they are still available for
 *     aggregation at that point.
 *
 * Q. Is the timestamp in the SdkMetricContext of the SdkMetric that represents a nested collection (SdkMetricCollection)
 *     redundant because the child collection has an endTime on it?
 * A. Yes. This was a sacrifice to avoid having to create a new set of interfaces to represent nested collections.
 *
 * Open questions:
 * 1. Should we make it configurable whether a child metric block gets published or not? A publisher can ignore them but
 *    if they are used excessively it could result in a performance hit depending on how the publisher is implemented.
 *    If we do implement this, I think it should be configurable per-block, like when the block is created.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class MetricsFunctionalTest {
    public static class Execution {
        private final Subscriber<SdkMetricCollection> metricsSubscriber;

        public Execution(Subscriber<SdkMetricCollection> metricsSubscriber) {
            this.metricsSubscriber = metricsSubscriber;
        }

        public void execute() {
            // create a top level (parent) metrics collector keyed with the execution ID
            try (SdkMetricCollector metrics =
                     SdkMetricCollector.create(SdkMetricContext.create(SdkMetricTypes.EXECUTION, "id123"))) {
                metrics.publisher().subscribe(metricsSubscriber);

                for (int i = 1; i <= 3; ++i) {
                    attemptExecution(i, metrics);
                }
            }
        }

        private void attemptExecution(int attemptNumber, SdkMetricCollector parentMetrics) {
            // Composite human-readable attempt key
            String retryIdentifier = parentMetrics.context().identifier() + ":" + attemptNumber;

            // create a nested (child) metrics collector keyed with the execution ID and attempt number
            try (SdkMetricCollector metrics =
                     parentMetrics.createChild(
                         SdkMetricContext.create(SdkMetricTypes.EXECUTION_ATTEMPT, retryIdentifier))) {

                // report a specific metric value within the child metrics collector (execution attempt)
                metrics.reportMetric(SdkMetricTypes.BYTES_SENT, 1234L);
            }
        }
    }

    @Mock
    private Subscriber<SdkMetricCollection> mockSubscriber;

    @Captor
    private ArgumentCaptor<SdkMetricCollection> sdkMetricCollectionCaptor;

    /**
     * Expected publish events :
     *
     * EXECUTION_ATTEMPT : id123:1
     * - BYTES_SENT : 1234L
     * EXECUTION_ATTEMPT : id123:2
     * - BYTES_SENT : 1234L
     * EXECUTION_ATTEMPT : id123:3
     * - BYTES_SENT : 1234L
     * EXECUTION : id123
     * - EXECUTION_ATTEMPT : id123:1
     *   - BYTES_SENT : 1234L
     * - EXECUTION_ATTEMPT : id123:2
     *   - BYTES_SENT : 1234L
     * - EXECUTION_ATTEMPT : id123:3
     *   - BYTES_SENT : 1234L
     */
    @Test
    public void acceptanceTest() {
        Instant testStartTime = Instant.now();

        doAnswer(args -> {
            args.getArgumentAt(0, Subscription.class).request(Integer.MAX_VALUE);
            return null;
        }).when(mockSubscriber).onSubscribe(any(Subscription.class));

        Execution execution = new Execution(mockSubscriber);

        // Guard assertion - ensure nothing has happened yet
        verifyZeroInteractions(mockSubscriber);

        execution.execute();

        Instant testEndTime = Instant.now();

        InOrder inOrder = Mockito.inOrder(mockSubscriber);
        inOrder.verify(mockSubscriber).onSubscribe(any(Subscription.class));

        // Three attempts and one execution expected
        inOrder.verify(mockSubscriber, times(4)).onNext(sdkMetricCollectionCaptor.capture());

        inOrder.verify(mockSubscriber).onComplete();
        inOrder.verifyNoMoreInteractions();

        // The parent (execution) block will be the last to be published and will include the nested attempt blocks
        SdkMetricCollection executionCollection = sdkMetricCollectionCaptor.getAllValues().get(3);
        Instant executionStartTime = executionCollection.startTime();
        Instant executionEndTime = executionCollection.endTime();

        assertThat(executionStartTime).isBetween(testStartTime, testEndTime);
        assertThat(executionEndTime).isBetween(testStartTime, testEndTime);
        assertThat(executionEndTime).isAfter(executionStartTime);

        assertThat(executionCollection.context().type()).isEqualTo(SdkMetricTypes.EXECUTION);
        String executionIdentifier = executionCollection.context().identifier();
        assertThat(executionIdentifier).isNotEmpty();

        assertThat(executionCollection).hasSize(3);
        Iterator<SdkMetric<?>> executionCollectionIterator = executionCollection.iterator();
        Instant previousAttemptEnd = executionStartTime;

        for (int i = 1; i <= 3; i++) {
            SdkMetric<?> metric = executionCollectionIterator.next();
            assertThat(metric.context().type()).isEqualTo(SdkMetricTypes.EXECUTION_ATTEMPT);
            assertThat(metric.timestamp()).isBetween(executionStartTime, executionEndTime);

            SdkMetricCollection attemptCollection = (SdkMetricCollection)metric.value();

            verifyAttemptCollection(attemptCollection,
                                    executionIdentifier,
                                    i,
                                    executionStartTime,
                                    executionEndTime,
                                    previousAttemptEnd);

            previousAttemptEnd = attemptCollection.endTime();
        }

        // Test individually published execution attempt blocks
        previousAttemptEnd = executionStartTime;

        for (int i = 1; i <= 3; ++i) {
            SdkMetricCollection attemptCollection = sdkMetricCollectionCaptor.getAllValues().get(i - 1);
            verifyAttemptCollection(attemptCollection,
                                    executionIdentifier,
                                    i,
                                    executionStartTime,
                                    executionEndTime,
                                    previousAttemptEnd);
            previousAttemptEnd = attemptCollection.endTime();
        }
    }

    private void verifyAttemptCollection(SdkMetricCollection attemptCollection,
                                         String executionIdentifier,
                                         int attemptNumber,
                                         Instant executionStartTime,
                                         Instant executionEndTime,
                                         Instant previousAttemptEnd) {

            Instant attemptStartTime = attemptCollection.startTime();
            Instant attemptEndTime = attemptCollection.endTime();

            assertThat(attemptStartTime).isBetween(executionStartTime, executionEndTime);
            assertThat(attemptEndTime).isBetween(executionStartTime, executionEndTime);
            assertThat(attemptEndTime).isAfter(attemptStartTime);
            assertThat(attemptStartTime).isAfter(previousAttemptEnd);

            assertThat(attemptCollection.context().type()).isEqualTo(SdkMetricTypes.EXECUTION_ATTEMPT);
            assertThat(attemptCollection.context().identifier()).isEqualTo(executionIdentifier + ":" + attemptNumber);
            assertThat(attemptCollection).hasOnlyOneElementSatisfying(metric -> {
                assertThat(metric.context().type()).isEqualTo(SdkMetricTypes.BYTES_SENT);
                assertThat(metric.context().identifier()).isEqualTo(attemptCollection.context().identifier());
                assertThat(metric.value()).isEqualTo(1234L);
                assertThat(metric.timestamp()).isBetween(attemptStartTime, attemptEndTime);
            });
    }
}