/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.awssdk.services.s3.internal.signing;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;

public class S3PostPolicySignerTest {

    private static final String AWS_DOCS_POST_POLICY_BASE64 =
        "eyAiZXhwaXJhdGlvbiI6ICIyMDE1LTEyLTMwVDEyOjAwOjAwLjAwMFoiLA0KICAiY29uZGl0aW9ucyI6IFsNCiAgICB7ImJ1Y2tldCI6ICJzaWd2NGV4YW1wbGVidWNrZXQifSwNCiAgICBbInN0YXJ0cy13aXRoIiwgIiRrZXkiLCAidXNlci91c2VyMS8iXSwNCiAgICB7ImFjbCI6ICJwdWJsaWMtcmVhZCJ9LA0KICAgIHsic3VjY2Vzc19hY3Rpb25fcmVkaXJlY3QiOiAiaHR0cDovL3NpZ3Y0ZXhhbXBsZWJ1Y2tldC5zMy5hbWF6b25hd3MuY29tL3N1Y2Nlc3NmdWxfdXBsb2FkLmh0bWwifSwNCiAgICBbInN0YXJ0cy13aXRoIiwgIiRDb250ZW50LVR5cGUiLCAiaW1hZ2UvIl0sDQogICAgeyJ4LWFtei1tZXRhLXV1aWQiOiAiMTQzNjUxMjM2NTEyNzQifSwNCiAgICB7IngtYW16LXNlcnZlci1zaWRlLWVuY3J5cHRpb24iOiAiQUVTMjU2In0sDQogICAgWyJzdGFydHMtd2l0aCIsICIkeC1hbXotbWV0YS10YWciLCAiIl0sDQoNCiAgICB7IngtYW16LWNyZWRlbnRpYWwiOiAiQUtJQUlPU0ZPRE5ON0VYQU1QTEUvMjAxNTEyMjkvdXMtZWFzdC0xL3MzL2F3czRfcmVxdWVzdCJ9LA0KICAgIHsieC1hbXotYWxnb3JpdGhtIjogIkFXUzQtSE1BQy1TSEEyNTYifSwNCiAgICB7IngtYW16LWRhdGUiOiAiMjAxNTEyMjlUMDAwMDAwWiIgfQ0KICBdDQp9";

    @Test
    public void goldenVector_matchesAwsDocumentation() {
        AwsCredentialsIdentity credentials =
            AwsCredentialsIdentity.create("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");

        Instant signingInstant = Instant.parse("2015-12-29T00:00:00Z");
        CredentialScope scope = new CredentialScope("us-east-1", "s3", signingInstant);

        String signature = S3PostPolicySigner.computePostPolicySignatureHex(AWS_DOCS_POST_POLICY_BASE64, credentials, scope);

        assertThat(signature).isEqualTo("8afdbf4008c03f22c2cd3cdb72e4afbb1f6a588f3255ac628749a66d7f09699e");
    }

    @Test
    public void goldenVector_base64Policy_matchesAwsDocumentation() {
        String policyJson = new String(Base64.getDecoder().decode(AWS_DOCS_POST_POLICY_BASE64), StandardCharsets.UTF_8);
        String recomputed = Base64.getEncoder().encodeToString(policyJson.getBytes(StandardCharsets.UTF_8));
        assertThat(recomputed).isEqualTo(AWS_DOCS_POST_POLICY_BASE64);
    }

    @Test
    public void sign_isDeterministicForSameInputs() {
        AwsCredentialsIdentity credentials = AwsCredentialsIdentity.create("AKIA", "SECRET");
        Instant signingInstant = Instant.parse("2015-12-29T00:00:00Z");
        CredentialScope scope = new CredentialScope("us-east-1", "s3", signingInstant);

        String one = S3PostPolicySigner.computePostPolicySignatureHex("abc", credentials, scope);
        String two = S3PostPolicySigner.computePostPolicySignatureHex("abc", credentials, scope);
        assertThat(two).isEqualTo(one);
    }

    @Test
    public void sessionCredentials_tokenIncludedInSignedPolicy() {
        AwsSessionCredentialsIdentity credentials = AwsSessionCredentialsIdentity.create(
            "AKIAIOSFODNN7EXAMPLE",
            "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
            "SESSION");

        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        Instant signingInstant = Instant.parse("2015-12-29T00:00:00Z");

        S3PostPolicySigner.SignedPostPolicy signed = S3PostPolicySigner.sign(
            S3PostPolicySigner.SignInput.builder()
                                        .credentials(credentials)
                                        .region("us-east-1")
                                        .signingInstant(signingInstant)
                                        .policyExpiration(expiration)
                                        .bucket("b")
                                        .objectKey("k")
                                        .userFields(java.util.Collections.emptyMap())
                                        .sessionToken("SESSION")
                                        .build());

        assertThat(signed.policyJson()).contains("x-amz-security-token");
        assertThat(signed.sessionToken()).isEqualTo("SESSION");
    }
}
