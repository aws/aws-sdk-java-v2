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
import static org.junit.Assert.assertThat;
import static software.amazon.awssdk.protocol.asserts.marshalling.QueryUtils.parseQueryParams;
import static software.amazon.awssdk.protocol.asserts.marshalling.QueryUtils.parseQueryParamsFromBody;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryBodyAssertion extends MarshallingAssertion  {
    private final String queryEquals;

    public QueryBodyAssertion(String queryEquals) {
        this.queryEquals = queryEquals;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        Map<String, List<String>> expectedParams = parseQueryParamsFromBody(queryEquals);
        try {
            Map<String, List<String>> actualParams = parseQueryParams(actual);
            doAssert(expectedParams, actualParams);
        } catch (AssertionError error) {
            // We may send the query params in the body if there is no other content. Try
            // decoding body as params and rerun the assertions.
            Map<String, List<String>> actualParams = parseQueryParamsFromBody(
                actual.getBodyAsString());
            doAssert(expectedParams, actualParams);
        }
    }

    private void doAssert(Map<String, List<String>> expectedParams, Map<String, List<String>> actualParams) {
        assertThat(actualParams.keySet(), equalTo(expectedParams.keySet()));
        expectedParams.forEach((key, value) -> assertParamsEqual(actualParams.get(key), value));
    }

    private void assertParamsEqual(List<String> actual, List<String> expected) {
        if (expected.stream().allMatch(QueryBodyAssertion::isNumeric)) {
            assertThat(
                actual.stream().map(Double::parseDouble).collect(Collectors.toList()),
                containsInAnyOrder(expected.stream().map(Double::parseDouble).toArray()));
        } else {
            assertThat(actual, containsInAnyOrder(expected.toArray()));
        }
    }

    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


}
