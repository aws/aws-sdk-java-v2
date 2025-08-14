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

import static software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute.MULTIPART_DOWNLOAD_RESUME_CONTEXT;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Request;

@SdkInternalApi
public final class MultipartDownloadUtils {

    private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile("bytes\\s+(\\d+)-(\\d+)/(\\d+)");

    private MultipartDownloadUtils() {
    }

    /**
     * This method checks the
     * {@link software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute#MULTIPART_DOWNLOAD_RESUME_CONTEXT}
     * execution attributes for a context object and returns the complete parts associated with it, or an empty list of no
     * context is found.
     *
     * @param request
     * @return The list of completed parts for a GetObjectRequest, or an empty list if none were found.
     */
    public static List<Integer> completedParts(GetObjectRequest request) {
        return multipartDownloadResumeContext(request)
            .map(MultipartDownloadResumeContext::completedParts)
            .orElseGet(Collections::emptyList);
    }

    /**
     * This method checks the
     * {@link software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute#MULTIPART_DOWNLOAD_RESUME_CONTEXT}
     * execution attributes for a context object and returns it if it finds one. Otherwise, returns an empty Optional.
     *
     * @param request the request to look for execution attributes
     * @return the MultipartDownloadResumeContext if one is found, otherwise an empty Optional.
     */
    public static Optional<MultipartDownloadResumeContext> multipartDownloadResumeContext(GetObjectRequest request) {
        return request
            .overrideConfiguration()
            .flatMap(conf -> Optional.ofNullable(conf.executionAttributes().getAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT)));
    }

    /**
     * This method checks the
     * {@link software.amazon.awssdk.services.s3.multipart.S3MultipartExecutionAttribute#MULTIPART_DOWNLOAD_RESUME_CONTEXT}
     * execution attributes for a context object and returns it if it finds one. Otherwise, returns an empty Optional.
     *
     * @param request the request to look for execution attributes
     * @return the MultipartDownloadResumeContext if one is found, otherwise an empty Optional.
     */
    public static Optional<MultipartDownloadResumeContext> multipartDownloadResumeContext(S3Request request) {
        return request
            .overrideConfiguration()
            .flatMap(conf -> Optional.ofNullable(conf.executionAttributes().getAttribute(MULTIPART_DOWNLOAD_RESUME_CONTEXT)));
    }

    /**
     * Parses the start byte from a Content-Range header.
     * 
     * @param contentRange the Content-Range header value (e.g., "bytes 0-1023/2048")
     * @return the start byte position, or -1 if parsing fails
     */
    public static long parseStartByteFromContentRange(String contentRange) {
        if (contentRange == null) {
            return -1;
        }
        Matcher matcher = CONTENT_RANGE_PATTERN.matcher(contentRange);
        if (!matcher.matches()) {
            return -1;
        }
        return Long.parseLong(matcher.group(1));
    }

    /**
     * Parses the total size from a Content-Range header.
     * 
     * @param contentRange the Content-Range header value (e.g., "bytes 0-1023/2048")
     * @return the total size, or empty if parsing fails
     */
    public static Optional<Long> parseContentRangeForTotalSize(String contentRange) {
        if (contentRange == null) {
            return Optional.empty();
        }
        Matcher matcher = CONTENT_RANGE_PATTERN.matcher(contentRange);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(matcher.group(3)));
    }

}
