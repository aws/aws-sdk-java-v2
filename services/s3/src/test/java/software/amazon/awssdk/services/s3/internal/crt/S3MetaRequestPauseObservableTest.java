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

package software.amazon.awssdk.services.s3.internal.crt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.crt.s3.ResumeToken;

public class S3MetaRequestPauseObservableTest {
    @Test
    void pause_notYetSubscribed_returnsNull() {
        S3MetaRequestPauseObservable observable = new S3MetaRequestPauseObservable();
        assertThat(observable.pause()).isNull();
    }

    @Test
    void pauseAsync_notYetSubscribed_returnsFutureCompletedWithNull() {
        S3MetaRequestPauseObservable observable = new S3MetaRequestPauseObservable();
        assertThat(observable.pauseAsync()).isCompletedWithValue(null);
    }

    @Test
    void pauseAsync_subscribed_delegatesToMetaRequest() {
        ResumeToken token = new ResumeToken(new ResumeToken.PutResumeTokenBuilder().withUploadId("id"));
        S3MetaRequestWrapper metaRequest = mock(S3MetaRequestWrapper.class);
        when(metaRequest.pauseAsync()).thenReturn(CompletableFuture.completedFuture(token));

        S3MetaRequestPauseObservable observable = new S3MetaRequestPauseObservable();
        observable.subscribe(metaRequest);

        assertThat(observable.pauseAsync()).isCompletedWithValue(token);
    }
}
