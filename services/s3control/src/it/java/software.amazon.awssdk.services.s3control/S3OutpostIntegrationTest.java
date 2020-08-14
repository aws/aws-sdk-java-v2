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

package software.amazon.awssdk.services.s3control;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.model.CreateAccessPointRequest;
import software.amazon.awssdk.services.s3control.model.CreateAccessPointResponse;
import software.amazon.awssdk.services.s3control.model.ExpirationStatus;
import software.amazon.awssdk.services.s3control.model.GetBucketResponse;
import software.amazon.awssdk.services.s3control.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3control.model.PutBucketTaggingResponse;
import software.amazon.awssdk.services.s3control.model.S3ControlException;
import software.amazon.awssdk.services.s3control.model.S3Tag;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;

public class S3OutpostIntegrationTest extends AwsIntegrationTestBase {

    private static final String ROLE_ARN = "arn:aws:iam::045140586512:role/SeaportBucketAPIsTestRole";
    private static final String ACCESS_POINT_NAME = "outpost-java-v2" + System.currentTimeMillis();
    private static final String OUTPOST_ID = "op-0d79779cef3c30a40";
    private static final String ACCOUNT_ID = "045140586512";
    private static final String BUCKET_NAME = "outpost-java-sdk-v2" + System.currentTimeMillis();
    private static final String BUCKET_ARN = "arn:aws:s3-outposts:us-west-2:045140586512:outpost/op-0d79779cef3c30a40/bucket/" + BUCKET_NAME;

    private static StsClient stsClient;
    private static S3ControlClient s3ControlClient;
    private static String accessPointArn;

    @BeforeClass
    public static void setup() {
        stsClient = StsClient.builder()
                             .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                             .region(Region.US_WEST_2)
                             .build();

        s3ControlClient = S3ControlClient.builder()
                                         .region(Region.US_WEST_2)
                                         .credentialsProvider(StsAssumeRoleCredentialsProvider.builder()
                                                                                              .refreshRequest(b -> b.roleArn(ROLE_ARN).roleSessionName("s3-outposts"))
                                                                                              .stsClient(stsClient)
                                                                                              .build())
                                         .build();

        s3ControlClient.createBucket(b -> b.outpostId(OUTPOST_ID).bucket(BUCKET_NAME));

        CreateAccessPointRequest request = CreateAccessPointRequest.builder()
                                                                   .bucket(BUCKET_ARN)
                                                                   .vpcConfiguration(b -> b.vpcId("123"))
                                                                   .name(ACCESS_POINT_NAME)
                                                                   .build();
        CreateAccessPointResponse accessPointResult = s3ControlClient.createAccessPoint(request);
        accessPointArn = accessPointResult.accessPointArn();
    }

    @AfterClass
    public static void tearDown() {
        try {
            s3ControlClient.deleteBucket(b -> b.bucket(BUCKET_ARN));
        } catch (S3ControlException exception) {
            assertThat(exception.awsErrorDetails().errorCode(), containsString("InvalidBucketState"));
        }

        try {
            s3ControlClient.deleteAccessPoint(b -> b.name(accessPointArn));
        } catch (S3ControlException exception) {
            assertThat(exception.getMessage(), containsString("Access Point is not in a state where it can be deleted"));
        }

        stsClient.close();
        s3ControlClient.close();
    }

    @Test
    public void bucketOperation() {

        GetBucketResponse getBucketResult = s3ControlClient.getBucket(b -> b.bucket(BUCKET_ARN));

        s3ControlClient.listRegionalBuckets(b -> b.outpostId(OUTPOST_ID).accountId(ACCOUNT_ID));

    }

    @Test
    public void bucketTaggingOperation() {
        PutBucketTaggingResponse putBucketTaggingResponse =
            s3ControlClient.putBucketTagging(b -> b.bucket(BUCKET_ARN).tagging(t -> t.tagSet(S3Tag.builder().key("test").value(
                "tests").build())));
        assertNotNull(putBucketTaggingResponse);
        GetBucketTaggingResponse bucketTagging = s3ControlClient.getBucketTagging(b -> b.bucket(BUCKET_ARN));
        assertNotNull(bucketTagging);
        assertNotNull(s3ControlClient.deleteBucketTagging(b -> b.bucket(BUCKET_ARN)));
    }

    @Test
    public void bucketPolicyOperation() {
        String bucketPolicy = String.format("{\"Version\": \"2012-10-17\", \"Statement\": [{ \"Sid\": \"id-1\",\"Effect\": \"Allow\","
                              + "\"Principal\": {\"AWS\": \"arn:aws:iam::045140586512:root\"}, \"Action\": [ \"*:*\" ], "
                              + "\"Resource\": [\"%s\" ] } ]}", BUCKET_ARN);
        assertNotNull(s3ControlClient.putBucketPolicy(b -> b.bucket(BUCKET_ARN).policy(bucketPolicy)));
        assertNotNull(s3ControlClient.getBucketPolicy(b -> b.bucket(BUCKET_ARN)));
        assertNotNull(s3ControlClient.deleteBucketPolicy(b -> b.bucket(BUCKET_ARN)));
    }

    @Test
    public void bucketLifecycleOperation() {
        assertNotNull(s3ControlClient.putBucketLifecycleConfiguration(b -> b.bucket(BUCKET_ARN)
                                                              .lifecycleConfiguration(l -> l.rules(r -> r.abortIncompleteMultipartUpload(a -> a.daysAfterInitiation(1))
                                                                                                         .status(ExpirationStatus.ENABLED)))));
        assertNotNull(s3ControlClient.getBucketLifecycleConfiguration(b -> b.bucket(BUCKET_ARN)));
        assertNotNull(s3ControlClient.deleteBucketLifecycleConfiguration(b -> b.bucket(BUCKET_ARN)));
    }

    @Test
    public void accessPointOperation() {

        assertNotNull(s3ControlClient.listAccessPoints(b -> b.bucket(BUCKET_ARN).maxResults(1)));

        String policy = String.format("{\"Version\": \"2012-10-17\", \"Statement\": [{ \"Sid\": \"id-1\",\"Effect\": \"Allow\",\"Principal\":"
                                      + " {\"AWS\": \"arn:aws:iam::045140586512:root\"}, \"Action\": [ \"*:*\" ], \"Resource\": "
                                      + "[\"%s\" ] } ]}", accessPointArn);

        assertNotNull(s3ControlClient.getAccessPoint(b -> b.name(accessPointArn)));
        assertNotNull(s3ControlClient.putAccessPointPolicy(b -> b.name(accessPointArn).policy(policy)));
        assertNotNull(s3ControlClient.getAccessPointPolicy(b -> b.name(accessPointArn)));
        assertNotNull(s3ControlClient.deleteAccessPointPolicy(b -> b.name(accessPointArn)));
    }
}
