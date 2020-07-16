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

package software.amazon.awssdk.services.s3;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.testutils.service.AwsTestBase.CREDENTIALS_PROVIDER_CHAIN;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.resource.S3OutpostAccessPointBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

/**
 * To run the test, change line 84 in {@link S3OutpostAccessPointBuilder#toUri()}
 * to String uriString = String.format("%s://%s-%s.%s.beta.%s.seaport.aws.a2z.com", protocol, accessPointName, accountId, outpostId, region);
 *
 * //TODO: change to use prod endpoint when it's ready
 */
public class OutpostIntegrationTest {
    private static final String ROLE_ARN = "arn:aws:iam::586467309111:role/SeaportTestingRole";
    private static final String BUCKET_ARN = "arn:aws:s3-outposts:us-east-1:586467309111:outpost/ec2-internal/accesspoint/sdk-integration";
    private static final String KEY = "java-sdk-v1-outposts";
    public static final byte[] BYTES = "helloworld".getBytes();

    private static StsClient securityTokenService;
    private static S3Client s3Client;
    private static S3AsyncClient s3AsyncClient;

    @BeforeClass
    public static void setup() {
        securityTokenService = StsClient.builder()
                                        .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                        .region(Region.US_WEST_2)
                                        .build();

        StsAssumeRoleCredentialsProvider stsCredentialProvider =
            StsAssumeRoleCredentialsProvider.builder().refreshRequest(b -> b.roleArn(ROLE_ARN).roleSessionName("s3-outposts"))
                                            .stsClient(securityTokenService)
                                            .build();

        s3Client = S3Client.builder()
                           .serviceConfiguration(S3Configuration.builder().useArnRegionEnabled(true).build())
                           .region(Region.US_WEST_2)
                           .credentialsProvider(stsCredentialProvider)
                           .build();

        s3AsyncClient = S3AsyncClient.builder()
                                     .serviceConfiguration(S3Configuration.builder().useArnRegionEnabled(true).build())
                                     .region(Region.US_WEST_2)
                                     .credentialsProvider(stsCredentialProvider)
                                     .build();

    }

    @AfterClass
    public static void tearDown() {
        securityTokenService.close();
        s3Client.close();
        s3AsyncClient.close();
    }

    @Test
    @Ignore("Temporary integ test to make sure there's no regression during development")
    public void sync() {
        PutObjectResponse putObjectResult = s3Client.putObject(b -> b.bucket(BUCKET_ARN).key(KEY), RequestBody.fromBytes(
            BYTES));
        assertEquals("helloworld", s3Client.getObjectAsBytes(b -> b.bucket(BUCKET_ARN).key(KEY)).asUtf8String());
    }

    @Test
    @Ignore("Temporary integ test to make sure there's no regression during development")
    public void async() {
        PutObjectResponse putObjectResult = s3AsyncClient.putObject(b -> b.bucket(BUCKET_ARN).key(KEY), AsyncRequestBody.fromBytes(
            BYTES)).join();
        assertEquals("helloworld", s3AsyncClient.getObject(b -> b.bucket(BUCKET_ARN).key(KEY), AsyncResponseTransformer.toBytes()).join().asUtf8String());
    }
}