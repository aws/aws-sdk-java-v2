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


package software.amazon.awssdk.auth.signer.internal.util;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeaderTransformsHelperTest {

    @Test
    void shouldExcludeIgnoredHeadersWhenCanonicalizing() {
        Map<String, List<String>> headers = new HashMap<>();

        // Ignored headers that should be excluded from signing
        headers.put("connection", Collections.singletonList("keep-alive"));
        headers.put("x-amzn-trace-id", Collections.singletonList("Root=1-234567890"));
        headers.put("user-agent", Collections.singletonList("md/user"));
        headers.put("expect", Collections.singletonList("100-continue"));
        headers.put("transfer-encoding", Collections.singletonList("chunked"));

        // Headers that should be included in signing
        headers.put("Content-Type", Collections.singletonList("application/json"));
        headers.put("Host", Collections.singletonList("example.com"));

        Map<String, List<String>> canonicalizedHeaders = HeaderTransformsHelper.canonicalizeSigningHeaders(headers);

        assertEquals(2, canonicalizedHeaders.size(), "Should only contain non-ignored headers");

        // Verify included headers
        assertTrue(canonicalizedHeaders.containsKey("content-type"), "Should contain content-type header");
        assertTrue(canonicalizedHeaders.containsKey("host"), "Should contain host header");

        // Verify excluded headers
        assertFalse(canonicalizedHeaders.containsKey("connection"), "Should not contain connection header");
        assertFalse(canonicalizedHeaders.containsKey("x-amzn-trace-id"), "Should not contain x-amzn-trace-id header");
        assertFalse(canonicalizedHeaders.containsKey("user-agent"), "Should not contain user-agent header");
        assertFalse(canonicalizedHeaders.containsKey("expect"), "Should not contain expect header");
        assertFalse(canonicalizedHeaders.containsKey("transfer-encoding"), "Should not contain transfer-encoding header");
    }

}
