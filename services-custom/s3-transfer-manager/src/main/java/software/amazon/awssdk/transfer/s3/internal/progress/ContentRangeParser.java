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

package software.amazon.awssdk.transfer.s3.internal.progress;

import java.util.OptionalLong;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Parse a Content-Range header value into a total byte count. The expected format is the following: <p></p>
 * {@code Content-Range: <unit> <range-start>-<range-end>\/<size>}<br>
 * {@code Content-Range: <unit> <range-start>-<range-end>\/*}<br> {@code Content-Range: <unit> *\/<size>}<p></p>
 * <p>
 * The only supported {@code <unit>} is the {@code bytes} value.
 */
@SdkInternalApi
public final class ContentRangeParser {

    private static final Logger log = Logger.loggerFor(ContentRangeParser.class);

    private ContentRangeParser() {
    }

    /**
     * Parse the Content-Range to extract the total number of byte from the content. Only supports the {@code bytes} unit, any
     * other unit will result in an empty OptionalLong. If the total length in unknown, which is represented by a {@code *} symbol
     * in the header value, an empty OptionalLong will be returned.
     *
     * @param contentRange the value of the Content-Range header to be parsed.
     * @return The total number of bytes in the content range or an empty optional if the contentRange is null, empty or if the
     * total length is not a valid long.
     */
    public static OptionalLong totalBytes(String contentRange) {
        if (StringUtils.isEmpty(contentRange)) {
            return OptionalLong.empty();
        }

        String trimmed = contentRange.trim();
        if (!trimmed.startsWith("bytes")) {
            return OptionalLong.empty();
        }

        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash == -1) {
            return OptionalLong.empty();
        }

        String totalBytes = trimmed.substring(lastSlash + 1);
        if ("*".equals(totalBytes)) {
            return OptionalLong.empty();
        }

        try {
            long value = Long.parseLong(totalBytes);
            return value > 0 ? OptionalLong.of(value) : OptionalLong.empty();
        } catch (NumberFormatException e) {
            log.warn(() -> "failed to parse content range", e);
            return OptionalLong.empty();
        }
    }
}
