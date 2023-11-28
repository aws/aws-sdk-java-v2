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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.LocationInfo;

public class CreateBucketInterceptorTest {

    @Test
    public void modifyRequest_DoesNotOverrideExistingLocationConstraint() {
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
    public void modifyRequest_UpdatesLocationConstraint_When_NullCreateBucketConfiguration() {
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
    public void modifyRequest_UpdatesLocationConstraint_When_NullLocationConstraint() {
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
    public void modifyRequest_DoesNotSetLocationConstraint_When_LocationPresent() {
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
    public void modifyRequest_UsEast1_UsesNullBucketConfiguration() {
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
    public void modifyRequest__When_NullLocationConstraint() {
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
}
