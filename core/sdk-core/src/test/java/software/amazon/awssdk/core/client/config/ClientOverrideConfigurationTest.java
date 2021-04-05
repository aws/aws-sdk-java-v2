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

package software.amazon.awssdk.core.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.internal.http.request.SlowExecutionInterceptor;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.utils.ImmutableMap;

public class ClientOverrideConfigurationTest {

    @Test
    public void addingSameItemTwice_shouldOverride() {
        ClientOverrideConfiguration configuration = ClientOverrideConfiguration.builder()
                                                                               .putHeader("value", "foo")
                                                                               .putHeader("value", "bar")
                                                                               .putAdvancedOption(SdkAdvancedClientOption
                                                                                                      .USER_AGENT_SUFFIX, "foo")
                                                                               .putAdvancedOption(SdkAdvancedClientOption
                                                                                                      .USER_AGENT_SUFFIX, "bar")
                                                                               .build();

        assertThat(configuration.headers().get("value")).containsExactly("bar");
        assertThat(configuration.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX).get()).isEqualTo("bar");

        ClientOverrideConfiguration anotherConfig = configuration.toBuilder().putHeader("value", "foobar").build();
        assertThat(anotherConfig.headers().get("value")).containsExactly("foobar");
    }

    @Test
    public void settingCollection_shouldOverrideAddItem() {
        ClientOverrideConfiguration configuration = ClientOverrideConfiguration.builder()
                                                                               .putHeader("value", "foo")
                                                                               .headers(ImmutableMap.of("value",
                                                                                                        Arrays.asList
                                                                                                                     ("hello",
                                                                                                                      "world")))
                                                                               .putAdvancedOption(SdkAdvancedClientOption
                                                                                                      .USER_AGENT_SUFFIX, "test")
                                                                               .advancedOptions(new HashMap<>())
                                                                               .putAdvancedOption(SdkAdvancedClientOption
                                                                                                      .USER_AGENT_PREFIX, "test")
                                                                               .addExecutionInterceptor(new SlowExecutionInterceptor())
                                                                               .executionInterceptors(new ArrayList<>())
                                                                               .build();

        assertThat(configuration.headers().get("value")).containsExactly("hello", "world");
        assertFalse(configuration.advancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX).isPresent());
        assertThat(configuration.advancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX).get()).isEqualTo("test");
        assertTrue(configuration.executionInterceptors().isEmpty());
    }

    @Test
    public void addSameItemAfterSetCollection_shouldOverride() {
        ImmutableMap<String, List<String>> map =
            ImmutableMap.of("value", Arrays.asList("hello", "world"));
        ClientOverrideConfiguration configuration = ClientOverrideConfiguration.builder()
                                                                               .headers(map)
                                                                               .putHeader("value", "blah")
                                                                               .build();

        assertThat(configuration.headers().get("value")).containsExactly("blah");
    }

    @Test
    public void shouldGuaranteeImmutability() {
        List<String> headerValues = new ArrayList<>();
        headerValues.add("bar");
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("foo", headerValues);

        List<ExecutionInterceptor> executionInterceptors = new ArrayList<>();
        SlowExecutionInterceptor slowExecutionInterceptor = new SlowExecutionInterceptor();
        executionInterceptors.add(slowExecutionInterceptor);

        ClientOverrideConfiguration.Builder configurationBuilder =
            ClientOverrideConfiguration.builder().executionInterceptors(executionInterceptors)
                                       .headers(headers);

        headerValues.add("test");
        headers.put("new header", Collections.singletonList("new value"));
        executionInterceptors.clear();

        assertThat(configurationBuilder.headers().size()).isEqualTo(1);
        assertThat(configurationBuilder.headers().get("foo")).containsExactly("bar");
        assertThat(configurationBuilder.executionInterceptors()).containsExactly(slowExecutionInterceptor);
    }

    @Test
    public void metricPublishers_createsCopy() {
        List<MetricPublisher> publishers = new ArrayList<>();
        publishers.add(mock(MetricPublisher.class));
        List<MetricPublisher> toModify = new ArrayList<>(publishers);

        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .metricPublishers(toModify)
                .build();

        toModify.clear();

        assertThat(overrideConfig.metricPublishers()).isEqualTo(publishers);
    }

    @Test
    public void addMetricPublisher_maintainsAllAdded() {
        List<MetricPublisher> publishers = new ArrayList<>();
        publishers.add(mock(MetricPublisher.class));
        publishers.add(mock(MetricPublisher.class));
        publishers.add(mock(MetricPublisher.class));

        ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();

        publishers.forEach(builder::addMetricPublisher);

        ClientOverrideConfiguration overrideConfig = builder.build();

        assertThat(overrideConfig.metricPublishers()).isEqualTo(publishers);
    }

    @Test
    public void metricPublishers_overwritesPreviouslyAdded() {
        MetricPublisher firstAdded = mock(MetricPublisher.class);

        List<MetricPublisher> publishers = new ArrayList<>();

        publishers.add(mock(MetricPublisher.class));
        publishers.add(mock(MetricPublisher.class));

        ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();

        builder.addMetricPublisher(firstAdded);

        builder.metricPublishers(publishers);

        ClientOverrideConfiguration overrideConfig = builder.build();

        assertThat(overrideConfig.metricPublishers()).isEqualTo(publishers);
    }

    @Test
    public void addMetricPublisher_listPreviouslyAdded_appendedToList() {
        List<MetricPublisher> publishers = new ArrayList<>();

        publishers.add(mock(MetricPublisher.class));
        publishers.add(mock(MetricPublisher.class));

        MetricPublisher thirdAdded = mock(MetricPublisher.class);

        ClientOverrideConfiguration.Builder builder = ClientOverrideConfiguration.builder();

        builder.metricPublishers(publishers);
        builder.addMetricPublisher(thirdAdded);

        ClientOverrideConfiguration overrideConfig = builder.build();

        assertThat(overrideConfig.metricPublishers()).containsExactly(publishers.get(0), publishers.get(1), thirdAdded);
    }
}
