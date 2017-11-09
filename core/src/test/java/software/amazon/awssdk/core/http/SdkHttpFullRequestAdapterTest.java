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

package software.amazon.awssdk.core.http;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Arrays;
import org.junit.Test;
import software.amazon.awssdk.core.Request;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import utils.ValidSdkObjects;

public class SdkHttpFullRequestAdapterTest {

    @Test
    public void adaptHeaders_AdaptsValuesToSingletonLists() {
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.addHeader("HeaderOne", "valOne");

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.headers().get("HeaderOne"), hasSize(1));
        assertThat(adapted.headers().get("HeaderOne").get(0), equalTo("valOne"));
    }

    @Test
    public void adaptHeaders_NullValueAdaptsToSingletonListContainingNull() {
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.addHeader("HeaderOne", null);

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.headers().get("HeaderOne"), hasSize(1));
        assertThat(adapted.headers().get("HeaderOne").get(0), nullValue());
    }

    @Test
    public void adapt_EndpointIsPreserved() {
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.setEndpoint(URI.create("http://shorea.com"));

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.protocol(), equalTo("http"));
        assertThat(adapted.host(), equalTo("shorea.com"));
        assertThat(adapted.port(), equalTo(80));
    }

    @Test
    public void adapt_QueryParamsArePreserved() {
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.addParameter("QueryParam", "value");

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.rawQueryParameters().get("QueryParam"), hasSize(1));
        assertThat(adapted.rawQueryParameters().get("QueryParam").get(0), equalTo("value"));
    }

    @Test
    public void adapt_QueryParamWithMultipleValuesArePreserved() {
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.addParameters("QueryParam", Arrays.asList("foo", "bar", "baz"));

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.rawQueryParameters().get("QueryParam"), hasSize(3));
        assertThat(adapted.rawQueryParameters().get("QueryParam"), hasItems("foo", "bar", "baz"));
    }

    @Test
    public void adapt_ResourcePathPreserved() {
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.setResourcePath("/foo/bar/");

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.encodedPath(), equalTo("/foo/bar/"));
    }

    @Test
    public void adapt_HttpMethodTranslatedToSdkHttpMethod() {
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.setHttpMethod(HttpMethodName.GET);

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.method(), equalTo(SdkHttpMethod.GET));
    }

    @Test
    public void adapt_InputStreamPreserved() throws UnsupportedEncodingException {
        StringInputStream contents = new StringInputStream("contents");
        Request<Void> request = ValidSdkObjects.legacyRequest();
        request.setContent(contents);

        SdkHttpFullRequest adapted = SdkHttpFullRequestAdapter.toHttpFullRequest(request);

        assertThat(adapted.content().orElse(null), equalTo(contents));
    }


}
