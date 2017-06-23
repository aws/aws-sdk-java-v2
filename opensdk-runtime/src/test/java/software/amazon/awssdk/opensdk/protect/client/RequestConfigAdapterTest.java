/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk.protect.client;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.event.ProgressListener;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.opensdk.BaseRequest;
import software.amazon.awssdk.opensdk.SdkRequestConfig;

public class RequestConfigAdapterTest {

    @Test
    public void timeoutsSetInBaseRequest_AreAdaptedToNonNullIntegers() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder()
                                                     .totalExecutionTimeout(5000)
                                                     .build());
        assertEquals(Integer.valueOf(5000), adapter.getClientExecutionTimeout());
    }

    @Test
    public void timeoutsNotSetInBaseRequest_AreNullWhenAdapted() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder().build());
        assertNull(adapter.getClientExecutionTimeout());
    }

    /**
     * This test isn't much different from {@link #timeoutsNotSetInBaseRequest_AreNullWhenAdapted()}
     * other than validating we aren't throwing some null pointer if we don't set the config object
     * at all.
     */
    @Test
    public void requestConfigNotSetInBaseResult_TimeoutsAreStillNullWhenAdapted() {
        RequestConfigAdapter adapter = new RequestConfigAdapter(new EmptyRequest());
        assertNull(adapter.getClientExecutionTimeout());
    }

    @Test
    public void customHeadersSetInBaseRequest_AreAdaptedToMap() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder()
                                                     .customHeader("FooHeader", "FooValue")
                                                     .customHeader("BarHeader", "BarValue")
                                                     .build());
        assertThat(adapter.getCustomRequestHeaders(), hasEntry("FooHeader", "FooValue"));
        assertThat(adapter.getCustomRequestHeaders(), hasEntry("BarHeader", "BarValue"));
    }


    @Test
    public void noHeadersSetInBaseRequest_AreAdaptedToEmptyMap() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder().build());
        assertThat(adapter.getCustomRequestHeaders().entrySet(), empty());
    }

    @Test
    public void customQueryParamsSetInBaseRequest_AreAdaptedToMap() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder()
                                                     .customQueryParam("FooParam", "FooValue")
                                                     .customQueryParam("BarParam", "BarValue")
                                                     .build());
        final Map<String, List<String>> params = adapter.getCustomQueryParameters();
        assertThat(params, hasEntry("FooParam", Arrays.asList("FooValue")));
        assertThat(params, hasEntry("BarParam", Arrays.asList("BarValue")));
    }

    @Test
    public void multipleValuesForSameQueryParamSet_IsAdaptedToMap() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder()
                                                     .customQueryParam("FooParam", "valOne")
                                                     .customQueryParam("FooParam", "valTwo")
                                                     .build());
        final Map<String, List<String>> params = adapter.getCustomQueryParameters();
        assertThat(params, hasEntry("FooParam", Arrays.asList("valOne", "valTwo")));
    }

    @Test
    public void noParamsSetInBaseRequest_AreAdaptedToEmptyMap() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder().build());
        assertThat(adapter.getCustomQueryParameters().entrySet(), empty());
    }

    @Test
    public void unsupportedConfigurations_AreAdaptedToNullOrNoOp() {
        RequestConfigAdapter adapter = adapt(SdkRequestConfig.builder().build());
        assertEquals(RequestMetricCollector.NONE, adapter.getRequestMetricsCollector());
        assertEquals(ProgressListener.NOOP, adapter.getProgressListener());
        assertNull(adapter.getCredentialsProvider());
        assertNotNull(adapter.getRequestClientOptions());
    }

    @Test
    public void originalRequestObject_IsSetOnAdapter() {
        final EmptyRequest request = new EmptyRequest();
        RequestConfigAdapter adapter = new RequestConfigAdapter(request);
        assertEquals(request, adapter.getOriginalRequest());
    }

    @Test
    public void requestType_IsAdaptedToRequestClassSimpleName() {
        RequestConfigAdapter adapter = new RequestConfigAdapter(new EmptyRequest());
        assertEquals("EmptyRequest", adapter.getRequestType());
    }

    private RequestConfigAdapter adapt(SdkRequestConfig requestConfig) {
        return new RequestConfigAdapter(new EmptyRequest().sdkRequestConfig(requestConfig));
    }

    private static class EmptyRequest extends BaseRequest {
    }
}
