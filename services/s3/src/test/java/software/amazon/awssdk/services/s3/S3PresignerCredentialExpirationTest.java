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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CreateSessionRequest;
import software.amazon.awssdk.services.s3.model.CreateSessionResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.SessionCredentials;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Verifies that the S3 presigner caps the signed and reported expiration at the signing credential's expiration.
 *
 * <p>The S3 presigner signs at {@link Instant#now()}, so each case sets the credential expiration relative to the test
 * start time, and the assertions bound the result by the instants captured before and after the presign call.
 */
class S3PresignerCredentialExpirationTest {

    private static final Duration REQUESTED_DURATION = Duration.ofHours(2);

    @ParameterizedTest(name = "{0}")
    @MethodSource("credentialExpirations")
    void presignGetObject_sessionCredentials_capsExpirationAndSignedWindow(String scenario,
                                                                           UnaryOperator<Instant> credentialExpirationFromNow,
                                                                           boolean capped) {
        assertCappedExpiration(S3PresignerCredentialExpirationTest::sessionCredentialsPresigner,
                               GetObjectRequest.builder().bucket("a").key("b").build(),
                               credentialExpirationFromNow,
                               capped);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("credentialExpirations")
    void presignGetObject_requestLevelSigner_sessionCredentials_capsExpirationAndSignedWindow(
        String scenario,
        UnaryOperator<Instant> credentialExpirationFromNow,
        boolean capped) {
        GetObjectRequest legacySignerRequest = GetObjectRequest.builder()
                                                               .bucket("a")
                                                               .key("b")
                                                               .overrideConfiguration(c -> c.signer(AwsS3V4Signer.create()))
                                                               .build();
        assertCappedExpiration(S3PresignerCredentialExpirationTest::sessionCredentialsPresigner,
                               legacySignerRequest,
                               credentialExpirationFromNow,
                               capped);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("credentialExpirations")
    void presignGetObject_s3ExpressSessionCredentials_capsExpirationAndSignedWindow(
        String scenario,
        UnaryOperator<Instant> credentialExpirationFromNow,
        boolean capped) {
        assertCappedExpiration(S3PresignerCredentialExpirationTest::s3ExpressSessionPresigner,
                               GetObjectRequest.builder().bucket("bucket--use1-az1--x-s3").key("b").build(),
                               credentialExpirationFromNow,
                               capped);
    }

    private static Stream<Arguments> credentialExpirations() {
        return Stream.of(
            Arguments.of("expires before requested end, capped",
                         (UnaryOperator<Instant>) now -> now.plus(Duration.ofHours(1)), true),
            Arguments.of("fractional remaining, capped to whole seconds",
                         (UnaryOperator<Instant>) now -> now.plus(Duration.ofMinutes(90)).plusMillis(700), true),
            Arguments.of("expires after requested end, not capped",
                         (UnaryOperator<Instant>) now -> now.plus(Duration.ofHours(5)), false),
            Arguments.of("no expiration, not capped", (UnaryOperator<Instant>) now -> null, false),
            Arguments.of("Instant.MAX expiration, not capped", (UnaryOperator<Instant>) now -> Instant.MAX, false),
            Arguments.of("under one second remaining, not capped", (UnaryOperator<Instant>) now -> now.plusMillis(500), false),
            Arguments.of("expires at test start, not capped", (UnaryOperator<Instant>) now -> now, false),
            Arguments.of("already expired, not capped",
                         (UnaryOperator<Instant>) now -> now.minus(Duration.ofMinutes(30)), false));
    }

    private static S3Presigner sessionCredentialsPresigner(Instant credentialExpiration) {
        AwsSessionCredentials sessionCredentials = AwsSessionCredentials.builder()
                                                                        .accessKeyId("akid")
                                                                        .secretAccessKey("skid")
                                                                        .sessionToken("token")
                                                                        .expirationTime(credentialExpiration)
                                                                        .build();
        return S3Presigner.builder()
                          .region(Region.US_WEST_2)
                          .credentialsProvider(StaticCredentialsProvider.create(sessionCredentials))
                          .build();
    }

    private static S3Presigner s3ExpressSessionPresigner(Instant sessionExpiration) {
        S3Client s3Client = mock(S3Client.class);
        SessionCredentials sessionCredentials = SessionCredentials.builder()
                                                                  .accessKeyId("akid")
                                                                  .secretAccessKey("skid")
                                                                  .sessionToken("token")
                                                                  .expiration(sessionExpiration)
                                                                  .build();
        when(s3Client.createSession(any(CreateSessionRequest.class)))
            .thenReturn(CreateSessionResponse.builder().credentials(sessionCredentials).build());
        return S3Presigner.builder()
                          .region(Region.US_EAST_1)
                          .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                          .s3Client(s3Client)
                          .build();
    }

    private static void assertCappedExpiration(Function<Instant, S3Presigner> presignerForCredentialExpiration,
                                               GetObjectRequest getObjectRequest,
                                               UnaryOperator<Instant> credentialExpirationFromNow,
                                               boolean capped) {
        Instant before = Instant.now();
        Instant credentialExpiration = credentialExpirationFromNow.apply(before);

        PresignedGetObjectRequest presigned;
        try (S3Presigner presigner = presignerForCredentialExpiration.apply(credentialExpiration)) {
            presigned = presigner.presignGetObject(r -> r.signatureDuration(REQUESTED_DURATION)
                                                         .getObjectRequest(getObjectRequest));
        }
        Instant after = Instant.now();

        long signedSeconds = Long.parseLong(presigned.httpRequest().rawQueryParameters().get("X-Amz-Expires").get(0));
        if (capped) {
            assertThat(signedSeconds).isBetween(Duration.between(after, credentialExpiration).getSeconds(),
                                                Duration.between(before, credentialExpiration).getSeconds());
            assertThat(presigned.expiration()).isAfter(credentialExpiration.minusSeconds(1))
                                              .isBeforeOrEqualTo(credentialExpiration);
        } else {
            assertThat(signedSeconds).isEqualTo(REQUESTED_DURATION.getSeconds());
            assertThat(presigned.expiration()).isBetween(before.plus(REQUESTED_DURATION), after.plus(REQUESTED_DURATION));
        }
    }
}
