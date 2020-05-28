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

import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Map;

/**
 * Asserts on the headers in the marshalled request
 */
public class HeadersAssertion extends MarshallingAssertion {

    private Map<String, String> contains;

    private List<String> doesNotContain;

    public void setContains(Map<String, String> contains) {
        this.contains = contains;
    }

    public void setDoesNotContain(List<String> doesNotContain) {
        this.doesNotContain = doesNotContain;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        if (contains != null) {
            assertHeadersContains(actual.getHeaders());
        }
        if (doesNotContain != null) {
            assertDoesNotContainHeaders(actual.getHeaders());
        }
    }

    private void assertHeadersContains(HttpHeaders actual) {
        contains.entrySet().forEach(e -> {
            assertEquals(e.getValue(), actual.getHeader(e.getKey()).firstValue());
        });
    }

    private void assertDoesNotContainHeaders(HttpHeaders actual) {
        doesNotContain.forEach(headerName -> {
            assertFalse(String.format("Header '%s' was expected to be absent", headerName),
                        actual.getHeader(headerName).isPresent());
        });
    }
}
