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

import nl.altindag.log.LogCaptor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.transfer.s3.S3TransferManager;

public class TransferManagerLoggingTest {

    LogCaptor logCaptor;

    @BeforeEach
    void initLogCaptor() {
        logCaptor = LogCaptor.forClass(S3TransferManager.class);
    }

    @Test
    public void transferManager_withCrtClient_shouldNotLogMessages(){

        S3AsyncClient s3Crt = S3AsyncClient.crtCreate();
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3Crt).build();

        assertThat(logCaptor.getDebugLogs()).isEmpty();
        assertThat(logCaptor.getWarnLogs()).isEmpty();
    }

    @Test
    public void transferManager_withJavaClient_shouldLogWarnMessage(){

        S3AsyncClient s3Java = S3AsyncClient.create();
        S3TransferManager tm = S3TransferManager.builder().s3Client(s3Java).build();

        assertThat(logCaptor.getDebugLogs()).isEmpty();
        assertThat(logCaptor.getWarnLogs()).containsExactly("The provided DefaultS3AsyncClient is not an instance of "
                                                            + "S3CrtAsyncClient, and thus multipart upload/download feature is "
                                                            + "not enabled and resumable file upload is not supported. To benefit"
                                                            + " from maximum throughput, consider using "
                                                            + "S3AsyncClient.crtBuilder().build() instead.");


    }
}
