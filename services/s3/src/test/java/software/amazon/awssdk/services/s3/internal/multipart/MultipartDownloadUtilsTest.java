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
    void parseStartByteFromContentRange_shouldParseValidAndInvalidRanges() {
        assertThat(MultipartDownloadUtils.parseStartByteFromContentRange("bytes 0-1023/2048")).isEqualTo(0);
        assertThat(MultipartDownloadUtils.parseStartByteFromContentRange("bytes 1024-2047/2048")).isEqualTo(1024);

        assertThat(MultipartDownloadUtils.parseStartByteFromContentRange("invalid")).isEqualTo(-1);
        assertThat(MultipartDownloadUtils.parseStartByteFromContentRange(null)).isEqualTo(-1);
    }

    @Test
    void parseContentRangeForTotalSize_shouldParseValidAndInvalidRanges() {
        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize("bytes 0-1023/2048")).contains(2048L);
        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize("bytes 0-0/1")).contains(1L);

        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize("invalid")).isEmpty();
        assertThat(MultipartDownloadUtils.parseContentRangeForTotalSize(null)).isEmpty();
    }
}