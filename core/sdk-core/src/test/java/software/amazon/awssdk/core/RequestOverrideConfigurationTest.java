/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core;


import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.util.ImmutableMapParameter;

public class RequestOverrideConfigurationTest {
    private static final String HEADER = "header";
    private static final String QUERY_PARAM = "queryparam";

    @Test
    public void addingSameItemTwice_shouldOverride() {
        RequestOverrideConfiguration configuration = SdkRequestOverrideConfiguration.builder()
                                                                                    .putHeader(HEADER, "foo")
                                                                                    .putHeader(HEADER, "bar")
                                                                                    .putRawQueryParameter(QUERY_PARAM, "foo")
                                                                                    .putRawQueryParameter(QUERY_PARAM, "bar")
                                                                                    .addApiName(a -> a.name("test1").version("1"))
                                                                                    .addApiName(a -> a.name("test2").version("2"))
                                                                                    .build();

        assertThat(configuration.headers().get(HEADER)).containsExactly("bar");
        assertThat(configuration.rawQueryParameters().get(QUERY_PARAM)).containsExactly("bar");
        assertThat(configuration.apiNames().size()).isEqualTo(2);
    }

    @Test
    public void settingCollection_shouldOverrideAddItem() {
        ImmutableMapParameter<String, List<String>> map =
            ImmutableMapParameter.of(HEADER, Arrays.asList("hello", "world"));
        ImmutableMapParameter<String, List<String>> queryMap =
            ImmutableMapParameter.of(QUERY_PARAM, Arrays.asList("hello", "world"));
        RequestOverrideConfiguration configuration = SdkRequestOverrideConfiguration.builder()
                                                                                    .putHeader(HEADER, "blah")
                                                                                    .headers(map)
                                                                                    .putRawQueryParameter(QUERY_PARAM, "blah")
                                                                                    .rawQueryParameters(queryMap)
                                                                                    .build();

        assertThat(configuration.headers().get(HEADER)).containsExactly("hello", "world");
        assertThat(configuration.rawQueryParameters().get(QUERY_PARAM)).containsExactly("hello", "world");
    }

    @Test
    public void addSameItemAfterSetCollection_shouldOverride() {
        ImmutableMapParameter<String, List<String>> map =
            ImmutableMapParameter.of(HEADER, Arrays.asList("hello", "world"));
        RequestOverrideConfiguration configuration = SdkRequestOverrideConfiguration.builder()
                                                                                    .headers(map)
                                                                                    .putHeader(HEADER, "blah")
                                                                                    .build();

        assertThat(configuration.headers().get(HEADER)).containsExactly("blah");
    }


    @Test
    public void shouldGuaranteeImmutability() {
        List<String> headerValues = new ArrayList<>();
        headerValues.add("bar");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", headerValues);

        SdkRequestOverrideConfiguration.Builder configurationBuilder =
            SdkRequestOverrideConfiguration.builder().headers(headers);

        headerValues.add("test");
        headers.put("new header", Collections.singletonList("new value"));

        assertThat(configurationBuilder.headers().size()).isEqualTo(1);
        assertThat(configurationBuilder.headers().get("foo")).containsExactly("bar");
    }
}
