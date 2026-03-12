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
package software.amazon.awssdk.services.s3.internal.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.LocationInfo;
import software.amazon.awssdk.services.s3.model.Tag;

public class CreateBucketInterceptorTest {

    @Test
    void modifyRequest_DoesNotOverrideExistingLocationConstraint() {
        CreateBucketRequest request = CreateBucketRequest.builder()
                                                         .bucket("test-bucket")
                                                         .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                                             .locationConstraint(
                                                                                                                     "us-west-2")
                                                                                                             .build())
                                                         .build();

        Context.ModifyRequest context = () -> request;

        ExecutionAttributes attributes = new ExecutionAttributes()
                .putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);

        SdkRequest modifiedRequest = new CreateBucketInterceptor().modifyRequest(context, attributes);
        String locationConstraint = ((CreateBucketRequest) modifiedRequest).createBucketConfiguration().locationConstraintAsString();

        assertThat(locationConstraint).isEqualToIgnoringCase("us-west-2");
    }

    @Test
    void modifyRequest_UpdatesLocationConstraint_When_NullCreateBucketConfiguration() {
        CreateBucketRequest request = CreateBucketRequest.builder()
                                                         .bucket("test-bucket")
                                                         .build();

        Context.ModifyRequest context = () -> request;

        ExecutionAttributes attributes = new ExecutionAttributes()
                .putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_2);

        SdkRequest modifiedRequest = new CreateBucketInterceptor().modifyRequest(context, attributes);
        String locationConstraint = ((CreateBucketRequest) modifiedRequest).createBucketConfiguration().locationConstraintAsString();

        assertThat(locationConstraint).isEqualToIgnoringCase("us-east-2");
    }

    @Test
    void modifyRequest_UpdatesLocationConstraint_When_NullLocationConstraint() {
        CreateBucketRequest request = CreateBucketRequest.builder()
                                                         .bucket("test-bucket")
                                                         .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                                             .build())
                                                         .build();

        Context.ModifyRequest context = () -> request;

        ExecutionAttributes attributes = new ExecutionAttributes()
                .putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_WEST_2);

        SdkRequest modifiedRequest = new CreateBucketInterceptor().modifyRequest(context, attributes);
        String locationConstraint = ((CreateBucketRequest) modifiedRequest).createBucketConfiguration().locationConstraintAsString();

        assertThat(locationConstraint).isEqualToIgnoringCase("us-west-2");
    }

    @Test
    void modifyRequest_DoesNotSetLocationConstraint_When_LocationPresent() {
        LocationInfo location = LocationInfo.builder().name("name").type("type").build();
        CreateBucketConfiguration configuration = CreateBucketConfiguration.builder().location(location).build();

        CreateBucketRequest request = CreateBucketRequest.builder()
                                                         .bucket("test-bucket")
                                                         .createBucketConfiguration(configuration)
                                                         .build();

        Context.ModifyRequest context = () -> request;

        ExecutionAttributes attributes = new ExecutionAttributes().putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_WEST_2);

        SdkRequest modifiedRequest = new CreateBucketInterceptor().modifyRequest(context, attributes);

        CreateBucketConfiguration bucketConfiguration = ((CreateBucketRequest) modifiedRequest).createBucketConfiguration();
        BucketLocationConstraint locationConstraint = bucketConfiguration.locationConstraint();

        assertThat(locationConstraint).isNull();
    }

    /**
     * For us-east-1 there must not be a location constraint (or containing CreateBucketConfiguration) sent.
     */
    @Test
    void modifyRequest_UsEast1_UsesNullBucketConfiguration() {
        CreateBucketRequest request = CreateBucketRequest.builder()
                                                         .bucket("test-bucket")
                                                         .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                                             .build())
                                                         .build();

        Context.ModifyRequest context = () -> request;

        ExecutionAttributes attributes = new ExecutionAttributes()
            .putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);

        SdkRequest modifiedRequest = new CreateBucketInterceptor().modifyRequest(context, attributes);
        assertThat(((CreateBucketRequest) modifiedRequest).createBucketConfiguration()).isNull();
    }

    @Test
    void modifyRequest__When_NullLocationConstraint() {
        CreateBucketRequest request = CreateBucketRequest.builder()
                                                         .bucket("test-bucket")
                                                         .createBucketConfiguration(CreateBucketConfiguration.builder()
                                                                                                             .build())
                                                         .build();

        Context.ModifyRequest context = () -> request;

        ExecutionAttributes attributes = new ExecutionAttributes()
            .putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_WEST_2);

        SdkRequest modifiedRequest = new CreateBucketInterceptor().modifyRequest(context, attributes);
        String locationConstraint =
            ((CreateBucketRequest) modifiedRequest).createBucketConfiguration().locationConstraintAsString();

        assertThat(locationConstraint).isEqualToIgnoringCase("us-west-2");
    }

    /*
    Tags on create test cases:
        Region     has tags      locationConstraint  Expected CreateBucketConfiguration body
        ----------------------------------------------------------------
        us-east-1 -    tags - no locationConstraint: Body with tags only
        us-east-1 -    tags -    locationConstraint: Body with both (locationConstraint=EU)
        us-east-1 - no tags - no locationConstraint: Empty body (null)
        us-east-1 - no tags -    locationConstraint: Body with locationConstraint=EU

        us-west-2 -    tags - no locationConstraint: Body with tags and locationConstraint=us-west-2
        us-west-2 -    tags -    locationConstraint: Body with both (locationConstraint=EU)
        us-west-2 - no tags - no locationConstraint: Body with locationConstraint=us-west-2
        us-west-2 - no tags -    locationConstraint: Body with locationConstraint=EU
     */
    public static Stream<Arguments> createWithTagsTestParam() {
        List<Tag> tags = Arrays.asList(
            Tag.builder().key("foo-key").value("foo-value").build(),
            Tag.builder().key("bar-key").value("bar-value").build()
        );
        return Stream.of(
            Arguments.of(Region.US_EAST_1, tags, null,
                         CreateBucketConfiguration.builder().tags(tags).build()),
            Arguments.of(Region.US_EAST_1, tags, BucketLocationConstraint.EU,
                         CreateBucketConfiguration.builder().tags(tags).locationConstraint(BucketLocationConstraint.EU).build()),
            Arguments.of(Region.US_EAST_1, null, null, null),
            Arguments.of(Region.US_EAST_1, null, BucketLocationConstraint.EU,
                         CreateBucketConfiguration.builder().locationConstraint(BucketLocationConstraint.EU).build()),

            Arguments.of(Region.US_WEST_2, tags, null,
                         CreateBucketConfiguration.builder().tags(tags).locationConstraint(BucketLocationConstraint.US_WEST_2).build()),
            Arguments.of(Region.US_WEST_2, tags, BucketLocationConstraint.EU,
                         CreateBucketConfiguration.builder().tags(tags).locationConstraint(BucketLocationConstraint.EU).build()),
            Arguments.of(Region.US_WEST_2, null, null,
                         CreateBucketConfiguration.builder().locationConstraint(BucketLocationConstraint.US_WEST_2).build()),
            Arguments.of(Region.US_WEST_2, null, BucketLocationConstraint.EU,
                         CreateBucketConfiguration.builder().locationConstraint(BucketLocationConstraint.EU).build())
        );
    }

    @ParameterizedTest
    @MethodSource("createWithTagsTestParam")
    void modifyRequest__CreateWithTags(Region region,
                                       List<Tag> tags,
                                       BucketLocationConstraint locationConstraint,
                                       CreateBucketConfiguration expected) {
        CreateBucketRequest request = CreateBucketRequest.builder()
                                                         .bucket("test-bucket")
                                                         .createBucketConfiguration(
                                                             CreateBucketConfiguration.builder()
                                                                                      .tags(tags)
                                                                                      .locationConstraint(locationConstraint)
                                                                                      .build())
                                                         .build();

        Context.ModifyRequest context = () -> request;
        ExecutionAttributes attributes = new ExecutionAttributes().putAttribute(AwsExecutionAttribute.AWS_REGION, region);

        CreateBucketRequest modifiedRequest =
            (CreateBucketRequest) new CreateBucketInterceptor().modifyRequest(context, attributes);

        assertThat(modifiedRequest.createBucketConfiguration()).isEqualTo(expected);

    }

}
