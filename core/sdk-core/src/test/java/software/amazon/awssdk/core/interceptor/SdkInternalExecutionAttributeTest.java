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

package software.amazon.awssdk.core.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * Tests for the deprecated {@link SdkInternalExecutionAttribute#HTTP_REQUEST_URI_BEFORE_MODIFY}, which is a derived
 * view over {@link SdkInternalExecutionAttribute#HTTP_REQUEST_BEFORE_MODIFY}.
 */
class SdkInternalExecutionAttributeTest {

    private ExecutionAttributes attributes;

    @BeforeEach
    void setup() {
        attributes = new ExecutionAttributes();
    }

    @Test
    void httpRequestUriBeforeModify_noSnapshot_isNull() {
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY)).isNull();
    }

    @Test
    void httpRequestUriBeforeModify_readsUriOfSnapshottedRequest() {
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY,
                                requestWithQueryParams());

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY))
            .isEqualTo(URI.create("https://monitoring.us-east-1.amazonaws.com/"
                                  + "?Action=PutMetricData&Namespace=My%2FNamespace"));
    }

    @Test
    void httpRequestUriBeforeModify_isNotComputedUntilRead() {
        URI uri = URI.create("https://monitoring.us-east-1.amazonaws.com/");
        SdkHttpRequest snapshot = mock(SdkHttpRequest.class);
        when(snapshot.host()).thenReturn("monitoring.us-east-1.amazonaws.com");
        when(snapshot.getUri()).thenReturn(uri);

        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY, snapshot);
        verify(snapshot, never()).getUri();

        // Reading the endpoint components must not build the URI either.
        SdkHttpRequest read = attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY);
        assertThat(read.host()).isEqualTo("monitoring.us-east-1.amazonaws.com");
        verify(snapshot, never()).getUri();

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY))
            .isSameAs(uri);
        verify(snapshot).getUri();
    }

    @Test
    void httpRequestUriBeforeModify_writeIsVisibleOnSnapshottedRequest() {
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY, requestWithQueryParams());

        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY,
                                URI.create("https://custom.example.com:8443/"));

        SdkHttpRequest snapshot = attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_BEFORE_MODIFY);
        assertThat(snapshot.host()).isEqualTo("custom.example.com");
        assertThat(snapshot.port()).isEqualTo(8443);
    }

    /**
     * Shaped like a marshalled query-protocol request: the payload lives in the raw query parameters until
     * {@code QueryParametersToBodyStage} moves it into the body.
     */
    private static SdkHttpRequest requestWithQueryParams() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.POST)
                                 .protocol("https")
                                 .host("monitoring.us-east-1.amazonaws.com")
                                 .encodedPath("/")
                                 .putRawQueryParameter("Action", "PutMetricData")
                                 .putRawQueryParameter("Namespace", "My/Namespace")
                                 .build();
    }
}
