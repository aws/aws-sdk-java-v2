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

package software.amazon.awssdk.core.util;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static software.amazon.awssdk.core.client.config.SdkClientOption.METRIC_PUBLISHER;

import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.RequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkRequestOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.metrics.MetricPublisher;

@RunWith(MockitoJUnitRunner.class)
public class MetricUtilsTest {

    @Mock
    private MetricPublisher metricPublisher;

    @Test
    public void resolvePublisher_requestConfigNull_ShouldUseSdkClientConfig() {
        SdkClientConfiguration config = SdkClientConfiguration.builder().option(METRIC_PUBLISHER, metricPublisher).build();
        RequestOverrideConfiguration requestOverrideConfiguration = null;
        Optional<MetricPublisher> result = MetricUtils.resolvePublisher(config, requestOverrideConfiguration);
        assertThat(result).isEqualTo(Optional.of(metricPublisher));
    }

    @Test
    public void resolvePublisher_requestConfigNotNull_shouldTakePrecedence() {
        SdkClientConfiguration config = SdkClientConfiguration.builder().option(METRIC_PUBLISHER, mock(MetricPublisher.class)).build();
        RequestOverrideConfiguration requestOverrideConfiguration = SdkRequestOverrideConfiguration.builder().metricPublisher(metricPublisher).build();
        Optional<MetricPublisher> result = MetricUtils.resolvePublisher(config, requestOverrideConfiguration);
        assertThat(result).isEqualTo(Optional.of(metricPublisher));
    }
}
