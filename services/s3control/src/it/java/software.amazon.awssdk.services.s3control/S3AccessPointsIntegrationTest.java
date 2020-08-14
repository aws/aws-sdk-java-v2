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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.util.StringJoiner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sts.StsClient;

public class S3AccessPointsIntegrationTest extends S3ControlIntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(S3AccessPointsIntegrationTest.class);

    private static final String AP_NAME = "java-sdk-" + System.currentTimeMillis();

    private static final String KEY = "some-key";

    private static S3ControlClient s3control;

    private static StsClient sts;

    private static String accountId;

    @BeforeClass
    public static void setupFixture() {
        createBucket(BUCKET);

        s3control = S3ControlClient.builder()
                                   .region(Region.US_WEST_2)
                                   .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                   .build();

        sts = StsClient.builder()
                       .region(Region.US_WEST_2)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .build();

        accountId = sts.getCallerIdentity().account();
        s3control.createAccessPoint(r -> r.accountId(accountId)
                                          .bucket(BUCKET)
                                          .name(AP_NAME));
    }

    @AfterClass
    public static void tearDown() {
        deleteBucketAndAllContents(BUCKET);
        s3control.deleteAccessPoint(b -> b.accountId(accountId).name(AP_NAME));
    }

    @Test
    public void transfer_Succeeds_UsingAccessPoint() {
        StringJoiner apArn = new StringJoiner(":");
        apArn.add("arn").add("aws").add("s3").add("us-west-2").add(accountId).add("accesspoint").add(AP_NAME);

        s3.putObject(PutObjectRequest.builder()
                                     .bucket(apArn.toString())
                                     .key(KEY)
                                     .build(), RequestBody.fromString("helloworld"));

        String objectContent = s3.getObjectAsBytes(GetObjectRequest.builder()
                                                                   .bucket(apArn.toString())
                                                                   .key(KEY)
                                                                   .build()).asUtf8String();

        assertThat(objectContent).isEqualTo("helloworld");
    }

    @Test
    public void transfer_Succeeds_UsingAccessPoint_CrossRegion() {
        S3Client s3DifferentRegion =
            s3ClientBuilder().region(Region.US_EAST_1).serviceConfiguration(c -> c.useArnRegionEnabled(true)).build();

        StringJoiner apArn = new StringJoiner(":");
        apArn.add("arn").add("aws").add("s3").add("us-west-2").add(accountId).add("accesspoint").add(AP_NAME);

        s3DifferentRegion.putObject(PutObjectRequest.builder()
                                                    .bucket(apArn.toString())
                                                    .key(KEY)
                                                    .build(), RequestBody.fromString("helloworld"));

        String objectContent = s3DifferentRegion.getObjectAsBytes(GetObjectRequest.builder()
                                                                                  .bucket(apArn.toString())
                                                                                  .key(KEY)
                                                                                  .build()).asUtf8String();

        assertThat(objectContent).isEqualTo("helloworld");
    }

    @Test
    public void accessPointOperation_nonArns() {
        assertNotNull(s3control.listAccessPoints(b -> b.bucket(BUCKET).accountId(accountId).maxResults(1)));
        assertNotNull(s3control.getAccessPoint(b -> b.name(AP_NAME).accountId(accountId)));
    }
}
