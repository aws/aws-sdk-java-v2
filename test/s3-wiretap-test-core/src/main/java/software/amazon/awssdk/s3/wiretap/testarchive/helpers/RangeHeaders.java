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

package software.amazon.awssdk.s3.wiretap.testarchive.helpers;

public final class RangeHeaders {

    private RangeHeaders() {
    }

    public static long start(String header) {
        String spec = spec(header);
        return Long.parseLong(spec.substring(0, spec.indexOf('-')));
    }

    public static long end(String header) {
        String spec = spec(header);
        return Long.parseLong(spec.substring(spec.indexOf('-') + 1));
    }

    private static String spec(String header) {
        if (header == null || !header.startsWith("bytes=") || !header.contains("-")) {
            throw new IllegalArgumentException("not a bytes=a-b range header: " + header);
        }
        return header.substring("bytes=".length());
    }
}
