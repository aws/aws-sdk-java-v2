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

package software.amazon.awssdk.retries.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

public class RetryStrategyTest {
    @Test
    void acquireInitialTokenAsync_syncThrows_reportedThroughFuture() {
        RetryStrategy retryStrategy = mock(RetryStrategy.class);

        when(retryStrategy.acquireInitialTokenAsync(any(AcquireInitialTokenRequest.class)))
            .thenCallRealMethod();

        RuntimeException t = new RuntimeException("oops");
        when(retryStrategy.acquireInitialToken(any(AcquireInitialTokenRequest.class)))
            .thenThrow(t);

        assertThatThrownBy(() -> retryStrategy.acquireInitialTokenAsync(AcquireInitialTokenRequest.create("test")).join())
            .hasRootCause(t);
    }

    @Test
    void refreshRetryTokenAsync_syncThrows_reportedThroughFuture() {
        RetryStrategy retryStrategy = mock(RetryStrategy.class);

        when(retryStrategy.refreshRetryTokenAsync(any(RefreshRetryTokenRequest.class)))
            .thenCallRealMethod();

        RuntimeException t = new RuntimeException("oops");
        when(retryStrategy.refreshRetryToken(any(RefreshRetryTokenRequest.class)))
            .thenThrow(t);

        RetryToken token = mock(RetryToken.class);
        RefreshRetryTokenRequest request = RefreshRetryTokenRequest.builder()
                                                                   .token(token)
                                                                   .failure(new RuntimeException("failure"))
                                                                   .build();

        assertThatThrownBy(() -> retryStrategy.refreshRetryTokenAsync(request).join())
            .hasRootCause(t);
    }
}
