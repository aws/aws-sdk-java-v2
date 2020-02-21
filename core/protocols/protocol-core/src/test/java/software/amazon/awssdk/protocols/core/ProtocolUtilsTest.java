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

package software.amazon.awssdk.protocols.core;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

public class ProtocolUtilsTest {

    @Test
    public void createSdkHttpRequest_SetsHttpMethodAndEndpointCorrectly() {
        SdkHttpFullRequest.Builder sdkHttpRequest = ProtocolUtils.createSdkHttpRequest(
            OperationInfo.builder()
                         .httpMethod(SdkHttpMethod.DELETE)
                         .build(), URI.create("http://localhost:8080"));
        assertThat(sdkHttpRequest.protocol()).isEqualTo("http");
        assertThat(sdkHttpRequest.host()).isEqualTo("localhost");
        assertThat(sdkHttpRequest.port()).isEqualTo(8080);
        assertThat(sdkHttpRequest.method()).isEqualTo(SdkHttpMethod.DELETE);
    }

    @Test
    public void createSdkHttpRequest_EndpointWithPathSetCorrectly() {
        SdkHttpFullRequest.Builder sdkHttpRequest = ProtocolUtils.createSdkHttpRequest(
            OperationInfo.builder()
                         .httpMethod(SdkHttpMethod.DELETE)
                         .build(), URI.create("http://localhost/foo/bar"));
        assertThat(sdkHttpRequest.encodedPath()).isEqualTo("/foo/bar");
    }

    @Test
    public void createSdkHttpRequest_RequestUriAppendedToEndpointPath() {
        SdkHttpFullRequest.Builder sdkHttpRequest = ProtocolUtils.createSdkHttpRequest(
            OperationInfo.builder()
                         .httpMethod(SdkHttpMethod.DELETE)
                         .requestUri("/baz")
                         .build(), URI.create("http://localhost/foo/bar/"));
        assertThat(sdkHttpRequest.encodedPath()).isEqualTo("/foo/bar/baz");
    }

    @Test
    public void createSdkHttpRequest_NoTrailingSlashInEndpointPath_RequestUriAppendedToEndpointPath() {
        SdkHttpFullRequest.Builder sdkHttpRequest = ProtocolUtils.createSdkHttpRequest(
            OperationInfo.builder()
                         .httpMethod(SdkHttpMethod.DELETE)
                         .requestUri("/baz")
                         .build(), URI.create("http://localhost/foo/bar"));
        assertThat(sdkHttpRequest.encodedPath()).isEqualTo("/foo/bar/baz");
    }

    @Test
    public void createSdkHttpRequest_NoLeadingSlashInRequestUri_RequestUriAppendedToEndpointPath() {
        SdkHttpFullRequest.Builder sdkHttpRequest = ProtocolUtils.createSdkHttpRequest(
            OperationInfo.builder()
                         .httpMethod(SdkHttpMethod.DELETE)
                         .requestUri("baz")
                         .build(), URI.create("http://localhost/foo/bar/"));
        assertThat(sdkHttpRequest.encodedPath()).isEqualTo("/foo/bar/baz");
    }

    @Test
    public void createSdkHttpRequest_NoTrailingOrLeadingSlash_RequestUriAppendedToEndpointPath() {
        SdkHttpFullRequest.Builder sdkHttpRequest = ProtocolUtils.createSdkHttpRequest(
            OperationInfo.builder()
                         .httpMethod(SdkHttpMethod.DELETE)
                         .requestUri("baz")
                         .build(), URI.create("http://localhost/foo/bar"));
        assertThat(sdkHttpRequest.encodedPath()).isEqualTo("/foo/bar/baz");
    }

    @Test
    public void request_null_returns_null() {
        Assert.assertNull(ProtocolUtils.addStaticQueryParametersToRequest(null,
                                                                          "foo"));
    }

    @Test
    public void uri_resource_path_null_returns_null() {
        Assert.assertNull(ProtocolUtils
                              .addStaticQueryParametersToRequest(emptyRequest(), null));
    }

    private SdkHttpFullRequest.Builder emptyRequest() {
        return SdkHttpFullRequest.builder();
    }

    @Test
    public void uri_resource_path_doesnot_have_static_query_params_returns_uri_resource_path() {

        final String uriResourcePath = "/foo/bar";

        Assert.assertEquals(uriResourcePath, ProtocolUtils
            .addStaticQueryParametersToRequest(emptyRequest(), uriResourcePath));

    }

    @Test
    public void uri_resource_path_ends_with_question_mark_returns_path_removed_with_question_mark() {

        final String expectedResourcePath = "/foo/bar";
        final String pathWithEmptyStaticQueryParams = expectedResourcePath + "?";

        Assert.assertEquals(expectedResourcePath, ProtocolUtils
            .addStaticQueryParametersToRequest(emptyRequest(), pathWithEmptyStaticQueryParams));

    }

    @Test
    public void queryparam_value_empty_adds_parameter_with_empty_string_to_request() {
        final String uriResourcePath = "/foo/bar";
        final String uriResourcePathWithParams =
            uriResourcePath + "?param1=";

        SdkHttpFullRequest.Builder request = emptyRequest();

        Assert.assertEquals(uriResourcePath, ProtocolUtils
            .addStaticQueryParametersToRequest(request,
                                               uriResourcePathWithParams));
        Assert.assertTrue(request.rawQueryParameters().containsKey("param1"));
        Assert.assertEquals(singletonList(""), request.rawQueryParameters().get("param1"));
    }

    @Test
    public void static_queryparams_in_path_added_to_request() {
        final String uriResourcePath = "/foo/bar";
        final String uriResourcePathWithParams =
            uriResourcePath + "?param1=value1&param2=value2";
        SdkHttpFullRequest.Builder request = emptyRequest();

        Assert.assertEquals(uriResourcePath, ProtocolUtils
            .addStaticQueryParametersToRequest(request,
                                               uriResourcePathWithParams));
        Assert.assertTrue(request.rawQueryParameters().containsKey("param1"));
        Assert.assertTrue(request.rawQueryParameters().containsKey("param2"));
        Assert.assertEquals(singletonList("value1"), request.rawQueryParameters().get("param1"));
        Assert.assertEquals(singletonList("value2"), request.rawQueryParameters().get("param2"));

    }

    @Test
    public void queryparam_without_value_returns_list_containing_null_value() {
        final String uriResourcePath = "/foo/bar";
        final String uriResourcePathWithParams =
            uriResourcePath + "?param";
        SdkHttpFullRequest.Builder request = emptyRequest();

        Assert.assertEquals(uriResourcePath, ProtocolUtils.addStaticQueryParametersToRequest(request, uriResourcePathWithParams));

        Assert.assertTrue(request.rawQueryParameters().containsKey("param"));
        Assert.assertEquals(singletonList((String) null), request.rawQueryParameters().get("param"));
    }

}
