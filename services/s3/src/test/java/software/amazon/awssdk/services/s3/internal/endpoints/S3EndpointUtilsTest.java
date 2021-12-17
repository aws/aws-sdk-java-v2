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
import static org.junit.Assert.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3EndpointUtilsTest {

    @Test
    public void removesFipsIfNeeded() {
        assertThat(S3EndpointUtils.removeFipsIfNeeded("fips-us-east-1")).isEqualTo("us-east-1");
        assertThat(S3EndpointUtils.removeFipsIfNeeded("us-east-1-fips")).isEqualTo("us-east-1");
    }

    @Test
    public void isFipsRegion() {
        Assertions.assertTrue(S3EndpointUtils.isFipsRegion("fips-us-east-1"));
        Assertions.assertTrue(S3EndpointUtils.isFipsRegion("us-east-1-fips"));
        Assertions.assertFalse(S3EndpointUtils.isFipsRegion("us-fips-1"));
    }

    @Test
    public void isAccelerateEnabled() {
        Assertions.assertFalse(S3EndpointUtils.isAccelerateEnabled(S3Configuration.builder().build()));
        Assertions.assertFalse(S3EndpointUtils.isAccelerateEnabled(null));
        Assertions.assertFalse(S3EndpointUtils.isAccelerateEnabled(S3Configuration.builder().accelerateModeEnabled(false).build()));
        Assertions.assertTrue(S3EndpointUtils.isAccelerateEnabled(S3Configuration.builder().accelerateModeEnabled(true).build()));
    }

    @Test
    public void isAccelerateSupported() {
        Assertions.assertFalse(S3EndpointUtils.isAccelerateSupported(ListBucketsRequest.builder().build()));
        Assertions.assertTrue(S3EndpointUtils.isAccelerateSupported(PutObjectRequest.builder().build()));
    }

    @Test
    public void accelerateEndpoint() {
        assertThat(S3EndpointUtils.accelerateEndpoint("domain", "https"))
            .isEqualTo(URI.create("https://s3-accelerate.domain"));

        assertThat(S3EndpointUtils.accelerateDualstackEndpoint("domain", "https"))
            .isEqualTo(URI.create("https://s3-accelerate.dualstack.domain"));
    }

    @Test
    public void isDualstackEnabled() {
        Assertions.assertFalse(S3EndpointUtils.isDualstackEnabled(S3Configuration.builder().build()));
        Assertions.assertFalse(S3EndpointUtils.isDualstackEnabled(null));
        Assertions.assertFalse(S3EndpointUtils.isDualstackEnabled(S3Configuration.builder().dualstackEnabled(false).build()));
        Assertions.assertTrue(S3EndpointUtils.isDualstackEnabled(S3Configuration.builder().dualstackEnabled(true).build()));
    }

    @Test
    public void dualStackEndpoint() {
        assertThat(S3EndpointUtils.dualstackEndpoint("id", "domain", "https"))
            .isEqualTo(URI.create("https://s3.dualstack.id.domain"));
    }

    @Test
    public void isPathstyleAccessEnabled() {
        Assertions.assertFalse(S3EndpointUtils.isPathStyleAccessEnabled(S3Configuration.builder().build()));
        Assertions.assertFalse(S3EndpointUtils.isPathStyleAccessEnabled(null));
        Assertions.assertFalse(S3EndpointUtils.isPathStyleAccessEnabled(S3Configuration.builder().pathStyleAccessEnabled(false).build()));
        Assertions.assertTrue(S3EndpointUtils.isPathStyleAccessEnabled(S3Configuration.builder().pathStyleAccessEnabled(true).build()));
    }

    @Test
    public void isArnRegionEnabled() {
        Assertions.assertFalse(S3EndpointUtils.isArnRegionEnabled(S3Configuration.builder().build()));
        Assertions.assertFalse(S3EndpointUtils.isArnRegionEnabled(null));
        Assertions.assertFalse(S3EndpointUtils.isArnRegionEnabled(S3Configuration.builder().useArnRegionEnabled(false).build()));
        Assertions.assertTrue(S3EndpointUtils.isArnRegionEnabled(S3Configuration.builder().useArnRegionEnabled(true).build()));
    }

    @Test
    public void changeToDnsEndpoint() {
        SdkHttpRequest.Builder mutableRequest = SdkHttpFullRequest.builder().host("s3").encodedPath("/test-bucket");
        S3EndpointUtils.changeToDnsEndpoint(mutableRequest, "test-bucket");
        assertThat(mutableRequest.host()).isEqualTo("test-bucket.s3");
        assertThat(mutableRequest.encodedPath()).isEqualTo("");
    }

    @Test
    public void isArn() {
        Assertions.assertFalse(S3EndpointUtils.isArn("bucketName"));
        Assertions.assertFalse(S3EndpointUtils.isArn("test:arn:"));
        Assertions.assertTrue(S3EndpointUtils.isArn("arn:test"));
    }
}
