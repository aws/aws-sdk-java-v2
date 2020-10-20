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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.utils.http.SdkHttpUtils.urlEncode;

import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.utils.InterceptorTestUtils;

public class S3AccessPointEndpointResolverTest {

    S3AccessPointEndpointResolver endpointResolver;

    @Before
    public void setUp()  {
        endpointResolver = S3AccessPointEndpointResolver.create();
    }

    @Test
    public void accesspointArn_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint:foobar",
                             "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
    }

    @Test
    public void accesspointArn_futureUnknownRegion_US_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-future-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-future-1.amazonaws.com",
                             Region.of("us-future-1"),
                             S3Configuration.builder(),
                             Region.of("us-future-1"));
    }

    @Test
    public void accesspointArn_futureUnknownRegion_crossRegion_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-future-2:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-future-2.amazonaws.com",
                             Region.of("us-future-2"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("us-future-1"));
    }

    @Test
    public void accesspointArn_futureUnknownRegion_CN_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws-cn:s3:cn-future-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.cn-future-1.amazonaws.com.cn",
                             Region.of("cn-future-1"),
                             S3Configuration.builder(),
                             Region.of("cn-future-1"));
    }

    @Test
    public void accesspointArn_futureUnknownRegionAndPartition_defaultsToAws() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:unknown:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.unknown.amazonaws.com",
                             Region.of("unknown"),
                             S3Configuration.builder(),
                             Region.of("unknown"));
    }

    @Test
    public void malformedArn_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:foobar",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ARN");
    }

    @Test
    public void unsupportedArn_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:unsupported:foobar",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ARN");
    }

    @Test
    public void accesspointArn_invalidPartition_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:bar:s3:us-east-1:12345678910:accesspoint:foobar",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bar");
    }

    @Test
    public void bucketArn_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:bucket_name:foobar",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bucket parameter");
    }


    @Test
    public void accesspointArn_withSlashes_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             S3Configuration.builder());
    }

    @Test
    public void accesspointArn_withDualStackEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.dualstack.us-east-1.amazonaws.com",
                             S3Configuration.builder().dualstackEnabled(true));
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.dualstack.us-east-1.amazonaws.com",
                             S3Configuration.builder().dualstackEnabled(true));
    }

    @Test
    public void accesspointArn_withCnPartition_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws-cn:s3:cn-north-1:12345678910:accesspoint:foobar",
                             "http://foobar-12345678910.s3-accesspoint.cn-north-1.amazonaws.com.cn",
                             Region.of("cn-north-1"),
                             S3Configuration.builder(),
                             Region.of("cn-north-1"));
        verifyAccesspointArn("https",
                             "arn:aws-cn:s3:cn-north-1:12345678910:accesspoint:foobar",
                             "https://foobar-12345678910.s3-accesspoint.cn-north-1.amazonaws.com.cn",
                             Region.of("cn-north-1"),
                             S3Configuration.builder(),
                             Region.of("cn-north-1"));
    }

    @Test
    public void accesspointArn_withDifferentPartition_useArnRegionEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws-cn:s3:cn-north-1:12345678910:accesspoint:foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.cn-north-1.amazonaws.com.cn",
                                                      Region.of("cn-north-1"),
                                                      S3Configuration.builder().useArnRegionEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("partition");
    }

    @Test
    public void accesspointArn_withFipsRegionPrefix_noFipsInArn_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder(),
                             Region.of("fips-us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder(),
                             Region.of("fips-us-east-1"));
    }

    @Test
    public void accesspointArn_withFipsRegionPrefix_FipsInArn_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder(),
                             Region.of("fips-us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder(),
                             Region.of("fips-us-east-1"));
    }

    @Test
    public void accesspointArn_withFipsRegionPrefix_noFipsInArn_useArnRegionEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("fips-us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("fips-us-east-1"));
    }


    @Test
    public void accesspointArn_withFipsRegionPrefix_FipsInArn_useArnRegionEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("fips-us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("fips-us-east-1"));
    }



    @Test
    public void accesspointArn_withFipsRegionPrefix_ArnRegionNotMatches_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder(),
                                                      Region.of("fips-us-gov-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("The region field of the ARN being passed as a bucket parameter to an S3 operation does not match the region the client was configured with.");
        assertThatThrownBy(() -> verifyAccesspointArn("https",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder(),
                                                      Region.of("fips-us-gov-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("The region field of the ARN being passed as a bucket parameter to an S3 operation does not match the region the client was configured with.");
    }

    @Test
    public void accesspointArn_withFipsRegionPrefix_noFipsInArn_DualstackEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.dualstack.fips-us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder().dualstackEnabled(true),
                             Region.of("fips-us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.dualstack.fips-us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder().dualstackEnabled(true),
                             Region.of("fips-us-east-1"));
    }

    @Test
    public void accesspointArn_withFipsRegionPrefix_FipsInArn_DualStackEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.dualstack.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().dualstackEnabled(true),
                             Region.of("fips-us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.dualstack.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().dualstackEnabled(true),
                             Region.of("fips-us-east-1"));
    }

    @Test
    public void accesspointArn_withFipsRegionSuffix_noFipsinArn_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder(),
                             Region.of("us-east-1-fips"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("us-east-1"),
                             S3Configuration.builder(),
                             Region.of("us-east-1-fips"));
    }

    @Test
    public void accesspointArn_noFipsRegionPrefix_FipsInArn_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder(),
                             Region.of("us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder(),
                             Region.of("us-east-1"));
    }

    @Test
    public void accesspointArn_noFipsRegionPrefix_FipsInArn_useArnRegionEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("us-east-1"));
    }

    @Test
    public void accesspointArn_noFipsRegionPrefix_FipsInArn_useArnRegionEnabled_DualstackEnabled_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "http://foobar-12345678910.s3-accesspoint.dualstack.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true).dualstackEnabled(true),
                             Region.of("us-east-1"));
        verifyAccesspointArn("https",
                             "arn:aws:s3:fips-us-east-1:12345678910:accesspoint/foobar",
                             "https://foobar-12345678910.s3-accesspoint.dualstack.fips-us-east-1.amazonaws.com",
                             Region.of("fips-us-east-1"),
                             S3Configuration.builder().useArnRegionEnabled(true).dualstackEnabled(true),
                             Region.of("us-east-1"));
    }

    @Test
    public void accesspointArn_withAccelerateEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().accelerateModeEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accelerate");
    }


    @Test
    public void accesspointArn_withPathStyleAddressingEnabled_shouldThrowIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3:us-east-1:12345678910:accesspoint/foobar",
                                                      "http://foobar-12345678910.s3-accesspoint.us-east-1.amazonaws.com",
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().pathStyleAccessEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path style");
    }

    @Test
    public void outpostAccessPointArn_shouldConvertEndpoint() {
        verifyAccesspointArn("http",
                             "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                             "http://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-west-2.amazonaws.com",
                             Region.of("us-west-2"),
                             S3Configuration.builder(),
                             Region.of("us-west-2"));

        verifyAccesspointArn("https",
                             "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                             "https://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-west-2.amazonaws.com",
                             Region.of("us-west-2"),
                             S3Configuration.builder(),
                             Region.of("us-west-2"));
    }

    @Test
    public void outpostAccessPointArn_futureUnknownRegion_US_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws:s3-outposts:us-future-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                             "http://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-future-2.amazonaws.com",
                             Region.of("us-future-2"),
                             S3Configuration.builder(),
                             Region.of("us-future-2"));
    }

    @Test
    public void outpostAccessPointArn_futureUnknownRegion_crossRegion_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws:s3-outposts:us-future-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                             "http://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-future-2.amazonaws.com",
                             Region.of("us-future-2"),
                             S3Configuration.builder().useArnRegionEnabled(true),
                             Region.of("us-future-1"));
    }

    @Test
    public void outpostAccessPointArn_futureUnknownRegion_CN_correctlyInfersPartition() {
        verifyAccesspointArn("http",
                             "arn:aws-cn:s3-outposts:cn-future-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                             "http://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.cn-future-1.amazonaws.com.cn",
                             Region.of("cn-future-1"),
                             S3Configuration.builder(),
                             Region.of("cn-future-1"));
    }

    @Test
    public void outpostAccessPointArn_futureUnknownRegionAndPartition_defaultsToAws() {
        verifyAccesspointArn("http",
                             "arn:aws:s3-outposts:unknown:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                             "http://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.unknown.amazonaws.com",
                             Region.of("unknown"),
                             S3Configuration.builder(),
                             Region.of("unknown"));
    }

    @Test
    public void outpostAccessPointArn_invalidPartition_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:bar:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bar");
    }

    @Test
    public void outpostAccessPointArn_differentRegionWithoutUseArnRegion_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:bar:aws-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                                                      null,
                                                      S3Configuration.builder()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("region");
    }

    @Test
    public void outpostAccessPointArn_fipsEnabled_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                                                      null,
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().useArnRegionEnabled(true),
                                                      Region.of("fips-us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FIPS");
    }

    @Test
    public void outpostAccessPointArn_dualStackEnabled_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                                                      null,
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().dualstackEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("dualstack");
    }

    @Test
    public void outpostAccessPointArn_accelerateEnabled_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint",
                                                      null,
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().accelerateModeEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("accelerate");
    }

    @Test
    public void outpostAccessPointArn_ArnMissingAccesspointName_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> verifyAccesspointArn("http",
                                                      "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456",
                                                      null,
                                                      Region.of("us-east-1"),
                                                      S3Configuration.builder().accelerateModeEnabled(true),
                                                      Region.of("us-east-1")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid format");
    }

    private void verifyAccesspointArn(String protocol, String accessPointArn, String expectedEndpoint,
                                      S3Configuration.Builder builder) {
        verifyAccesspointArn(protocol, accessPointArn, expectedEndpoint, Region.US_EAST_1, builder, Region.US_EAST_1);
    }

    private void verifyAccesspointArn(String protocol, String accessPointArn, String expectedEndpoint,
                                      Region expectedSigningRegion,
                                      S3Configuration.Builder configBuilder, Region region) {
        String key = "test-key";

        URI customUri = URI.create(String.format("%s://s3-test.com/%s/%s", protocol, urlEncode(accessPointArn), key));
        URI expectedUri = URI.create(String.format("%s/%s", expectedEndpoint, key));
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(accessPointArn)
                                                            .key(key)
                                                            .build();

        S3EndpointResolverContext context = S3EndpointResolverContext.builder()
                                                                     .request(InterceptorTestUtils.sdkHttpRequest(customUri))
                                                                     .originalRequest(putObjectRequest)
                                                                     .region(region)
                                                                     .serviceConfiguration(configBuilder.build())
                                                                     .build();

        ConfiguredS3SdkHttpRequest sdkHttpFullRequest = endpointResolver.applyEndpointConfiguration(context);

        assertThat(sdkHttpFullRequest.sdkHttpRequest().getUri()).isEqualTo(expectedUri);
        assertThat(sdkHttpFullRequest.signingRegionModification()).isPresent();
        assertThat(sdkHttpFullRequest.signingRegionModification().get()).isEqualTo(expectedSigningRegion);
        assertSigningRegion(accessPointArn, sdkHttpFullRequest);
    }

    private void assertSigningRegion(String accessPointArn, ConfiguredS3SdkHttpRequest sdkHttpFullRequest) {
        if (accessPointArn.contains(":s3-outposts")) {
            String expectedSigningName = "s3-outposts";
            assertThat(sdkHttpFullRequest.signingServiceModification()).isPresent();
            assertThat(sdkHttpFullRequest.signingServiceModification().get()).isEqualTo(expectedSigningName);
        } else {
            assertThat(sdkHttpFullRequest.signingServiceModification()).isEmpty();
        }
    }

}
