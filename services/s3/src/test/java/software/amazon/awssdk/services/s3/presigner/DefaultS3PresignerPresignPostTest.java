/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.awssdk.services.s3.presigner;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.signing.PostPolicyDocument;
import software.amazon.awssdk.services.s3.internal.signing.S3PostPolicySigner;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions;
import software.amazon.awssdk.services.s3.presigner.model.PresignPostObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPostObjectRequest;

public class DefaultS3PresignerPresignPostTest {

    @Test
    public void postPolicy_formFieldOrdering_isDeterministic() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            "AKIAIOSFODNN7EXAMPLE",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.US_EAST_1)
                                           .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                           .clock(Clock.fixed(Instant.parse("2015-12-29T00:00:00Z"), ZoneOffset.UTC))
                                           .build();

        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("examplebucket")
                                                                   .key("obj/${filename}")
                                                                   .signatureDuration(Duration.ofHours(1))
                                                                   .conditions(c -> c.eq("acl", "private").startsWith("$key", "obj/"))
                                                                   .addExactField("acl", "private")
                                                                   .build();

        PresignedPostObjectRequest presigned = presigner.presignPost(request);

        Iterator<Map.Entry<String, String>> iterator = presigned.signedFormFields().entrySet().iterator();
        assertThat(iterator.next().getKey()).isEqualTo("key");
        assertThat(iterator.next().getKey()).isEqualTo("acl");
        assertThat(presigned.signedFormFields().get("policy")).isNotBlank();
        assertThat(presigned.signedFormFields().get("x-amz-signature")).hasSize(64);
    }

    @Test
    public void postPolicy_url_resolvesVirtualHostedStyle() throws Exception {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            "AKIAIOSFODNN7EXAMPLE",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.US_EAST_1)
                                           .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                           .clock(Clock.fixed(Instant.parse("2015-12-29T00:00:00Z"), ZoneOffset.UTC))
                                           .build();

        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("my-bucket")
                                                                   .key("k")
                                                                   .signatureDuration(Duration.ofHours(1))
                                                                   .addExactField("acl", "private")
                                                                   .build();

        URL url = presigner.presignPost(request).url();
        assertThat(url.getProtocol()).isEqualTo("https");
        assertThat(url.getHost()).contains("my-bucket");
        assertThat(url.getPath()).isEqualTo("/");
    }

    @Test
    public void sessionCredentials_includesSessionTokenInPayloadAndFields() {
        AwsSessionCredentials credentials = AwsSessionCredentials.create(
            "AKIAIOSFODNN7EXAMPLE",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
            "SESSIONTOKEN");

        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.US_EAST_1)
                                           .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                           .clock(Clock.fixed(Instant.parse("2015-12-29T00:00:00Z"), ZoneOffset.UTC))
                                           .build();

        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("k")
                                                                   .signatureDuration(Duration.ofHours(1))
                                                                   .addExactField("acl", "private")
                                                                   .build();

        PresignedPostObjectRequest presigned = presigner.presignPost(request);

        assertThat(presigned.signedFormFields()).containsKey("x-amz-security-token");
        assertThat(presigned.signedFormFields().get("x-amz-security-token")).isEqualTo("SESSIONTOKEN");
    }

    @Test
    public void decoupledContentType_conditionAndField_works() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            "AKIAIOSFODNN7EXAMPLE",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.US_EAST_1)
                                           .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                           .clock(Clock.fixed(Instant.parse("2015-12-29T00:00:00Z"), ZoneOffset.UTC))
                                           .build();

        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("k")
                                                                   .signatureDuration(Duration.ofHours(1))
                                                                   .conditions(c -> c.startsWith("$Content-Type", "image/"))
                                                                   .addField("Content-Type", "image/jpeg")
                                                                   .addExactField("acl", "private")
                                                                   .build();

        PresignedPostObjectRequest presigned = presigner.presignPost(request);
        assertThat(presigned.signedFormFields().get("Content-Type")).isEqualTo("image/jpeg");

        String policyJson = new String(Base64.getDecoder().decode(presigned.signedFormFields().get("policy")),
                                       StandardCharsets.UTF_8);
        assertThat(policyJson).contains("[\"starts-with\",\"$Content-Type\",\"image/\"]");
        assertThat(policyJson).doesNotContain("\"Content-Type\":\"image/jpeg\"");
        assertThat(policyJson).doesNotContain("{\"Content-Type\"");
    }

    /**
     * End-to-end golden check: presigning via {@link DefaultS3Presigner} must produce the same base64 policy and SigV4
     * signature that a direct call to {@link PostPolicyDocument} + {@link S3PostPolicySigner} produces for equivalent inputs.
     * This pins the wiring of the presigner's orchestration to the building blocks that are independently covered by
     * per-class golden tests.
     */
    @Test
    public void postPolicy_endToEndGolden_matchesSignerBuildingBlocks() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            "AKIAIOSFODNN7EXAMPLE",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
        Instant signingInstant = Instant.parse("2015-12-29T00:00:00Z");
        Duration duration = Duration.ofHours(1);
        Instant expiration = signingInstant.plus(duration);

        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.US_EAST_1)
                                           .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                           .clock(Clock.fixed(signingInstant, ZoneOffset.UTC))
                                           .build();

        PostPolicyConditions userConditions = PostPolicyConditions.builder()
                                                                  .eq("acl", "public-read")
                                                                  .contentLengthRange(0, 1024)
                                                                  .build();

        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("examplebucket")
                                                                   .key("obj.txt")
                                                                   .signatureDuration(duration)
                                                                   .conditions(userConditions)
                                                                   .addExactField("acl", "public-read")
                                                                   .build();

        PresignedPostObjectRequest presigned = presigner.presignPost(request);

        PostPolicyDocument expectedPolicy = PostPolicyDocument.from("examplebucket",
                                                                    "obj.txt",
                                                                    expiration,
                                                                    userConditions,
                                                                    Collections.emptyMap(),
                                                                    "AKIAIOSFODNN7EXAMPLE/20151229/us-east-1/s3/aws4_request",
                                                                    "20151229T000000Z",
                                                                    null);
        String expectedBase64Policy = Base64.getEncoder()
                                            .encodeToString(expectedPolicy.toJson().getBytes(StandardCharsets.UTF_8));

        S3PostPolicySigner.SignedPostPolicy expectedSigned = S3PostPolicySigner.sign(
            S3PostPolicySigner.SignInput.builder()
                                        .credentials(credentials)
                                        .region("us-east-1")
                                        .signingInstant(signingInstant)
                                        .policyExpiration(expiration)
                                        .bucket("examplebucket")
                                        .objectKey("obj.txt")
                                        .userConditions(userConditions)
                                        .userFields(Collections.emptyMap())
                                        .build());

        assertThat(presigned.signedFormFields()).containsEntry("policy", expectedBase64Policy);
        assertThat(expectedSigned.base64Policy()).isEqualTo(expectedBase64Policy);
        assertThat(presigned.signedFormFields()).containsEntry("x-amz-signature", expectedSigned.hexSignature());
        assertThat(presigned.signedFormFields()).containsEntry("x-amz-algorithm", "AWS4-HMAC-SHA256");
        assertThat(presigned.signedFormFields()).containsEntry("x-amz-date", "20151229T000000Z");
        assertThat(presigned.signedFormFields()).containsEntry("x-amz-credential",
            "AKIAIOSFODNN7EXAMPLE/20151229/us-east-1/s3/aws4_request");
        assertThat(presigned.expiration()).isEqualTo(expiration);
    }
}
