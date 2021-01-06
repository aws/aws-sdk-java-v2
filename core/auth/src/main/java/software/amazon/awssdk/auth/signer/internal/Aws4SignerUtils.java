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

package software.amazon.awssdk.auth.signer.internal;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.http.SdkHttpFullRequest;

/**
 * Utility methods that is used by the different AWS Signer implementations.
 * This class is strictly internal and is subjected to change.
 */
@SdkInternalApi
public final class Aws4SignerUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd").withZone(ZoneId.of("UTC"));

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneId.of("UTC"));

    private Aws4SignerUtils() {
    }

    /**
     * Returns a string representation of the given date time in yyyyMMdd
     * format. The date returned is in the UTC zone.
     *
     * For example, given a time "1416863450581", this method returns "20141124"
     */
    public static String formatDateStamp(long timeMilli) {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timeMilli));
    }

    public static String formatDateStamp(Instant instant) {
        return DATE_FORMATTER.format(instant);
    }

    /**
     * Returns a string representation of the given date time in
     * yyyyMMdd'T'HHmmss'Z' format. The date returned is in the UTC zone.
     *
     * For example, given a time "1416863450581", this method returns
     * "20141124T211050Z"
     */
    public static String formatTimestamp(long timeMilli) {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(timeMilli));
    }

    public static String formatTimestamp(Instant instant) {
        return TIME_FORMATTER.format(instant);
    }

    /**
     * Calculates the content length of a request. If the content-length isn't in the header,
     * the method reads the whole input stream to get the length.
     */
    public static long calculateRequestContentLength(SdkHttpFullRequest.Builder mutableRequest) {
        String contentLength = mutableRequest.firstMatchingHeader(Header.CONTENT_LENGTH)
                                             .orElse(null);
        long originalContentLength;
        if (contentLength != null) {
            originalContentLength = Long.parseLong(contentLength);
        } else {
            try {
                originalContentLength = getContentLength(mutableRequest.contentStreamProvider().newStream());
            } catch (IOException e) {
                throw SdkClientException.builder()
                                        .message("Cannot get the content-length of the request content.")
                                        .cause(e)
                                        .build();
            }
        }
        return originalContentLength;
    }

    /**
     * Read a stream to get the length.
     */
    private static long getContentLength(InputStream content) throws IOException {
        long contentLength = 0;
        byte[] tmp = new byte[4096];
        int read;
        while ((read = content.read(tmp)) != -1) {
            contentLength += read;
        }
        return contentLength;
    }
}
