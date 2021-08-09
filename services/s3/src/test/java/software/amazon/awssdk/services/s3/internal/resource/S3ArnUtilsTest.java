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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.arns.Arn;
import software.amazon.awssdk.arns.ArnResource;


public class S3ArnUtilsTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void parseS3AccessPointArn_shouldParseCorrectly() {
        S3AccessPointResource s3AccessPointResource = S3ArnUtils.parseS3AccessPointArn(Arn.builder()
                                                                                          .partition("aws")
                                                                                          .service("s3")
                                                                                          .region("us-east-1")
                                                                                          .accountId("123456789012")
                                                                                          .resource("accesspoint:accesspoint-name")
                                                                                          .build());

        assertThat(s3AccessPointResource.accessPointName(), is("accesspoint-name"));
        assertThat(s3AccessPointResource.accountId(), is(Optional.of("123456789012")));
        assertThat(s3AccessPointResource.partition(), is(Optional.of("aws")));
        assertThat(s3AccessPointResource.region(), is(Optional.of("us-east-1")));
        assertThat(s3AccessPointResource.type(), is(S3ResourceType.ACCESS_POINT.toString()));
    }

    @Test
    public void parseOutpostArn_arnWithColon_shouldParseCorrectly() {
        IntermediateOutpostResource intermediateOutpostResource = S3ArnUtils.parseOutpostArn(Arn.builder()
                                                                                                .partition("aws")
                                                                                                .service("s3")
                                                                                                .region("us-east-1")
                                                                                                .accountId("123456789012")
                                                                                                .resource("outpost:22222:accesspoint:foobar")
                                                                                                .build());

        assertThat(intermediateOutpostResource.outpostId(), is("22222"));
        assertThat(intermediateOutpostResource.outpostSubresource(), equalTo(ArnResource.fromString("accesspoint:foobar")));
    }

    @Test
    public void parseOutpostArn_arnWithSlash_shouldParseCorrectly() {
        IntermediateOutpostResource intermediateOutpostResource = S3ArnUtils.parseOutpostArn(Arn.builder()
                                                                                                .partition("aws")
                                                                                                .service("s3")
                                                                                                .region("us-east-1")
                                                                                                .accountId("123456789012")
                                                                                                .resource("outpost/22222/accesspoint/foobar")
                                                                                                .build());

        assertThat(intermediateOutpostResource.outpostId(), is("22222"));
        assertThat(intermediateOutpostResource.outpostSubresource(), equalTo(ArnResource.fromString("accesspoint/foobar")));
    }

    @Test
    public void parseOutpostArn_shouldParseCorrectly() {
        IntermediateOutpostResource intermediateOutpostResource = S3ArnUtils.parseOutpostArn(Arn.builder()
                                                                                                .partition("aws")
                                                                                                .service("s3")
                                                                                                .region("us-east-1")
                                                                                                .accountId("123456789012")
                                                                                                .resource("outpost:22222:futuresegment:foobar")
                                                                                                .build());

        assertThat(intermediateOutpostResource.outpostId(), is("22222"));
        assertThat(intermediateOutpostResource.outpostSubresource(), equalTo(ArnResource.fromString("futuresegment/foobar")));
    }

    @Test
    public void parseOutpostArn_malformedArnNullSubresourceType_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format");
        S3ArnUtils.parseOutpostArn(Arn.builder()
                                      .partition("aws")
                                      .service("s3")
                                      .region("us-east-1")
                                      .accountId("123456789012")
                                      .resource("outpost/22222/")
                                      .build());
    }

    @Test
    public void parseOutpostArn_malformedArnNullSubresource_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Invalid format for S3 Outpost ARN");

        S3ArnUtils.parseOutpostArn(Arn.builder()
                                    .partition("aws")
                                    .service("s3")
                                    .region("us-east-1")
                                    .accountId("123456789012")
                                    .resource("outpost:op-01234567890123456:accesspoint")
                                    .build());
    }

    @Test
    public void parseOutpostArn_malformedArnEmptyOutpostId_shouldThrowException() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("resource must not be blank or empty");

        S3ArnUtils.parseOutpostArn(Arn.builder()
                                      .partition("aws")
                                      .service("s3")
                                      .region("us-east-1")
                                      .accountId("123456789012")
                                      .resource("outpost::accesspoint:name")
                                      .build());
    }
}
