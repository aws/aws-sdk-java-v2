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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import software.amazon.awssdk.transfer.s3.internal.progress.DefaultTransferProgressSnapshot;
import software.amazon.awssdk.transfer.s3.progress.TransferProgressSnapshot;

public class DefaultTransferProgressSnapshotTest {
    @Test
    public void bytesTransferred_greaterThan_transferSize_shouldThrow() {
        DefaultTransferProgressSnapshot.Builder builder = DefaultTransferProgressSnapshot.builder()
                                                                                         .bytesTransferred(2)
                                                                                         .transferSizeInBytes(1L);
        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("bytesTransferred (2) must not be greater than transferSizeInBytes (1)");
    }

    @Test
    public void ratioTransferred_withoutTransferSize_isEmpty() {
        TransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder()
                                                                           .bytesTransferred(1)
                                                                           .build();
        assertThat(snapshot.ratioTransferred()).isNotPresent();
    }

    @Test
    public void ratioTransferred_withTransferSize_isCorrect() {
        TransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder()
                                                                           .bytesTransferred(1)
                                                                           .transferSizeInBytes(2L)
                                                                           .build();
        assertThat(snapshot.ratioTransferred()).hasValue(0.5);
    }

    @Test
    public void bytesRemainingTransferred_withoutTransferSize_isEmpty() {
        TransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder()
                                                                           .bytesTransferred(1)
                                                                           .build();
        assertThat(snapshot.bytesRemaining()).isNotPresent();
    }

    @Test
    public void bytesRemainingTransferred_withTransferSize_isCorrect() {
        TransferProgressSnapshot snapshot = DefaultTransferProgressSnapshot.builder()
                                                                           .bytesTransferred(1)
                                                                           .transferSizeInBytes(3L)
                                                                           .build();
        assertThat(snapshot.bytesRemaining()).hasValue(2L);
    }
}