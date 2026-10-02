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

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import software.amazon.awssdk.utils.StringUtils;

public final class QueryUtils {
    private QueryUtils() {
    }

    public static Map<String, List<String>> parseQueryParamsFromBody(String body) {
        return toQueryParamMap(URLEncodedUtils.parse(body, StandardCharsets.UTF_8));
    }

    public static Map<String, List<String>> parseQueryParams(LoggedRequest actual) {
        return toQueryParamMap(parseNameValuePairsFromQuery(actual));
    }

    /**
     * Group the list of {@link NameValuePair} by parameter name.
     */
    private static Map<String, List<String>> toQueryParamMap(List<NameValuePair> queryParams) {
        return queryParams.stream().collect(Collectors.groupingBy(NameValuePair::getName, Collectors
            .mapping(NameValuePair::getValue, Collectors.toList())));
    }

    private static List<NameValuePair> parseNameValuePairsFromQuery(LoggedRequest actual) {
        String queryParams = URI.create(actual.getUrl()).getQuery();
        if (StringUtils.isEmpty(queryParams)) {
            return Collections.emptyList();
        }
        return URLEncodedUtils.parse(queryParams, StandardCharsets.UTF_8);
    }
}
