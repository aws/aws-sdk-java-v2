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
import org.junit.Test;
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
        assertTrue(S3EndpointUtils.isFipsRegion("fips-us-east-1"));
        assertTrue(S3EndpointUtils.isFipsRegion("us-east-1-fips"));
        assertFalse(S3EndpointUtils.isFipsRegion("us-fips-1"));
    }

    @Test
    public void isFipsRegionProvided() {
        assertTrue(S3EndpointUtils.isFipsRegionProvided("fips-us-east-1", "us-east-1", false));
        assertFalse(S3EndpointUtils.isFipsRegionProvided("us-east-1", "fips-us-east-1", false));
        assertTrue(S3EndpointUtils.isFipsRegionProvided("us-east-1", "us-east-1-fips", true));
        assertFalse(S3EndpointUtils.isFipsRegionProvided("us-east-1-fips", "us-east-1", true));
    }

    @Test
    public void isAccelerateEnabled() {
        assertFalse(S3EndpointUtils.isAccelerateEnabled(S3Configuration.builder().build()));
        assertFalse(S3EndpointUtils.isAccelerateEnabled(null));
        assertFalse(S3EndpointUtils.isAccelerateEnabled(S3Configuration.builder().accelerateModeEnabled(false).build()));
        assertTrue(S3EndpointUtils.isAccelerateEnabled(S3Configuration.builder().accelerateModeEnabled(true).build()));
    }

    @Test
    public void isAccelerateSupported() {
        assertFalse(S3EndpointUtils.isAccelerateSupported(ListBucketsRequest.builder().build()));
        assertTrue(S3EndpointUtils.isAccelerateSupported(PutObjectRequest.builder().build()));
    }

    @Test
    public void accelerateEndpoint() {
        assertThat(S3EndpointUtils.accelerateEndpoint(S3Configuration.builder().build(),
                                                      "domain",
                                                      "https"))
            .isEqualTo(URI.create("https://s3-accelerate.domain"));

        assertThat(S3EndpointUtils.accelerateEndpoint(S3Configuration.builder().dualstackEnabled(true).build(),
                                                      "domain",
                                                      "https"))
            .isEqualTo(URI.create("https://s3-accelerate.dualstack.domain"));
    }

    @Test
    public void isDualstackEnabled() {
        assertFalse(S3EndpointUtils.isDualstackEnabled(S3Configuration.builder().build()));
        assertFalse(S3EndpointUtils.isDualstackEnabled(null));
        assertFalse(S3EndpointUtils.isDualstackEnabled(S3Configuration.builder().dualstackEnabled(false).build()));
        assertTrue(S3EndpointUtils.isDualstackEnabled(S3Configuration.builder().dualstackEnabled(true).build()));
    }

    @Test
    public void dualStackEndpoint() {
        assertThat(S3EndpointUtils.dualstackEndpoint("id", "domain", "https"))
            .isEqualTo(URI.create("https://s3.dualstack.id.domain"));
    }

    @Test
    public void isPathstyleAccessEnabled() {
        assertFalse(S3EndpointUtils.isPathStyleAccessEnabled(S3Configuration.builder().build()));
        assertFalse(S3EndpointUtils.isPathStyleAccessEnabled(null));
        assertFalse(S3EndpointUtils.isPathStyleAccessEnabled(S3Configuration.builder().pathStyleAccessEnabled(false).build()));
        assertTrue(S3EndpointUtils.isPathStyleAccessEnabled(S3Configuration.builder().pathStyleAccessEnabled(true).build()));
    }

    @Test
    public void isArnRegionEnabled() {
        assertFalse(S3EndpointUtils.isArnRegionEnabled(S3Configuration.builder().build()));
        assertFalse(S3EndpointUtils.isArnRegionEnabled(null));
        assertFalse(S3EndpointUtils.isArnRegionEnabled(S3Configuration.builder().useArnRegionEnabled(false).build()));
        assertTrue(S3EndpointUtils.isArnRegionEnabled(S3Configuration.builder().useArnRegionEnabled(true).build()));
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
        assertFalse(S3EndpointUtils.isArn("bucketName"));
        assertFalse(S3EndpointUtils.isArn("test:arn:"));
        assertTrue(S3EndpointUtils.isArn("arn:test"));
    }
}
