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

import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.endpoints.EndpointUrl;

/**
 * Tests for the deprecated {@link SdkInternalExecutionAttribute#HTTP_REQUEST_URI_BEFORE_MODIFY}, which is a derived
 * view over {@link SdkInternalExecutionAttribute#HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY}.
 */
class SdkInternalExecutionAttributeTest {

    private static final EndpointUrl ENDPOINT =
        EndpointUrl.fromComponents("https", "lambda.us-east-1.amazonaws.com", 443,
                                   "/2015-03-31/functions/my-function/invocations");

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
    void httpRequestUriBeforeModify_rendersSnapshottedEndpoint() {
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY, ENDPOINT);

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY))
            .isEqualTo(URI.create("https://lambda.us-east-1.amazonaws.com:443"
                                  + "/2015-03-31/functions/my-function/invocations"));
    }

    @Test
    void httpRequestUriBeforeModify_neverCarriesQueryString() {
        // The snapshot holds components only, so the query string is always absent. This is what keeps the query and
        // ec2 protocols cheap: for those the raw query parameters are the entire request payload at this point.
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY, ENDPOINT);

        URI uri = attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY);

        assertThat(uri.getQuery()).isNull();
        assertThat(uri.getRawPath()).isEqualTo("/2015-03-31/functions/my-function/invocations");
    }

    @Test
    void httpRequestUriBeforeModify_repeatedReadsShareTheSameUri() {
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY, ENDPOINT);

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY))
            .isSameAs(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY));
    }

    @Test
    void httpRequestUriBeforeModify_writeReplacesTheEndpoint() {
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY, ENDPOINT);

        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY,
                                URI.create("http://custom.example.com:8443/my-path"));

        EndpointUrl endpoint =
            attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY);
        assertThat(endpoint.scheme()).isEqualTo("http");
        assertThat(endpoint.host()).isEqualTo("custom.example.com");
        assertThat(endpoint.port()).isEqualTo(8443);
        assertThat(endpoint.encodedPath()).isEqualTo("/my-path");
    }

    @Test
    void httpRequestUriBeforeModify_writeRoundTripsExactly() {
        // A written URI reads back unchanged, query string included, so a caller that sets this attribute sees
        // precisely what it set.
        URI written = URI.create("https://custom.example.com/my-path?Qualifier=prod");

        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY, written);

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY))
            .isEqualTo(written);
    }

    @Test
    void httpRequestUriBeforeModify_writeNull_clearsTheEndpoint() {
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY, ENDPOINT);

        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY, null);

        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY)).isNull();
        assertThat(attributes.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY)).isNull();
    }

    @Test
    void httpRequestEndpointBeforeModify_survivesCopy() {
        attributes.putAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY, ENDPOINT);

        // Copies duplicate the backing map rather than re-setting each attribute, so the read-only derived view must
        // still resolve against the copy.
        ExecutionAttributes copy = attributes.copy();

        assertThat(copy.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_ENDPOINT_BEFORE_MODIFY))
            .isSameAs(ENDPOINT);
        assertThat(copy.getAttribute(SdkInternalExecutionAttribute.HTTP_REQUEST_URI_BEFORE_MODIFY))
            .isEqualTo(ENDPOINT.toUri());
    }
}
