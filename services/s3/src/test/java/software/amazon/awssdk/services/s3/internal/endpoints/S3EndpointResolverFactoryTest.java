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

import org.junit.Test;

public class S3EndpointResolverFactoryTest {

    @Test
    public void nullBucketName_returnsBucketEndpointResolver() {
        assertThat(S3EndpointResolverFactory.getEndpointResolver(null)).isInstanceOf(S3BucketEndpointResolver.class);
    }

    @Test
    public void emptyBucketName_returnsBucketEndpointResolver() {
        String bucketName = "";
        assertThat(S3EndpointResolverFactory.getEndpointResolver(bucketName)).isInstanceOf(S3BucketEndpointResolver.class);
    }

    @Test
    public void nonAccessPointBucketName_returnsBucketEndpointResolver() {
        String bucketName = "test-bucket";
        assertThat(S3EndpointResolverFactory.getEndpointResolver(bucketName)).isInstanceOf(S3BucketEndpointResolver.class);
    }

    @Test
    public void accessPointBucketName_returnsAccessPointEndpointResolver() {
        String bucketName = "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar";
        assertThat(S3EndpointResolverFactory.getEndpointResolver(bucketName)).isInstanceOf(S3AccessPointEndpointResolver.class);
    }

}
