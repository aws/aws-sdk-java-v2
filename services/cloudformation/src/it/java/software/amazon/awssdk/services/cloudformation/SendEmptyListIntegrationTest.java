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

package software.amazon.awssdk.services.cloudformation;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

/**
 * See https://github.com/aws/aws-sdk-java/issues/721. Cloudformation treats empty lists as removing
 * that list of values.
 */
public class SendEmptyListIntegrationTest extends AwsIntegrationTestBase {

    private static final String STARTING_TEMPLATE =
            "{" +
            "   \"AWSTemplateFormatVersion\" : \"2010-09-09\"," +
            "   \"Resources\" : {" +
            "      \"JavaSdkCfSendEmptyListTest\" : {" +
            "         \"Type\" : \"AWS::S3::Bucket\"" +
            "       }" +
            "   }" +
            "}";

    private static final String UPDATED_TEMPLATE =
            "{" +
            "   \"AWSTemplateFormatVersion\" : \"2010-09-09\"," +
            "   \"Resources\" : {" +
            "      \"JavaSdkCfSendEmptyListTestUpdated\" : {" +
            "         \"Type\" : \"AWS::S3::Bucket\"" +
            "       }" +
            "   }" +
            "}";

    private CloudFormationClient cf;
    private CloudFormationWaiter waiter;
    private String stackName;

    @Before
    public void setup() {
        stackName = getClass().getSimpleName() + "-" + System.currentTimeMillis();
        cf = CloudFormationClient.builder()
                                 .credentialsProvider(StaticCredentialsProvider.create(getCredentials()))
                                 .region(Region.US_WEST_2)
                                 .build();

        cf.createStack(CreateStackRequest.builder()
                                         .stackName(stackName)
                                         .templateBody(STARTING_TEMPLATE)
                                         .tags(Tag.builder().key("FooKey").value("FooValue").build()).build());

        waiter = cf.waiter();
        waiter.waitUntilStackCreateComplete(b -> b.stackName(stackName));
    }

    @After
    public void tearDown() {
        cf.deleteStack(DeleteStackRequest.builder().stackName(stackName).build());
    }

    @Test
    public void explicitlyEmptyTagList_RemovesTagsFromStack() {
        assertThat(getTagsForStack(stackName), not(empty()));
        cf.updateStack(UpdateStackRequest.builder()
                                         .stackName(stackName)
                                         .templateBody(STARTING_TEMPLATE)
                                         .tags(Collections.emptyList()).build());

        waiter.waitUntilStackUpdateComplete(b -> b.stackName(stackName));
        assertThat(getTagsForStack(stackName), empty());
    }

    @Test
    public void autoConstructedEmptyTagList_DoesNotRemoveTagsFromStack() {
        assertThat(getTagsForStack(stackName), not(empty()));
        cf.updateStack(UpdateStackRequest.builder()
                                         .stackName(stackName)
                                         .templateBody(UPDATED_TEMPLATE).build());

        waiter.waitUntilStackUpdateComplete(b -> b.stackName(stackName));
        assertThat(getTagsForStack(stackName), not(empty()));
    }

    private List<Tag> getTagsForStack(String stackName) {
        return cf.describeStacks(
                DescribeStacksRequest.builder().stackName(stackName).build())
                 .stacks().get(0)
                 .tags();
    }
}
