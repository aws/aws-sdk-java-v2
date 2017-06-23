/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import software.amazon.awssdk.util.StringUtils;

/**
 * Asserts on the query parameters of the marshalled request.
 */
public class QueryParamsAssertion extends MarshallingAssertion {

    private Map<String, List<String>> contains;
    private List<String> doesNotContain;

    public void setContains(Map<String, List<String>> contains) {
        this.contains = contains;
    }

    public void setDoesNotContain(List<String> doesNotContain) {
        this.doesNotContain = doesNotContain;
    }

    @Override
    protected void doAssert(LoggedRequest actual) throws Exception {
        try {
            final Map<String, List<String>> actualParams = parseQueryParams(actual);
            doAssert(actualParams);
        } catch (AssertionError error) {
            // We may send the query params in the body if there is no other content. Try
            // decoding body as params and rerun the assertions.
            final Map<String, List<String>> actualParams = parseQueryParamsFromBody(
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
    }

    private Map<String, List<String>> parseQueryParamsFromBody(String body) {
        return toQueryParamMap(URLEncodedUtils.parse(body, StandardCharsets.UTF_8));
    }

    private Map<String, List<String>> parseQueryParams(LoggedRequest actual) {
        return toQueryParamMap(parseNameValuePairsFromQuery(actual));
    }

    /**
     * Group the list of {@link NameValuePair} by parameter name.
     */
    private Map<String, List<String>> toQueryParamMap(List<NameValuePair> queryParams) {
        return queryParams.stream().collect(Collectors.groupingBy(p -> p.getName(), Collectors
                .mapping(p -> p.getValue(), Collectors.toList())));
    }

    private List<NameValuePair> parseNameValuePairsFromQuery(LoggedRequest actual) {
        final String queryParams = URI.create(actual.getUrl()).getQuery();
        if (StringUtils.isNullOrEmpty(queryParams)) {
            return Collections.emptyList();
        }
        return URLEncodedUtils.parse(queryParams, StandardCharsets.UTF_8);
    }

    private void assertContains(Map<String, List<String>> actualParams) {
        contains.entrySet().forEach(e -> {
            assertThat(actualParams.get(e.getKey()), containsInAnyOrder(e.getValue().toArray()));
        });
    }

    private void assertDoesNotContain(Map<String, List<String>> actualParams) {
        doesNotContain.forEach(key -> {
            assertThat(actualParams, not(hasKey(key)));
        });
    }

}
