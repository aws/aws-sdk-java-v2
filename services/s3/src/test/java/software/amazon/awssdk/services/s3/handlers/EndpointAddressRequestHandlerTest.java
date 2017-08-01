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
package software.amazon.awssdk.services.s3.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.Test;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.http.DefaultSdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;

public class EndpointAddressRequestHandlerTest {

    @Test
    public void beforeRequest_MaintainsPort_When_PathStyleAddressing() throws Exception {
        RequestConfig config = mock(RequestConfig.class);
        when(config.getOriginalRequest()).thenReturn(new Object());

        URI endpoint = new URI("http://localhost:12345");

        S3AdvancedConfiguration ac = S3AdvancedConfiguration.builder().pathStyleAccessEnabled(true).build();

        SdkHttpFullRequest request = DefaultSdkHttpFullRequest.builder()
                                                              .handlerContext(AwsHandlerKeys.REQUEST_CONFIG, config)
                                                              .handlerContext(AwsHandlerKeys.SERVICE_ADVANCED_CONFIG, ac)
                                                              .endpoint(endpoint)
                                                              .build();

        EndpointAddressRequestHandler handler = new EndpointAddressRequestHandler();

        SdkHttpFullRequest newRequest = handler.beforeRequest(request);

        assertThat(newRequest.getEndpoint()).isEqualTo(endpoint);
    }

    @Test
    public void beforeRequest_MaintainsPort_When_DnsStyleAddressing() throws Exception {
        RequestConfig config = mock(RequestConfig.class);
        when(config.getOriginalRequest()).thenReturn(new Object());

        URI endpoint = new URI("http://localhost:12345");

        SdkHttpFullRequest request = DefaultSdkHttpFullRequest.builder()
                                                              .handlerContext(AwsHandlerKeys.REQUEST_CONFIG, config)
                                                              .endpoint(endpoint)
                                                              .build();

        EndpointAddressRequestHandler handler = new EndpointAddressRequestHandler();

        SdkHttpFullRequest newRequest = handler.beforeRequest(request);

        assertThat(newRequest.getEndpoint()).isEqualTo(endpoint);
    }
}
