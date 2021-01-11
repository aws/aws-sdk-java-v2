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

package software.amazon.awssdk.services.s3.internal.resource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import software.amazon.awssdk.arns.Arn;

public class S3ArnConverterTest {
    private static final S3ArnConverter S3_ARN_PARSER = S3ArnConverter.create();
    private static final String ACCOUNT_ID = "123456789012";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void parseArn_objectThroughAp_v2Arn() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .region("us-east-1")
                                                          .accountId("123456789012")
                                                          .resource("accesspoint:test-ap/object/test-key")
                                                          .build());

        assertThat(resource, instanceOf(S3ObjectResource.class));

        S3ObjectResource s3ObjectResource = (S3ObjectResource) resource;
        assertThat(s3ObjectResource.parentS3Resource().get(), instanceOf(S3AccessPointResource.class));
        S3AccessPointResource s3AccessPointResource = (S3AccessPointResource)s3ObjectResource.parentS3Resource().get();
        assertThat(s3AccessPointResource.accessPointName(), is("test-ap"));
        assertThat(s3ObjectResource.key(), is("test-key"));
        assertThat(s3ObjectResource.accountId(), is(Optional.of("123456789012")));
        assertThat(s3ObjectResource.partition(), is(Optional.of("aws")));
        assertThat(s3ObjectResource.region(), is(Optional.of("us-east-1")));
        assertThat(s3ObjectResource.type(), is(S3ResourceType.OBJECT.toString()));
    }

    @Test
    public void parseArn_object_v1Arn() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .resource("bucket/key")
                                                          .build());

        assertThat(resource, instanceOf(S3ObjectResource.class));
        S3ObjectResource s3ObjectResource = (S3ObjectResource) resource;
        assertThat(s3ObjectResource.parentS3Resource().get(), instanceOf(S3BucketResource.class));
        S3BucketResource s3BucketResource = (S3BucketResource) s3ObjectResource.parentS3Resource().get();

        assertThat(s3BucketResource.bucketName(), is("bucket"));
        assertThat(s3ObjectResource.key(), is("key"));
        assertThat(s3ObjectResource.accountId(), is(Optional.empty()));
        assertThat(s3ObjectResource.partition(), is(Optional.of("aws")));
        assertThat(s3ObjectResource.region(), is(Optional.empty()));
        assertThat(s3ObjectResource.type(), is(S3ResourceType.OBJECT.toString()));
    }

    @Test
    public void parseArn_accessPoint() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .region("us-east-1")
                                                          .accountId("123456789012")
                                                          .resource("accesspoint:accesspoint-name")
                                                          .build());

        assertThat(resource, instanceOf(S3AccessPointResource.class));

        S3AccessPointResource s3EndpointResource = (S3AccessPointResource) resource;
        assertThat(s3EndpointResource.accessPointName(), is("accesspoint-name"));
        assertThat(s3EndpointResource.accountId(), is(Optional.of("123456789012")));
        assertThat(s3EndpointResource.partition(), is(Optional.of("aws")));
        assertThat(s3EndpointResource.region(), is(Optional.of("us-east-1")));
        assertThat(s3EndpointResource.type(), is(S3ResourceType.ACCESS_POINT.toString()));
    }

    @Test
    public void parseArn_accessPoint_withQualifier() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .region("us-east-1")
                                                          .accountId("123456789012")
                                                          .resource("accesspoint:accesspoint-name:1214234234")
                                                          .build());

        assertThat(resource, instanceOf(S3AccessPointResource.class));

        S3AccessPointResource s3EndpointResource = (S3AccessPointResource) resource;
        assertThat(s3EndpointResource.accessPointName(), is("accesspoint-name"));
        assertThat(s3EndpointResource.accountId(), is(Optional.of("123456789012")));
        assertThat(s3EndpointResource.partition(), is(Optional.of("aws")));
        assertThat(s3EndpointResource.region(), is(Optional.of("us-east-1")));
        assertThat(s3EndpointResource.type(), is(S3ResourceType.ACCESS_POINT.toString()));
    }

    @Test
    public void parseArn_v1Bucket() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .resource("bucket-name")
                                                          .build());

        assertThat(resource, instanceOf(S3BucketResource.class));

        S3BucketResource s3BucketResource = (S3BucketResource) resource;
        assertThat(s3BucketResource.bucketName(), is("bucket-name"));
        assertThat(s3BucketResource.accountId(), is(Optional.empty()));
        assertThat(s3BucketResource.partition(), is(Optional.of("aws")));
        assertThat(s3BucketResource.region(), is(Optional.empty()));
        assertThat(s3BucketResource.type(), is(S3ResourceType.BUCKET.toString()));
    }

    @Test
    public void parseArn_v2Bucket() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .region("us-east-1")
                                                          .accountId("123456789012")
                                                          .resource("bucket_name:bucket-name")
                                                          .build());

        assertThat(resource, instanceOf(S3BucketResource.class));

        S3BucketResource s3BucketResource = (S3BucketResource) resource;
        assertThat(s3BucketResource.bucketName(), is("bucket-name"));
        assertThat(s3BucketResource.accountId(), is(Optional.of("123456789012")));
        assertThat(s3BucketResource.partition(), is(Optional.of("aws")));
        assertThat(s3BucketResource.region(), is(Optional.of("us-east-1")));
        assertThat(s3BucketResource.type(), is(S3ResourceType.BUCKET.toString()));
    }

    @Test
    public void parseArn_unknownResource() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("ARN type");
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("unknown:foobar")
                                    .build());
    }

    @Test
    public void parseArn_bucket_noName() {
        exception.expect(IllegalArgumentException.class);
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("bucket_name:")
                                    .build());
    }

    @Test
    public void parseArn_accesspoint_noName() {
        exception.expect(IllegalArgumentException.class);
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("access_point:")
                                    .build());
    }

    @Test
    public void parseArn_object_v2Arn_noKey() {
        exception.expect(IllegalArgumentException.class);
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("object:bucket")
                                    .build());
    }

    @Test
    public void parseArn_object_v2Arn_emptyBucket() {
        exception.expect(IllegalArgumentException.class);
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("object:/key")
                                    .build());
    }

    @Test
    public void parseArn_object_v2Arn_emptyKey() {
        exception.expect(IllegalArgumentException.class);
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("object:bucket/")
                                    .build());
    }

    @Test
    public void parseArn_object_v1Arn_emptyKey() {
        exception.expect(IllegalArgumentException.class);
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .resource("bucket/")
                                    .build());
    }

    @Test
    public void parseArn_object_v1Arn_emptyBucket() {
        exception.expect(IllegalArgumentException.class);
        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .resource("/key")
                                    .build());
    }

    @Test
    public void parseArn_unknownType_throwsCorrectException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("invalidType");

        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("invalidType:something")
                                    .build());
    }

    @Test
    public void parseArn_outpostAccessPoint_slash() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .region("us-east-1")
                                                          .accountId(ACCOUNT_ID)
                                                          .resource("outpost/22222/accesspoint/foobar")
                                                          .build());

        assertThat(resource, instanceOf(S3AccessPointResource.class));
        S3AccessPointResource s3AccessPointResource = (S3AccessPointResource) resource;
        assertThat(s3AccessPointResource.accessPointName(), is("foobar"));
        assertThat(s3AccessPointResource.parentS3Resource().get(), instanceOf(S3OutpostResource.class));
        S3OutpostResource outpostResource = (S3OutpostResource)s3AccessPointResource.parentS3Resource().get();
        assertThat(outpostResource.accountId(), is(Optional.of(ACCOUNT_ID)));
        assertThat(outpostResource.partition(), is(Optional.of("aws")));
        assertThat(outpostResource.region(), is(Optional.of("us-east-1")));
        assertThat(outpostResource.outpostId(), is("22222"));
        assertThat(outpostResource.type(), is(S3ResourceType.OUTPOST.toString()));
    }

    @Test
    public void parseArn_outpostAccessPoint_colon() {
        S3Resource resource = S3_ARN_PARSER.convertArn(Arn.builder()
                                                          .partition("aws")
                                                          .service("s3")
                                                          .region("us-east-1")
                                                          .accountId(ACCOUNT_ID)
                                                          .resource("outpost:22222:accesspoint:foobar")
                                                          .build());

        assertThat(resource, instanceOf(S3AccessPointResource.class));
        S3AccessPointResource s3AccessPointResource = (S3AccessPointResource) resource;
        assertThat(s3AccessPointResource.accessPointName(), is("foobar"));

        assertThat(s3AccessPointResource.parentS3Resource().get(), instanceOf(S3OutpostResource.class));

        S3OutpostResource outpostResource = (S3OutpostResource)s3AccessPointResource.parentS3Resource().get();

        assertThat(outpostResource.accountId(), is(Optional.of(ACCOUNT_ID)));
        assertThat(outpostResource.partition(), is(Optional.of("aws")));
        assertThat(outpostResource.region(), is(Optional.of("us-east-1")));
        assertThat(outpostResource.outpostId(), is("22222"));
        assertThat(outpostResource.type(), is(S3ResourceType.OUTPOST.toString()));
    }

    @Test
    public void parseArn_invalidOutpostAccessPointMissingAccessPointName_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");

        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId(ACCOUNT_ID)
                                    .resource("outpost:op-01234567890123456:accesspoint")
                                    .build());
    }

    @Test
    public void parseArn_invalidOutpostAccessPointMissingOutpostId_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");

        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId(ACCOUNT_ID)
                                    .resource("outpost/myaccesspoint")
                                    .build());
    }

    @Test
    public void parseArn_malformedOutpostArn_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unknown outpost ARN type");

        S3_ARN_PARSER.convertArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId(ACCOUNT_ID)
                                    .resource("outpost:1:accesspoin1:1")
                                    .build());
    }
}