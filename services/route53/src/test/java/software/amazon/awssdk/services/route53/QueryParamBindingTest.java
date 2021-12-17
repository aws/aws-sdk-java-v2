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

package software.amazon.awssdk.services.route53;

import java.net.URI;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.services.route53.model.GetHealthCheckLastFailureReasonRequest;
import software.amazon.awssdk.services.route53.model.ListHealthChecksRequest;
import software.amazon.awssdk.services.route53.transform.GetHealthCheckLastFailureReasonRequestMarshaller;
import software.amazon.awssdk.services.route53.transform.ListHealthChecksRequestMarshaller;

public class QueryParamBindingTest {

    protected static final AwsXmlProtocolFactory PROTOCOL_FACTORY = AwsXmlProtocolFactory
        .builder()
        .clientConfiguration(SdkClientConfiguration.builder()
                                                   .option(SdkClientOption.ENDPOINT, URI.create("http://localhost"))
                                                   .build())
        .build();

    /**
     * Make sure the marshaller is able to handle @UriLabel parameter values
     * containing special characters.
     */
    @Test
    public void testReservedCharInParamValue() {

        final String VALUE_WITH_SEMICOLON = ";foo";
        final String VALUE_WITH_AMPERSAND = "&bar";
        final String VALUE_WITH_QUESTION_MARK = "?charlie";

        ListHealthChecksRequest listReq = ListHealthChecksRequest.builder()
                .marker(VALUE_WITH_SEMICOLON)
                .maxItems(VALUE_WITH_AMPERSAND)
                .build();

        SdkHttpFullRequest httpReq_List = new ListHealthChecksRequestMarshaller(PROTOCOL_FACTORY).marshall(listReq);
        Assert.assertEquals("/2013-04-01/healthcheck", httpReq_List.encodedPath());

        Map<String, List<String>> queryParams = httpReq_List.rawQueryParameters();
        Assert.assertEquals(2, queryParams.size());
        Assert.assertEquals(VALUE_WITH_SEMICOLON, queryParams.get("marker").get(0));
        Assert.assertEquals(VALUE_WITH_AMPERSAND, queryParams.get("maxitems").get(0));

        GetHealthCheckLastFailureReasonRequest getFailureReq = GetHealthCheckLastFailureReasonRequest.builder()
                .healthCheckId(VALUE_WITH_QUESTION_MARK)
                .build();

        SdkHttpFullRequest httpReq_GetFailure =
                new GetHealthCheckLastFailureReasonRequestMarshaller(PROTOCOL_FACTORY).marshall(getFailureReq);
        System.out.println(httpReq_GetFailure);
        // parameter value should be URL encoded
        Assert.assertEquals(
                "/2013-04-01/healthcheck/%3Fcharlie/lastfailurereason",
                httpReq_GetFailure.encodedPath());

        queryParams = httpReq_GetFailure.rawQueryParameters();
        Assert.assertEquals(0, queryParams.size());
    }
}
