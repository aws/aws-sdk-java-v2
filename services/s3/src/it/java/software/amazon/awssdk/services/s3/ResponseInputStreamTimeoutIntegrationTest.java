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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.time.Duration;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class ResponseInputStreamTimeoutIntegrationTest extends S3IntegrationTestBase {

    private static final String BUCKET = temporaryBucketName(GetObjectIntegrationTest.class);
    private static final String KEY = "TestKey";
    private static final String CONTENT = "Hello";
    private static final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                                             .bucket(BUCKET)
                                                                             .key(KEY)
                                                                             .build();
    private S3Client s3Client;

    @Before
    public void init() {
        s3Client = s3ClientBuilder()
            .httpClientBuilder(ApacheHttpClient.builder().maxConnections(1))
            .overrideConfiguration(o -> o.retryStrategy(r -> r.maxAttempts(1)))
            .build();
    }

    @BeforeClass
    public static void setupFixture() throws IOException {
        createBucket(BUCKET);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET)
                                     .key(KEY)
                                     .build(), RequestBody.fromString(CONTENT));


    }

    @AfterClass
    public static void tearDownFixture() {
        deleteBucketAndAllContents(BUCKET);
    }

    @Test
    public void defaultTimeout_firstStreamNotConsumed_secondRequestTimesOut() {
        s3Client.getObject(getObjectRequest);

        assertThatThrownBy(() -> s3Client.getObject(getObjectRequest))
            .hasRootCauseInstanceOf(ConnectionPoolTimeoutException.class)
            .hasMessageContaining("Timeout waiting for connection from pool");
    }

    @Test
    public void defaultTimeout_firstStreamConsumed_secondRequestSucceeds() throws IOException {
        ResponseInputStream<GetObjectResponse> get1 = s3Client.getObject(getObjectRequest);
        byte[] buf = new byte[CONTENT.length()];
        get1.read(buf);

        ResponseInputStream<GetObjectResponse> get2 = s3Client.getObject(getObjectRequest);
        assertThat(get2.response().contentLength()).isEqualTo(CONTENT.length());
    }

    @Test
    public void defaultTimeout_firstStreamAborted_secondRequestSucceeds() throws IOException {
        ResponseInputStream<GetObjectResponse> get1 = s3Client.getObject(getObjectRequest);
        get1.abort();

        ResponseInputStream<GetObjectResponse> get2 = s3Client.getObject(getObjectRequest);
        assertThat(get2.response().contentLength()).isEqualTo(CONTENT.length());
    }

    @Test
    public void defaultTimeout_firstStreamClosed_secondRequestSucceeds() throws IOException {
        ResponseInputStream<GetObjectResponse> get1 = s3Client.getObject(getObjectRequest);
        get1.close();

        ResponseInputStream<GetObjectResponse> get2 = s3Client.getObject(getObjectRequest);
        assertThat(get2.response().contentLength()).isEqualTo(CONTENT.length());
    }

    @Test
    public void customTimeout_waitForTimeout_secondRequestSucceeds() throws InterruptedException {
       s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream(Duration.ofSeconds(2)));
       Thread.sleep(3000);

        ResponseInputStream<GetObjectResponse> get2 = s3Client.getObject(getObjectRequest);
        assertThat(get2.response().contentLength()).isEqualTo(CONTENT.length());
    }
}
