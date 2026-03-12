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

package software.amazon.awssdk.services.sqs.batchmanager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

class SqsAsyncBatchManagerBuilderTest {


    static Stream<Arguments> incompleteBuilders() {
        return Stream.of(
            Arguments.of(SqsAsyncBatchManager.builder()
                                             .client(null)
                                             .scheduledExecutor(Executors.newScheduledThreadPool(2)),
                         "client cannot be null"),
            Arguments.of(SqsAsyncBatchManager.builder()
                                             .client(mock(SqsAsyncClient.class))
                                             .scheduledExecutor(null),
                         "scheduledExecutor cannot be null")
        );
    }

    @ParameterizedTest
    @MethodSource("incompleteBuilders")
    void testIncompleteBuilders(SqsAsyncBatchManager.Builder builder, String errorMessage) {
        assertThatExceptionOfType(NullPointerException.class)
            .isThrownBy(builder::build)
            .withMessage(errorMessage);

    }

    @Test
    void testCompleteBuilder() {
        SqsAsyncBatchManager sqsAsyncBatchManager = SqsAsyncBatchManager.builder()
                                                                        .client(mock(SqsAsyncClient.class))
                                                                        .scheduledExecutor(mock(ScheduledExecutorService.class))
                                                                        .build();
        assertThat(sqsAsyncBatchManager).isNotNull();

    }
}
