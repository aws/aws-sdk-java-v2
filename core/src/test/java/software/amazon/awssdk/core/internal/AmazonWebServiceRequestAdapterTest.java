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

package software.amazon.awssdk.core.internal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.AmazonWebServiceRequest;
import software.amazon.awssdk.core.RequestClientOptions;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import utils.model.EmptyAmazonWebServiceRequest;

public class AmazonWebServiceRequestAdapterTest {

    @Test
    public void timeoutsSetInBaseRequest_AreAdaptedToNonNullIntegers() {
        AmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        request.setSdkClientExecutionTimeout(4000);
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);

        assertEquals(Integer.valueOf(4000), adapter.getClientExecutionTimeout());
    }

    @Test
    public void timeoutsNotSetInBaseRequest_AreNullWhenAdapted() {
        AmazonWebServiceRequestAdapter adapter = adaptEmpty();
        assertNull(adapter.getClientExecutionTimeout());
    }

    @Test
    public void customHeadersSetInBaseRequest_AreAdaptedToMap() {
        AmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        request.putCustomRequestHeader("FooHeader", "FooValue");
        request.putCustomRequestHeader("BarHeader", "BarValue");
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);

        assertThat(adapter.getCustomRequestHeaders(), hasEntry("FooHeader", "FooValue"));
        assertThat(adapter.getCustomRequestHeaders(), hasEntry("BarHeader", "BarValue"));
    }

    @Test
    public void noHeadersSetInBaseRequest_AreAdaptedToEmptyMap() {
        AmazonWebServiceRequestAdapter adapter = adaptEmpty();
        assertThat(adapter.getCustomRequestHeaders().entrySet(), empty());
    }

    @Test
    public void customQueryParamsSetInBaseRequest_AreAdaptedToMap() {
        AmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        request.putCustomQueryParameter("FooParam", "FooValue");
        request.putCustomQueryParameter("BarParam", "BarValue");
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);

        final Map<String, List<String>> params = adapter.getCustomQueryParameters();
        assertThat(params, hasEntry("FooParam", Arrays.asList("FooValue")));
        assertThat(params, hasEntry("BarParam", Arrays.asList("BarValue")));
    }

    @Test
    public void multipleValuesForSameQueryParamSet_IsAdaptedToMap() {
        AmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        request.putCustomQueryParameter("FooParam", "valOne");
        request.putCustomQueryParameter("FooParam", "valTwo");
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);
        final Map<String, List<String>> params = adapter.getCustomQueryParameters();
        assertThat(params, hasEntry("FooParam", Arrays.asList("valOne", "valTwo")));
    }

    @Test
    public void noParamsSetInBaseRequest_AreAdaptedToEmptyMap() {
        AmazonWebServiceRequestAdapter adapter = adaptEmpty();
        assertThat(adapter.getCustomQueryParameters().entrySet(), empty());
    }

    @Test
    public void originalRequestObject_IsSetOnAdapter() {
        EmptyAmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);
        assertEquals(request, adapter.getOriginalRequest());
    }

    @Test
    public void requestType_IsAdaptedToRequestClassSimpleName() {
        AmazonWebServiceRequestAdapter adapter = adaptEmpty();
        assertEquals("EmptyAmazonWebServiceRequest", adapter.getRequestType());
    }

    @Test
    public void customCredentialsProviderSetInBaseRequest_IsSetOnAdapter() {
        EmptyAmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        AwsCredentialsProvider credentialsProvider = mock(AwsCredentialsProvider.class);
        request.setRequestCredentialsProvider(credentialsProvider);
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);

        assertEquals(credentialsProvider, adapter.getCredentialsProvider());
    }

    @Test
    public void customCredentialsSetInBaseRequest_IsSetOnAdapter() {
        EmptyAmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        AwsCredentials credentials = AwsCredentials.create("akid", "skid");
        request.setRequestCredentials(credentials);
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);

        AwsCredentials adaptedCredentials = adapter.getCredentialsProvider().getCredentials();
        assertEquals("akid", adaptedCredentials.accessKeyId());
        assertEquals("skid", adaptedCredentials.secretAccessKey());
    }

    @Test
    public void readLimitMutatedOnClientOptions_IsReflectedInAdaptedClientOptions() {
        EmptyAmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        request.getRequestClientOptions().setReadLimit(9001);
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);
        assertEquals(9001, adapter.getRequestClientOptions().getReadLimit());
    }

    @Test
    public void userAgentAppendedToClientOptions_IsReflectedInAdaptedClientOptions() {
        EmptyAmazonWebServiceRequest request = new EmptyAmazonWebServiceRequest();
        request.getRequestClientOptions().appendUserAgent("foo-agent");
        AmazonWebServiceRequestAdapter adapter = new AmazonWebServiceRequestAdapter(request);
        assertThat(adapter.getRequestClientOptions().getClientMarker(
            RequestClientOptions.Marker.USER_AGENT), containsString("foo-agent"));
    }

    private AmazonWebServiceRequestAdapter adaptEmpty() {
        return new AmazonWebServiceRequestAdapter(
                new EmptyAmazonWebServiceRequest());
    }

}
