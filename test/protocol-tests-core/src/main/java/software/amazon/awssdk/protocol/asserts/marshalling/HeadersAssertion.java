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

package software.amazon.awssdk.protocol.asserts.marshalling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Asserts on the headers in the marshalled request
 */
public class HeadersAssertion extends MarshallingAssertion {

    private Map<String, List<String>> contains;

    private List<String> doesNotContain;
    private List<String> mustContain;

    public void setContains(Map<String, List<String>> contains) {
        this.contains = contains;
    }

    public void setDoesNotContain(List<String> doesNotContain) {
        this.doesNotContain = doesNotContain;
    }

    public void setMustContain(List<String> mustContain) {
        this.mustContain = mustContain;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        if (contains != null) {
            assertHeadersContains(actual.getHeaders());
        }
        if (doesNotContain != null) {
            assertDoesNotContainHeaders(actual.getHeaders());
        }
        if (mustContain != null) {
            assertMustContainHeaders(actual.getHeaders());
        }
    }

    private void assertHeadersContains(HttpHeaders actual) {
        contains.forEach((expectedKey, expectedValues) -> {
            assertTrue(String.format("Header '%s' was expected to be present. Actual headers: %s", expectedKey, actual),
                       actual.getHeader(expectedKey).isPresent());
            List<String> actualValues = actual.getHeader(expectedKey).values();
            if (expectedKey.equalsIgnoreCase("Content-Type") && actualValues.size() == 1) {
                actualValues = Collections.singletonList(actualValues.get(0).replace("; charset=UTF-8", ""));
            }
            assertEquals(expectedValues, actualValues);
        });
    }

    private void assertDoesNotContainHeaders(HttpHeaders actual) {
        doesNotContain.forEach(headerName -> {
            assertFalse(String.format("Header '%s' was expected to be absent", headerName),
                        actual.getHeader(headerName).isPresent());
        });
    }

    private void assertMustContainHeaders(HttpHeaders actual) {
        mustContain.forEach(headerName -> {
            assertTrue(String.format("Header '%s' was expected to be present", headerName),
                        actual.getHeader(headerName).isPresent());
        });
    }
}
