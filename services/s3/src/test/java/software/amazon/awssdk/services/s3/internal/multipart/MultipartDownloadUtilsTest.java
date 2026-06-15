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
import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

class MultipartDownloadUtilsTest {

    @Test
    void noContext_completedPartShouldBeEmpty() {
        GetObjectRequest req = GetObjectRequest.builder().build();
        assertThat(MultipartDownloadUtils.completedParts(req)).isEmpty();
    }

    @Test
    void noContext_contextShouldBeEmpty() {
        GetObjectRequest req = GetObjectRequest.builder().build();
        assertThat(MultipartDownloadUtils.multipartDownloadResumeContext(req)).isEmpty();
    }

    @Test
    void contextWithParts_completedPartsShouldReturnListOfParts() {
        MultipartDownloadResumeContext ctx = new MultipartDownloadResumeContext();
        ctx.addCompletedPart(1);
        ctx.addCompletedPart(2);
        ctx.addCompletedPart(3);
        GetObjectRequest req = GetObjectRequest
            .builder()
            .overrideConfiguration(conf -> conf.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT, ctx))
            .build();

        assertThat(MultipartDownloadUtils.completedParts(req)).containsExactly(1, 2, 3);
    }

    @Test
    void contextWithParts_contextShouldBePresent() {
        MultipartDownloadResumeContext ctx = new MultipartDownloadResumeContext();
        GetObjectRequest req = GetObjectRequest
            .builder()
            .overrideConfiguration(conf -> conf.putExecutionAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT, ctx))
            .build();

        assertThat(MultipartDownloadUtils.multipartDownloadResumeContext(req)).isPresent();
    }

    @Test
    void parseContentRange_shouldParseValidAndInvalidRanges() {
        long[] result = MultipartDownloadUtils.parseContentRange("bytes 0-1023/2048");
        assertThat(result).isNotNull();
        assertThat(result[0]).isEqualTo(0);
        assertThat(result[1]).isEqualTo(1023);

        result = MultipartDownloadUtils.parseContentRange("bytes 1024-2047/2048");
        assertThat(result[0]).isEqualTo(1024);
        assertThat(result[1]).isEqualTo(2047);

        assertThat(MultipartDownloadUtils.parseContentRange("invalid")).isNull();
        assertThat(MultipartDownloadUtils.parseContentRange(null)).isNull();
    }

    @Test
    void parseContentRangeForTotalSize_shouldParseValidAndInvalidRanges() {
        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize("bytes 0-1023/2048")).contains(2048L);
        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize("bytes 0-0/1")).contains(1L);

        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize("invalid")).isEmpty();
        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize(null)).isEmpty();
    }

    @Test
    void calculateTotalParts_shouldCalculateCorrectly() {
        assertThat(MultipartDownloadUtils.calculateTotalParts(32, 16)).isEqualTo(2);   // exact fit
        assertThat(MultipartDownloadUtils.calculateTotalParts(33, 16)).isEqualTo(3);   // remainder rounds up
        assertThat(MultipartDownloadUtils.calculateTotalParts(1, 16)).isEqualTo(1);    // smaller than part size
        assertThat(MultipartDownloadUtils.calculateTotalParts(16, 16)).isEqualTo(1);   // exactly one part
        assertThat(MultipartDownloadUtils.calculateTotalParts(0, 16)).isEqualTo(0);    // empty object
        // 5 GiB / 1 byte = 5_368_709_120 parts (exceeds Integer.MAX_VALUE)
        long fiveGiB = 5L * 1024L * 1024L * 1024L;
        assertThat(MultipartDownloadUtils.calculateTotalParts(fiveGiB, 1L)).isEqualTo(fiveGiB);
        assertThat(MultipartDownloadUtils.calculateTotalParts(Long.MAX_VALUE - 1, 1L))
            .isEqualTo(Long.MAX_VALUE - 1);
        assertThat(MultipartDownloadUtils.calculateTotalParts(Long.MAX_VALUE, 2))
            .isEqualTo((Long.MAX_VALUE / 2) + 1);
    }
}
