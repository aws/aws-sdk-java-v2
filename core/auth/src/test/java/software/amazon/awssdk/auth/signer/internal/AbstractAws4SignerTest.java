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

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class AbstractAws4SignerTest {

    @Test
    public void testAppendCompactedString() {
        StringBuilder destination = new StringBuilder();
        AbstractAws4Signer.appendCompactedString(destination, "    a   b   c  ");
        String expected = "a b c";
        assertEquals(expected, destination.toString());
    }

    @Test
    public void testGetCanonicalizedHeaderString_SpecExample() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Host", Arrays.asList("iam.amazonaws.com"));
        headers.put("Content-Type", Arrays.asList("application/x-www-form-urlencoded; charset=utf-8"));
        headers.put("My-header1", Arrays.asList("    a   b   c  "));
        headers.put("X-Amz-Date", Arrays.asList("20150830T123600Z"));
        headers.put("My-Header2", Arrays.asList("    \"a   b   c\"  "));

        Map<String, List<String>> canonicalizeSigningHeaders =
                AbstractAws4Signer.canonicalizeSigningHeaders(headers);

        String actual = AbstractAws4Signer.getCanonicalizedHeaderString(canonicalizeSigningHeaders);

        String expected = String.join("\n",
                "content-type:application/x-www-form-urlencoded; charset=utf-8",
                "host:iam.amazonaws.com",
                "my-header1:a b c",
                "my-header2:\"a b c\"",
                "x-amz-date:20150830T123600Z",
                "");

        assertEquals(expected, actual);
    }

    @Test
    public void testGetCanonicalizedHeaderString_MultipleHeaderValuess() {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Host", Arrays.asList("iam.amazonaws.com"));
        headers.put("Content-Type", Arrays.asList("application/x-www-form-urlencoded; charset=utf-8"));
        headers.put("My-header1", Arrays.asList("    a   b   c  "));
        headers.put("X-Amz-Date", Arrays.asList("20150830T123600Z"));
        headers.put("My-Header1", Arrays.asList("    \"a   b   c\"  "));

        Map<String, List<String>> canonicalizeSigningHeaders =
                AbstractAws4Signer.canonicalizeSigningHeaders(headers);

        String actual = AbstractAws4Signer.getCanonicalizedHeaderString(canonicalizeSigningHeaders);

        String expected = String.join("\n",
                "content-type:application/x-www-form-urlencoded; charset=utf-8",
                "host:iam.amazonaws.com",
                "my-header1:a b c,\"a b c\"",
                "x-amz-date:20150830T123600Z",
                "");

        assertEquals(expected, actual);
    }

}
