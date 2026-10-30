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

package software.amazon.awssdk.services.s3.internal.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateSessionRequest;
import software.amazon.awssdk.services.s3.model.CreateSessionResponse;
import software.amazon.awssdk.services.s3.model.SessionCredentials;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Verifies that an {@link S3Presigner} shares one S3 Express session cache across presign calls and that closing the
 * presigner stops the background refresh of the cached session credentials.
 */
class DefaultS3PresignerS3ExpressCacheTest {
    private static final String PRESIGN_EXPRESS_SESSION_HEADER = "X-Amz-S3session-Token";
    private static final String BUCKET = "b--use1-az1--x-s3";
    private static final String OTHER_BUCKET = "other--use1-az1--x-s3";

    /**
     * The credentials expire 62 seconds after they are fetched. The prefetch time is 60 seconds before expiration,
     * so a background refresh runs about 3 seconds after each fetch.
     */
    private static final Duration CREDENTIAL_LIFETIME = Duration.ofSeconds(62);

    /**
     * Long enough to cover at least one background refresh.
     */
    private static final Duration OBSERVATION_WINDOW = Duration.ofSeconds(6);

    private S3Client s3Client;
    private S3Presigner presigner;

    @BeforeEach
    void setup() {
        s3Client = mock(S3Client.class);
        when(s3Client.createSession(any(CreateSessionRequest.class)))
            .thenAnswer(invocation -> createSessionResponse());

        presigner = S3Presigner.builder()
                               .region(Region.US_EAST_1)
                               .credentialsProvider(StaticCredentialsProvider.create(
                                   AwsBasicCredentials.create("akid", "skid")))
                               .s3Client(s3Client)
                               .disableS3ExpressSessionAuth(false)
                               .build();
    }

    @AfterEach
    void teardown() {
        presigner.close();
    }

    @Test
    void repeatedPresignSameBucket_callsCreateSessionOnce() {
        for (int i = 0; i < 4; i++) {
            PresignedGetObjectRequest presigned = presign(BUCKET);
            assertThat(presigned.url().toString()).contains(PRESIGN_EXPRESS_SESSION_HEADER);
        }

        verify(s3Client, times(1)).createSession(any(CreateSessionRequest.class));
    }

    @Test
    void distinctBuckets_eachCreateSessionOnce() {
        presign(BUCKET);
        presign(BUCKET);
        presign(OTHER_BUCKET);
        presign(OTHER_BUCKET);

        verify(s3Client, times(2)).createSession(any(CreateSessionRequest.class));
    }

    @Test
    void withoutClose_backgroundRefreshCallsCreateSession() throws InterruptedException {
        presign(BUCKET);

        Thread.sleep(OBSERVATION_WINDOW.toMillis());

        verify(s3Client, atLeast(2)).createSession(any(CreateSessionRequest.class));
    }

    @Test
    void close_stopsBackgroundRefresh() throws InterruptedException {
        presign(BUCKET);
        presigner.close();

        Thread.sleep(OBSERVATION_WINDOW.toMillis());

        verify(s3Client, times(1)).createSession(any(CreateSessionRequest.class));
    }

    private PresignedGetObjectRequest presign(String bucket) {
        return presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                .getObjectRequest(go -> go.bucket(bucket).key("key")));
    }

    private static CreateSessionResponse createSessionResponse() {
        return CreateSessionResponse.builder()
                                    .credentials(SessionCredentials.builder()
                                                                   .accessKeyId("AccessKeyId")
                                                                   .secretAccessKey("SecretAccessKey")
                                                                   .sessionToken("s3expressSessionToken")
                                                                   .expiration(Instant.now().plus(CREDENTIAL_LIFETIME))
                                                                   .build())
                                    .build();
    }
}
