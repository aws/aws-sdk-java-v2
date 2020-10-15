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

package software.amazon.awssdk.services.s3.internal.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3EndpointResolverContextTest {

    @Test
    public void toBuilder_minimal() {
        S3EndpointResolverContext context = S3EndpointResolverContext.builder().build();
        assertFalse(context.endpointOverridden());
        assertNull(context.originalRequest());
        assertNull(context.region());
        assertNull(context.serviceConfiguration());
        assertNull(context.request());
    }

    @Test
    public void toBuilder_maximal() {
        S3Configuration serviceConfiguration = S3Configuration.builder().build();
        SdkHttpFullRequest httpRequest = SdkHttpFullRequest.builder().protocol("http").host("host").method(SdkHttpMethod.POST).build();
        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                                                                     .endpointOverridden(true)
                                                                     .originalRequest(PutObjectRequest.builder().build())
                                                                     .region(Region.US_EAST_1)
                                                                     .serviceConfiguration(serviceConfiguration)
                                                                     .request(httpRequest)
                                                                     .build();
        assertTrue(context.endpointOverridden());
        assertThat(context.originalRequest()).isInstanceOf(PutObjectRequest.class);
        assertThat(context.region()).isEqualTo(Region.US_EAST_1);
        assertThat(context.serviceConfiguration()).isEqualTo(serviceConfiguration);
        assertThat(context.request()).isEqualTo(httpRequest);
    }
}
