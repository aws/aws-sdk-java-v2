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

package software.amazon.awssdk.s3.wiretap.server;

import java.util.List;
import java.util.function.Predicate;

public final class S3RequestFilters {

    private S3RequestFilters() {
    }

    public static Predicate<CapturedRequest> put() {
        return r -> "PUT".equals(r.method());
    }

    public static Predicate<CapturedRequest> simplePut() {
        return r -> "PUT".equals(r.method()) && !r.queryParameters().containsKey("partNumber");
    }

    public static Predicate<CapturedRequest> uploadPart() {
        return r -> "PUT".equals(r.method()) && r.queryParameters().containsKey("partNumber");
    }

    public static Predicate<CapturedRequest> createMultipartUpload() {
        return r -> "POST".equals(r.method()) && r.queryParameters().containsKey("uploads");
    }

    public static Predicate<CapturedRequest> completeMultipartUpload() {
        return r -> "POST".equals(r.method()) && r.queryParameters().containsKey("uploadId")
                && !r.queryParameters().containsKey("partNumber");
    }

    public static Predicate<CapturedRequest> abortMultipartUpload() {
        return r -> "DELETE".equals(r.method()) && r.queryParameters().containsKey("uploadId");
    }

    public static Predicate<CapturedRequest> listParts() {
        return r -> "GET".equals(r.method()) && r.queryParameters().containsKey("uploadId");
    }

    public static Predicate<CapturedRequest> listObjectsV2() {
        return r -> "GET".equals(r.method())
                && List.of("2").equals(r.queryParameters().get("list-type"));
    }

    public static Predicate<CapturedRequest> get() {
        return r -> "GET".equals(r.method());
    }

    public static Predicate<CapturedRequest> getObject() {
        return r -> "GET".equals(r.method()) && !r.queryParameters().containsKey("uploadId")
                && !r.queryParameters().containsKey("list-type");
    }

    public static Predicate<CapturedRequest> head() {
        return r -> "HEAD".equals(r.method());
    }
}
