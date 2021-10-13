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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class TransferProgressSnapshotTest {
    @Test
    public void totalBytesTransferred_greaterThan_totalTransferSize_shouldThrow() {
        TransferProgressSnapshot.Builder builder = TransferProgressSnapshot.builder()
                                                                           .totalBytesTransferred(2)
                                                                           .totalTransferSize(1);
        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("totalBytesTransferred (2) must not be greater than totalTransferSize (1)");
    }

    @Test
    public void ratioTransferred_withoutTransferSize_isEmpty() {
        TransferProgressSnapshot snapshot = TransferProgressSnapshot.builder()
                                                                    .totalBytesTransferred(1)
                                                                    .build();
        assertThat(snapshot.ratioTransferred()).isNotPresent();
    }

    @Test
    public void ratioTransferred_withTransferSize_isCorrect() {
        TransferProgressSnapshot snapshot = TransferProgressSnapshot.builder()
                                                                    .totalBytesTransferred(1)
                                                                    .totalTransferSize(2)
                                                                    .build();
        assertThat(snapshot.ratioTransferred()).hasValue(0.5);
    }

    @Test
    public void percentageTransferred_withoutTransferSize_isEmpty() {
        TransferProgressSnapshot snapshot = TransferProgressSnapshot.builder()
                                                                    .totalBytesTransferred(1)
                                                                    .build();
        assertThat(snapshot.percentageTransferred()).isNotPresent();
    }

    @Test
    public void percentageTransferred_withTransferSize_isCorrect() {
        TransferProgressSnapshot snapshot = TransferProgressSnapshot.builder()
                                                                    .totalBytesTransferred(1)
                                                                    .totalTransferSize(2)
                                                                    .build();
        assertThat(snapshot.percentageTransferred()).hasValue(50.0);
    }

    @Test
    public void bytesRemainingTransferred_withoutTransferSize_isEmpty() {
        TransferProgressSnapshot snapshot = TransferProgressSnapshot.builder()
                                                                    .totalBytesTransferred(1)
                                                                    .build();
        assertThat(snapshot.totalBytesRemaining()).isNotPresent();
    }

    @Test
    public void bytesRemainingTransferred_withTransferSize_isCorrect() {
        TransferProgressSnapshot snapshot = TransferProgressSnapshot.builder()
                                                                    .totalBytesTransferred(1)
                                                                    .totalTransferSize(3)
                                                                    .build();
        assertThat(snapshot.totalBytesRemaining()).hasValue(2L);
    }
}