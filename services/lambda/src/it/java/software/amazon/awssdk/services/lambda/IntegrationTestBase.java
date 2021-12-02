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

package software.amazon.awssdk.services.lambda;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AttachRolePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreatePolicyRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleRequest;
import software.amazon.awssdk.services.iam.model.CreateRoleResponse;
import software.amazon.awssdk.services.iam.model.DeletePolicyRequest;
import software.amazon.awssdk.services.iam.model.DeleteRoleRequest;
import software.amazon.awssdk.services.iam.model.DetachRolePolicyRequest;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.StreamDescription;
import software.amazon.awssdk.services.kinesis.model.StreamStatus;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class IntegrationTestBase extends AwsTestBase {

    private static final Duration MAX_WAIT_TIME = Duration.ofMinutes(2);
    private static final String HELLOWORLD_JS = "helloworld.js";
    private static final String LAMBDA_SERVICE_ROLE_NAME = "lambda-java-sdk-test-role-" + System.currentTimeMillis();
    private static final String LAMBDA_SERVICE_ROLE_POLICY_NAME = LAMBDA_SERVICE_ROLE_NAME + "-policy";
    private static final String LAMBDA_ROLE_EXECUTION_POLICY = "{" + "\"Version\": \"2012-10-17\","
                                                               + "\"Statement\": [" + "{" + "\"Sid\": \"\"," + "\"Effect\": \"Allow\"," + "\"Action\": \"kinesis:*\","
                                                               + "\"Resource\": \"*\"" + "}" + "]" + "}";
    private static final String LAMBDA_ASSUME_ROLE_POLICY = "{" + "\"Version\": \"2012-10-17\"," + "\"Statement\": ["
                                                            + "{" + "\"Sid\": \"\"," + "\"Effect\": \"Allow\"," + "\"Principal\": {"
                                                            + "\"Service\": \"lambda.amazonaws.com\"" + "}," + "\"Action\": \"sts:AssumeRole\"" + "}" + "]" + "}";
    private static final String KINESIS_STREAM_NAME = "lambda-java-sdk-test-kinesis-stream-"
                                                      + System.currentTimeMillis();
    protected static LambdaAsyncClient lambda;
    protected static File cloudFuncZip;
    protected static String lambdaServiceRoleArn;
    protected static String streamArn;
    private static String roleExecutionPolicyArn;
    private static IamClient iam;
    private static KinesisClient kinesis;

    @BeforeClass
    public static void setup() throws IOException {
        setUpCredentials();
        lambda = LambdaAsyncClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_WEST_2).build();

        cloudFuncZip = setupFunctionZip(HELLOWORLD_JS);

        createLambdaServiceRole();
    }

    @AfterClass
    public static void tearDown() {
        iam.detachRolePolicy(DetachRolePolicyRequest.builder().roleName(LAMBDA_SERVICE_ROLE_NAME).policyArn(
                roleExecutionPolicyArn).build());

        iam.deletePolicy(DeletePolicyRequest.builder().policyArn(roleExecutionPolicyArn).build());

        iam.deleteRole(DeleteRoleRequest.builder().roleName(LAMBDA_SERVICE_ROLE_NAME).build());

        if (kinesis != null) {
            kinesis.deleteStream(DeleteStreamRequest.builder().streamName(KINESIS_STREAM_NAME).build());
            kinesis = null;
        }
    }

    private static File setupFunctionZip(String jsFile) throws IOException {
        InputStream in = IntegrationTestBase.class.getResourceAsStream(jsFile);

        File zipFile = File.createTempFile("lambda-cloud-function", ".zip");
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

        out.putNextEntry(new ZipEntry(jsFile));

        byte[] b = new byte[1024];
        int count;

        while ((count = in.read(b)) != -1) {
            out.write(b, 0, count);
        }

        out.close();
        in.close();

        return zipFile;
    }

    private static void createLambdaServiceRole() {
        iam = IamClient.builder()
                .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                .region(Region.AWS_GLOBAL)
                .build();

        CreateRoleResponse result = iam.createRole(CreateRoleRequest.builder().roleName(LAMBDA_SERVICE_ROLE_NAME)
                                                                        .assumeRolePolicyDocument(LAMBDA_ASSUME_ROLE_POLICY).build());

        lambdaServiceRoleArn = result.role().arn();

        roleExecutionPolicyArn = iam
                .createPolicy(CreatePolicyRequest.builder().policyName(LAMBDA_SERVICE_ROLE_POLICY_NAME).policyDocument(
                                LAMBDA_ROLE_EXECUTION_POLICY).build()).policy().arn();

        iam.attachRolePolicy(AttachRolePolicyRequest.builder().roleName(LAMBDA_SERVICE_ROLE_NAME).policyArn(
                roleExecutionPolicyArn).build());
    }

    protected static void createKinesisStream() {
        kinesis = KinesisClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).region(Region.US_WEST_2).build();

        kinesis.createStream(CreateStreamRequest.builder().streamName(KINESIS_STREAM_NAME).shardCount(1).build());

        StreamDescription description = kinesis.describeStream(DescribeStreamRequest.builder().streamName(KINESIS_STREAM_NAME).build())
                .streamDescription();
        streamArn = description.streamARN();

        // Wait till stream is active (less than a minute)
        Instant start = Instant.now();
        while (StreamStatus.ACTIVE != description.streamStatus()) {
            if (Duration.between(start, Instant.now()).toMillis() > MAX_WAIT_TIME.toMillis()) {
                throw new RuntimeException("Timed out waiting for stream to become active");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                // Ignored or expected.
            }

            description = kinesis.describeStream(DescribeStreamRequest.builder().streamName(KINESIS_STREAM_NAME).build())
                    .streamDescription();
        }
    }

    protected static byte[] read(InputStream stream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        int len;
        while ((len = stream.read(buffer)) >= 0) {
            result.write(buffer, 0, len);
        }

        return result.toByteArray();
    }
}
