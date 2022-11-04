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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.signer.internal.AbstractAws4Signer.CanonicalRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.ToString;

/**
 * Tests how canonical resource paths are created including normalization
 */
public class Aws4SignerPathNormalizationTest {
    public static Iterable<TestCase> data() {
        return Arrays.asList(
            // Handling slash
            tc("Empty path -> (initial) slash added", "", "/"),
            tc("Slash -> unchanged", "/", "/"),
            tc("Single segment with initial slash -> unchanged", "/foo", "/foo"),
            tc("Single segment no slash -> slash prepended", "foo", "/foo"),
            tc("Multiple segments -> unchanged", "/foo/bar", "/foo/bar"),
            tc("Multiple segments with trailing slash -> unchanged", "/foo/bar/", "/foo/bar/"),

            // Double URL encoding
            tc("Multiple segments, urlEncoded slash -> encodes percent", "/foo%2Fbar", "/foo%252Fbar", true, true),

            // No double-url-encoding + normalization
            tc("Single segment, dot -> should remove dot", "/.", "/"),
            tc("Single segment, double dot -> unchanged", "/..", "/.."),
            tc("Multiple segments with dot -> should remove dot", "/foo/./bar", "/foo/bar"),
            tc("Multiple segments with ending dot -> should remove dot and trailing slash", "/foo/bar/.", "/foo/bar"),
            tc("Multiple segments with dots -> should remove dots and preceding segment", "/foo/bar/../baz", "/foo/baz"),
            tc("First segment has colon -> unchanged, url encoded first", "foo:/bar", "/foo%3A/bar", true, true),

            // Double-url-encoding + normalization
            tc("Multiple segments, urlEncoded slash -> encodes percent", "/foo%2F.%2Fbar", "/foo%252F.%252Fbar", true, true),

            // Double-url-encoding + no normalization
            tc("No url encode, Multiple segments with dot -> unchanged", "/foo/./bar", "/foo/./bar", false, false),
            tc("Multiple segments with dots -> unchanged", "/foo/bar/../baz", "/foo/bar/../baz", false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void verifyNormalizedPath(TestCase tc) {
        String canonicalRequest = tc.canonicalRequest.string();
        String[] requestParts = canonicalRequest.split("\\n");
        String canonicalPath = requestParts[1];
        assertEquals(tc.expectedPath, canonicalPath);
    }

    private static TestCase tc(String name, String path, String expectedPath) {
        return new TestCase(name, path, expectedPath, false, true);
    }

    private static TestCase tc(String name, String path, String expectedPath, boolean urlEncode, boolean normalizePath) {
        return new TestCase(name, path, expectedPath, urlEncode, normalizePath);
    }

    private static class TestCase {
        private String name;
        private String path;
        private String expectedPath;
        private CanonicalRequest canonicalRequest;

        public TestCase(String name,
                        String path,
                        String expectedPath,
                        boolean urlEncode,
                        boolean normalizePath) {
            SdkHttpFullRequest request = SdkHttpFullRequest.builder()
                                                           .protocol("https")
                                                           .host("localhost")
                                                           .encodedPath(path)
                                                           .method(SdkHttpMethod.PUT)
                                                           .build();
            this.name = name;
            this.path = path;
            this.expectedPath = expectedPath;
            this.canonicalRequest = new CanonicalRequest(request, request.toBuilder(), "sha-256", urlEncode, normalizePath);
        }

        @Override
        public String toString() {
            return ToString.builder("TestCase")
                           .add("name", name)
                           .add("path", path)
                           .add("expectedPath", expectedPath)
                           .build();
        }
    }
}
