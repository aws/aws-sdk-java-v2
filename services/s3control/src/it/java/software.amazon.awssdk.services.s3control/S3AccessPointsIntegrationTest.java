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
import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.IOException;
import java.time.Duration;
import java.util.StringJoiner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;

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
    public void uploadAndDownloadWithPresignedUrlWorks() throws IOException {
        String accessPointArn = new StringJoiner(":").add("arn").add("aws").add("s3").add("us-west-2").add(accountId)
                                                     .add("accesspoint").add(AP_NAME).toString();
        String key = "foo/a0A!-_.*'()&@:,$=+?; \n\\^`<>{}[]#%\"~|山";

        testAccessPointPresigning(accessPointArn, key);
    }

    private void testAccessPointPresigning(String accessPointArn, String key) throws IOException {
        String data = "Hello";

        S3Presigner presigner = S3Presigner.builder().region(Region.US_WEST_2).build();

        SdkHttpRequest presignedPut = presigner.presignPutObject(r -> r.signatureDuration(Duration.ofDays(7))
                                                                       .putObjectRequest(por -> por.bucket(accessPointArn)
                                                                                                   .key(key)))
                                               .httpRequest();


        SdkHttpRequest presignedGet = presigner.presignGetObject(r -> r.signatureDuration(Duration.ofDays(7))
                                                                       .getObjectRequest(gor -> gor.bucket(accessPointArn)
                                                                                                   .key(key)))
                                               .httpRequest();

        try (SdkHttpClient client = ApacheHttpClient.create()) {
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(presignedPut)
                                                    .contentStreamProvider(() -> new StringInputStream(data))
                                                    .build())
                  .call();

            HttpExecuteResponse getResult = client.prepareRequest(HttpExecuteRequest.builder()
                                                                                    .request(presignedGet)
                                                                                    .build())
                                             .call();

            String result = getResult.responseBody()
                                     .map(stream -> invokeSafely(() -> IoUtils.toUtf8String(stream)))
                                     .orElseThrow(AssertionError::new);

            assertThat(result).isEqualTo(data);
        }
    }

    @Test
    public void accessPointOperation_nonArns() {
        assertNotNull(s3control.listAccessPoints(b -> b.bucket(BUCKET).accountId(accountId).maxResults(1)));
        assertNotNull(s3control.getAccessPoint(b -> b.name(AP_NAME).accountId(accountId)));
    }
}
