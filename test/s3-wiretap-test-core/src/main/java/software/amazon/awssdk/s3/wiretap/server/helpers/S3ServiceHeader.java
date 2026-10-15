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

package software.amazon.awssdk.s3.wiretap.server.helpers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import software.amazon.awssdk.s3.wiretap.server.Response;

public final class S3ServiceHeader {

    public static final Instant LAST_MODIFIED = Instant.parse("2025-09-01T00:00:00Z");

    public static final Set<String> NAMES =
            Set.of("date", "server", "x-amz-request-id", "x-amz-id-2", "last-modified");

    private static final DateTimeFormatter HTTP_DATE =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                    .withZone(ZoneOffset.UTC);

    private S3ServiceHeader() {
    }

    public static Response.ResponseBuilder applyWithLastModified(Response.ResponseBuilder builder) {
        return apply(builder).header("Last-Modified", HTTP_DATE.format(LAST_MODIFIED));
    }

    public static Response.ResponseBuilder apply(Response.ResponseBuilder builder) {
        return builder
                .header("Date", HTTP_DATE.format(LAST_MODIFIED))
                .header("Server", "AmazonS3")
                .header("x-amz-request-id", "WIRETAPMOCKREQID0")
                .header("x-amz-id-2", "aGVpbWRhbGwtbW9jay1pZC0yLXBsYWNlaG9sZGVyLXZhbHVl");
    }
}
