package software.amazon.awssdk.core;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.core.client.config.SdkClientOption.METRIC_PUBLISHERS;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.internal.util.MetricUtils;
import software.amazon.awssdk.metrics.MetricPublisher;

public class MetricsConfigurationTest {

    @Test
    public void onlyConfigRequestMetricPublisher_returnRequestPublisher() {
        MetricPublisher metricPublisher1 = mock(MetricPublisher.class);
        MetricPublisher metricPublisher2 = mock(MetricPublisher.class);
        List<MetricPublisher> expectedPublishers = Arrays.asList(metricPublisher1, metricPublisher2);
        SdkRequest sdkRequest = mock(SdkRequest.class);
        Optional requestConfig = Optional.of(SdkRequestOverrideConfiguration.builder()
                                                                            .addMetricPublisher(metricPublisher1)
                                                                            .addMetricPublisher(metricPublisher2)
                                                                            .build());
        when(sdkRequest.overrideConfiguration()).thenReturn(requestConfig);
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder().build();
        List<MetricPublisher> metricPublishers = MetricUtils.resolvePublishers(clientConfig, sdkRequest);
        assertEquals(expectedPublishers, metricPublishers);
    }

    @Test
    public void onlyConfigRequestOverrideMetricPublisher_returnRequestPublisher() {
        MetricPublisher metricPublisher1 = mock(MetricPublisher.class);
        MetricPublisher metricPublisher2 = mock(MetricPublisher.class);
        List<MetricPublisher> expectedPublishers = Arrays.asList(metricPublisher1, metricPublisher2);
        RequestOverrideConfiguration requestConfig = SdkRequestOverrideConfiguration.builder()
                                                                                    .addMetricPublisher(metricPublisher1)
                                                                                    .addMetricPublisher(metricPublisher2)
                                                                                    .build();
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder().build();
        List<MetricPublisher> metricPublishers = MetricUtils.resolvePublishers(clientConfig, requestConfig);
        assertEquals(expectedPublishers, metricPublishers);
    }

    @Test
    public void onlyConfigClientMetricPublisher_returnClientPublisher() {
        MetricPublisher metricPublisher1 = mock(MetricPublisher.class);
        MetricPublisher metricPublisher2 = mock(MetricPublisher.class);
        List<MetricPublisher> expectedPublishers = Arrays.asList(metricPublisher1, metricPublisher2);
        RequestOverrideConfiguration requestConfig = SdkRequestOverrideConfiguration.builder().build();
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                                                                    .option(METRIC_PUBLISHERS,
                                                                            Arrays.asList(metricPublisher1, metricPublisher2))
                                                                    .build();
        List<MetricPublisher> metricPublishers = MetricUtils.resolvePublishers(clientConfig, requestConfig);
        assertEquals(expectedPublishers, metricPublishers);
    }

    @Test
    public void clientOverrideConfigurationMetricPublisher_returnClientPublisher() {
        MetricPublisher metricPublisher1 = mock(MetricPublisher.class);
        MetricPublisher metricPublisher2 = mock(MetricPublisher.class);
        List<MetricPublisher> expectedPublishers = Arrays.asList(metricPublisher1, metricPublisher2);
        ClientOverrideConfiguration clientConfig = ClientOverrideConfiguration.builder()
                                                                              .addMetricPublisher(metricPublisher1)
                                                                              .addMetricPublisher(metricPublisher2)
                                                                              .build();
        List<MetricPublisher> metricPublishers = clientConfig.metricPublishers();
        assertEquals(expectedPublishers, metricPublishers);
    }


    @Test
    public void configBothRequestAndClientMetricPublisher_returnRequestPublisher() {
        MetricPublisher metricPublisher1 = mock(MetricPublisher.class);
        MetricPublisher metricPublisher2 = mock(MetricPublisher.class);
        MetricPublisher metricPublisher3 = mock(MetricPublisher.class);
        List<MetricPublisher> expectedPublishers = Collections.singletonList(metricPublisher3);
        SdkClientConfiguration clientConfig = SdkClientConfiguration.builder()
                                                                    .option(METRIC_PUBLISHERS,
                                                                            Arrays.asList(metricPublisher1, metricPublisher2))
                                                                    .build();
        RequestOverrideConfiguration requestConfig = SdkRequestOverrideConfiguration.builder()
                                                                                    .addMetricPublisher(metricPublisher3)
                                                                                    .build();
        List<MetricPublisher> metricPublishers = MetricUtils.resolvePublishers(clientConfig, requestConfig);
        assertEquals(expectedPublishers, metricPublishers);
    }

}
