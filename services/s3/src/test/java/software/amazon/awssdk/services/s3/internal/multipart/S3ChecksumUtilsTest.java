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

package software.amazon.awssdk.services.s3.internal.multipart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3ChecksumUtilsTest {

    @Test
    public void checksumValueSpecified_requestWithoutChecksum_returnsFalse() {
        boolean checksumValueSpecified = S3ChecksumUtils.checksumValueSpecified(putObjectRequestBuilder().build());
        assertThat(checksumValueSpecified).isFalse();
    }

    @Test
    public void checksumValueSpecified_requestWithChecksum_returnsTrue() {
        boolean checksumValueSpecified = S3ChecksumUtils.checksumValueSpecified(putObjectRequestBuilder()
                                                                                    .checksumCRC64NVME("val")
                                                                                    .build());
        assertThat(checksumValueSpecified).isTrue();
    }

    @Test
    public void setChecksumAlgorithm_requestWithoutChecksum_doesNotSetAlgoOnCreateMpu() {
        PutObjectRequest putObject = putObjectRequestBuilder().build();
        assertThat(S3ChecksumUtils.checksumAlgorithmFromPutObjectRequest(putObject))
            .isEmpty();
    }

    @Test
    public void setChecksumAlgorithm_requestWithChecksum_setsAlgoOnCreateMpu() {
        PutObjectRequest putObject = putObjectRequestBuilder().checksumCRC64NVME("val").build();
        assertThat(S3ChecksumUtils.checksumAlgorithmFromPutObjectRequest(putObject))
            .contains(ChecksumAlgorithm.CRC64_NVME);
    }
    
    private PutObjectRequest.Builder putObjectRequestBuilder() {
        return PutObjectRequest.builder().bucket("bucket").key("key");
    }

    private CreateMultipartUploadRequest.Builder createMultipartUploadRequestBuilder() {
        return CreateMultipartUploadRequest.builder().bucket("bucket").key("key");
    }
}
