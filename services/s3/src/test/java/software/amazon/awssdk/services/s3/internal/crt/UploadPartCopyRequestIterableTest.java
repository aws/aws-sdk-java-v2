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

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartCopyRequest;

class UploadPartCopyRequestIterableTest {

    @Test
    void threeParts_shouldCreateThreeRequests() {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                                                               .destinationKey("destination")
                                                               .destinationBucket("destinationBucket")
                                                               .sourceKey("source")
                                                               .sourceBucket("sourceBucket")
                                                               .build();
        UploadPartCopyRequestIterable uploadPartCopyRequests = new UploadPartCopyRequestIterable("id",
                                                                                                 1024L,
                                                                                                 copyObjectRequest,
                                                                                                 3000L);

        List<UploadPartCopyRequest> requests = uploadPartCopyRequests.stream().collect(Collectors.toList());
        assertThat(requests.size()).isEqualTo(3);
        assertThat(requests.get(0).copySourceRange()).isEqualTo("bytes=0-1023");
        assertThat(requests.get(1).copySourceRange()).isEqualTo("bytes=1024-2047");
        assertThat(requests.get(2).copySourceRange()).isEqualTo("bytes=2048-2999");
    }

    @Test
    void twoParts_shouldCreateTwoRequests() {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                                                               .destinationKey("destination")
                                                               .destinationBucket("destinationBucket")
                                                               .sourceKey("source")
                                                               .sourceBucket("sourceBucket")
                                                               .build();
        UploadPartCopyRequestIterable uploadPartCopyRequests = new UploadPartCopyRequestIterable("id",
                                                                                                 1024L,
                                                                                                 copyObjectRequest,
                                                                                                 1025L);

        List<UploadPartCopyRequest> requests = uploadPartCopyRequests.stream().collect(Collectors.toList());
        assertThat(requests.size()).isEqualTo(2);
        assertThat(requests.get(0).copySourceRange()).isEqualTo("bytes=0-1023");
        assertThat(requests.get(1).copySourceRange()).isEqualTo("bytes=1024-1024");
    }
}
