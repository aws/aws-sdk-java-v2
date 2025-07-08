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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static software.amazon.awssdk.protocol.asserts.marshalling.QueryUtils.parseQueryParams;
import static software.amazon.awssdk.protocol.asserts.marshalling.QueryUtils.parseQueryParamsFromBody;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Map;

/**
 * Asserts on the query parameters of the marshalled request.
 */
public class QueryParamsAssertion extends MarshallingAssertion {

    private Map<String, List<String>> contains;
    private Map<String, List<String>> containsOnly;
    private List<String> doesNotContain;
    private List<String> mustContain;

    public void setContains(Map<String, List<String>> contains) {
        this.contains = contains;
    }

    public void setContainsOnly(Map<String, List<String>> containsOnly) {
        this.containsOnly = containsOnly;
    }

    public void setDoesNotContain(List<String> doesNotContain) {
        this.doesNotContain = doesNotContain;
    }

    public void setMustContain(List<String> mustContain) {
        this.mustContain = mustContain;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        try {
            Map<String, List<String>> actualParams = parseQueryParams(actual);
            doAssert(actualParams);
        } catch (AssertionError error) {
            // We may send the query params in the body if there is no other content. Try
            // decoding body as params and rerun the assertions.
            Map<String, List<String>> actualParams = parseQueryParamsFromBody(
                    actual.getBodyAsString());
            doAssert(actualParams);
        }
    }

    private void doAssert(Map<String, List<String>> actualParams) {
        if (contains != null) {
            assertContains(actualParams);
        }

        if (doesNotContain != null) {
            assertDoesNotContain(actualParams);
        }

        if (containsOnly != null) {
            assertContainsOnly(actualParams);
        }

        if (mustContain != null) {
            assertMustContain(actualParams);
        }
    }


    private void assertContains(Map<String, List<String>> actualParams) {
        contains.entrySet().forEach(e -> assertThat(actualParams.get(e.getKey()), containsInAnyOrder(e.getValue().toArray())));
    }

    private void assertDoesNotContain(Map<String, List<String>> actualParams) {
        doesNotContain.forEach(key -> assertThat(actualParams, not(hasKey(key))));
    }

    private void assertMustContain(Map<String, List<String>> actualParams) {
        doesNotContain.forEach(key -> assertThat(actualParams, hasKey(key)));
    }

    private void assertContainsOnly(Map<String, List<String>> actualParams) {
        assertThat(actualParams.keySet(), equalTo(containsOnly.keySet()));
        containsOnly.entrySet().forEach(e -> assertThat(
            actualParams.get(e.getKey()), containsInAnyOrder(e.getValue().toArray())
        ));
    }

}
