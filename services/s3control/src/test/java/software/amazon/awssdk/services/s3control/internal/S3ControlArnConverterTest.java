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
package software.amazon.awssdk.services.s3control.internal;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.services.s3.internal.resource.S3AccessPointResource;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostResource;
import software.amazon.awssdk.services.s3.internal.resource.S3Resource;
import software.amazon.awssdk.services.s3control.S3ControlBucketResource;

public class S3ControlArnConverterTest {

    private static final S3ControlArnConverter ARN_PARSER = S3ControlArnConverter.getInstance();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void parseArn_outpostBucketArn() {
        S3Resource resource = ARN_PARSER.convertArn(Arn.builder()
                                                       .partition("aws")
                                                       .service("s3")
                                                       .region("us-east-1")
                                                       .accountId("123456789012")
                                                       .resource("outpost/1234/bucket/myBucket")
                                                       .build());

        assertThat(resource, instanceOf(S3ControlBucketResource.class));

        S3ControlBucketResource bucketResource = (S3ControlBucketResource) resource;
        assertThat(bucketResource.bucketName(), is("myBucket"));

        assertThat(bucketResource.parentS3Resource().get(), instanceOf(S3OutpostResource.class));
        S3OutpostResource outpostResource = (S3OutpostResource) bucketResource.parentS3Resource().get();

        assertThat(outpostResource.accountId(), is(Optional.of("123456789012")));
        assertThat(outpostResource.partition(), is(Optional.of("aws")));
        assertThat(outpostResource.region(), is(Optional.of("us-east-1")));
        assertThat(outpostResource.type(), is(S3ControlResourceType.OUTPOST.toString()));
        assertThat(outpostResource.outpostId(), is("1234"));
    }

    @Test
    public void parseArn_outpostAccessPointArn() {
        S3Resource resource = ARN_PARSER.convertArn(Arn.builder()
                                                       .partition("aws")
                                                       .service("s3-outposts")
                                                       .region("us-east-1")
                                                       .accountId("123456789012")
                                                       .resource("outpost/1234/accesspoint/myAccessPoint")
                                                       .build());

        assertThat(resource, instanceOf(S3AccessPointResource.class));

        S3AccessPointResource accessPointResource = (S3AccessPointResource) resource;
        assertThat(accessPointResource.accessPointName(), is("myAccessPoint"));

        assertThat(accessPointResource.parentS3Resource().get(), instanceOf(S3OutpostResource.class));
        S3OutpostResource outpostResource = (S3OutpostResource) accessPointResource.parentS3Resource().get();

        assertThat(outpostResource.outpostId(), is("1234"));
        assertThat(outpostResource.accountId(), is(Optional.of("123456789012")));
        assertThat(outpostResource.partition(), is(Optional.of("aws")));
        assertThat(outpostResource.region(), is(Optional.of("us-east-1")));
    }

    @Test
    public void parseArn_invalidOutpostAccessPointMissingAccessPointName_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");

        ARN_PARSER.convertArn(Arn.builder()
                                 .partition("aws")
                                 .service("s3")
                                 .region("us-east-1")
                                 .accountId("123456789012")
                                 .resource("outpost:op-01234567890123456:accesspoint")
                                 .build());
    }

    @Test
    public void parseArn_invalidOutpostAccessPointMissingOutpostId_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");

        ARN_PARSER.convertArn(Arn.builder()
                                 .partition("aws")
                                 .service("s3")
                                 .region("us-east-1")
                                 .accountId("123456789012")
                                 .resource("outpost/myaccesspoint")
                                 .build());
    }

    @Test
    public void parseArn_malformedOutpostArn_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unknown outpost ARN");

        ARN_PARSER.convertArn(Arn.builder()
                                 .partition("aws")
                                 .service("s3")
                                 .region("us-east-1")
                                 .accountId("123456789012")
                                 .resource("outpost:1:accesspoin1:1")
                                 .build());
    }

    @Test
    public void parseArn_unknownResource() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("ARN type");
        ARN_PARSER.convertArn(Arn.builder()
                                 .partition("aws")
                                 .service("s3")
                                 .region("us-east-1")
                                 .accountId("123456789012")
                                 .resource("unknown:foobar")
                                 .build());
    }

    @Test
    public void parseArn_unknownType_throwsCorrectException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("invalidType");

        ARN_PARSER.convertArn(Arn.builder()
                                 .partition("aws")
                                 .service("s3")
                                 .region("us-east-1")
                                 .accountId("123456789012")
                                 .resource("invalidType:something")
                                 .build());
    }
}
