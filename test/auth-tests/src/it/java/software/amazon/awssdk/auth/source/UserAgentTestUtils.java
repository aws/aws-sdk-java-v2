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

package software.amazon.awssdk.auth.source;

import static org.assertj.core.api.Assertions.assertThat;

import software.amazon.awssdk.core.useragent.BusinessMetricCollection;

public final class UserAgentTestUtils {
    private UserAgentTestUtils() {
    }

    /**
     * Assert that the user-agent has the given feature IDs. Note that this does *not* assert that only the given feature IDs
     * are present (i.e. other IDs may be in the UA).
     */
    public static void assertUserAgentHasFeatureIds(String userAgent, Iterable<String> featureIds) {
        for (String featureId : featureIds) {
            assertThat(userAgent).matches(BusinessMetricCollection.METRIC_SEARCH_PATTERN.apply(featureId));
        }
    }
}
