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

package software.amazon.awssdk.http;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.DefaultRequest;
import software.amazon.awssdk.Request;
import software.amazon.awssdk.util.StringInputStream;

public class SdkHttpFullRequestAdapterTest {

    @Test
    public void adaptHeaders_AdaptsValuesToSingletonLists() {
        Request<Void> request = new DefaultRequest<>("foo");
        request.addHeader("HeaderOne", "valOne");

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getHeaders().get("HeaderOne"), hasSize(1));
        assertThat(adapted.getHeaders().get("HeaderOne").get(0), equalTo("valOne"));
    }

    @Test
    public void adaptHeaders_NullValueAdaptsToSingletonListContainingNull() {
        Request<Void> request = new DefaultRequest<>("foo");
        request.addHeader("HeaderOne", null);

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getHeaders().get("HeaderOne"), hasSize(1));
        assertThat(adapted.getHeaders().get("HeaderOne").get(0), nullValue());
    }

    @Test
    public void adapt_EndpointIsPreserved() {
        Request<Void> request = new DefaultRequest<>("foo");
        request.setEndpoint(URI.create("http://shorea.com"));

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getEndpoint(), equalTo(request.getEndpoint()));
    }

    @Test
    public void adapt_QueryParamsArePreserved() {
        Request<Void> request = new DefaultRequest<>("foo");
        request.addParameter("QueryParam", "value");

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getParameters().get("QueryParam"), hasSize(1));
        assertThat(adapted.getParameters().get("QueryParam").get(0), equalTo("value"));
    }

    @Test
    public void adapt_QueryParamWithMultipleValuesArePreserved() {
        Request<Void> request = new DefaultRequest<>("foo");
        request.addParameters("QueryParam", Arrays.asList("foo", "bar", "baz"));

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getParameters().get("QueryParam"), hasSize(3));
        assertThat(adapted.getParameters().get("QueryParam"), hasItems("foo", "bar", "baz"));
    }

    @Test
    public void adapt_ResourcePathPreserved() {
        Request<Void> request = new DefaultRequest<>("foo");
        request.setResourcePath("/foo/bar");

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getResourcePath(), equalTo("/foo/bar"));
    }

    @Test
    public void adapt_HttpMethodTranslatedToSdkHttpMethod() {
        Request<Void> request = new DefaultRequest<>("foo");
        request.setHttpMethod(HttpMethodName.GET);

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getHttpMethod(), equalTo(SdkHttpMethod.GET));
    }

    @Test
    public void adapt_InputStreamPreserved() throws UnsupportedEncodingException {
        StringInputStream contents = new StringInputStream("contents");
        Request<Void> request = new DefaultRequest<>("foo");
        request.setContent(contents);

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.getContent(), equalTo(contents));
    }


}
