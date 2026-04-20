/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.awssdk.services.s3.internal.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.model.PostPolicyConditions;

public class PostPolicyDocumentTest {

    @Test
    public void minimalPolicy_matchesExpectedJson() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        PostPolicyDocument doc = PostPolicyDocument.from(
            "my-bucket",
            "my-key",
            expiration,
            PostPolicyConditions.empty(),
            Collections.emptyMap(),
            "AKIA/20151229/us-east-1/s3/aws4_request",
            "20151229T000000Z",
            null);

        assertThat(doc.toJson()).isEqualTo(
            "{\"expiration\":\"2015-12-30T12:00:00.000Z\",\"conditions\":["
            + "{\"bucket\":\"my-bucket\"},"
            + "{\"key\":\"my-key\"},"
            + "{\"x-amz-credential\":\"AKIA/20151229/us-east-1/s3/aws4_request\"},"
            + "{\"x-amz-algorithm\":\"AWS4-HMAC-SHA256\"},"
            + "{\"x-amz-date\":\"20151229T000000Z\"}"
            + "]}");
    }

    @Test
    public void awsDocs_threeCondition_example_matchesOrder() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        PostPolicyConditions conditions = PostPolicyConditions.builder()
                                                                .eq("acl", "public-read")
                                                                .startsWith("$key", "user/eric/")
                                                                .build();

        PostPolicyDocument doc = PostPolicyDocument.from(
            "johnsmith",
            "ignored-key",
            expiration,
            conditions,
            Collections.emptyMap(),
            "AKIA/20151229/us-east-1/s3/aws4_request",
            "20151229T000000Z",
            null);

        assertThat(doc.toJson()).isEqualTo(
            "{\"expiration\":\"2015-12-30T12:00:00.000Z\",\"conditions\":["
            + "{\"bucket\":\"johnsmith\"},"
            + "{\"acl\":\"public-read\"},"
            + "[\"starts-with\",\"$key\",\"user/eric/\"],"
            + "{\"x-amz-credential\":\"AKIA/20151229/us-east-1/s3/aws4_request\"},"
            + "{\"x-amz-algorithm\":\"AWS4-HMAC-SHA256\"},"
            + "{\"x-amz-date\":\"20151229T000000Z\"}"
            + "]}");
    }

    @Test
    public void characterEscaping_matchesSpec() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        PostPolicyConditions conditions = PostPolicyConditions.builder()
                                                                .eq("k\"ey", "v\\al\"ue\n\t\f\b\r\u0001")
                                                                .build();

        PostPolicyDocument doc = PostPolicyDocument.from(
            "b",
            "k",
            expiration,
            conditions,
            Collections.emptyMap(),
            "cred",
            "date",
            null);

        assertThat(doc.toJson()).contains("\\\"");
        assertThat(doc.toJson()).contains("\\\\");
        assertThat(doc.toJson()).contains("\\n");
        assertThat(doc.toJson()).contains("\\t");
        assertThat(doc.toJson()).contains("\\f");
        assertThat(doc.toJson()).contains("\\b");
        assertThat(doc.toJson()).contains("\\r");
        assertThat(doc.toJson()).contains("\\u0001");
    }

    @Test
    public void reservedFieldNames_rejected() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        Map<String, String> bad = new HashMap<>();
        bad.put("policy", "nope");

        assertThatThrownBy(() -> PostPolicyDocument.from(
            "b", "k", expiration, PostPolicyConditions.empty(), bad, "c", "d", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("policy");
    }

    @Test
    public void filenameVariable_inObjectKey_emitsStartsWithKeyCondition() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        PostPolicyDocument doc = PostPolicyDocument.from(
            "bucket",
            "prefix/${filename}",
            expiration,
            PostPolicyConditions.empty(),
            Collections.emptyMap(),
            "cred",
            "date",
            null);

        assertThat(doc.toJson()).contains("[\"starts-with\",\"$key\",\"prefix\"]");
        assertThat(doc.toJson()).doesNotContain("\"key\":\"prefix");
    }

    @Test
    public void userSuppliedKeyCondition_suppressesAutoKeyCondition() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        PostPolicyConditions conditions = PostPolicyConditions.builder()
                                                                .startsWith("key", "manual/")
                                                                .build();

        PostPolicyDocument doc = PostPolicyDocument.from(
            "b",
            "should-not-appear",
            expiration,
            conditions,
            Collections.emptyMap(),
            "cred",
            "date",
            null);

        assertThat(doc.toJson()).contains("[\"starts-with\",\"$key\",\"manual/\"]");
        assertThat(doc.toJson()).doesNotContain("\"key\":\"should-not-appear\"");
    }

    @Test
    public void userSuppliedEqKeyCondition_suppressesAutoKeyCondition() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        PostPolicyConditions conditions = PostPolicyConditions.builder()
                                                                .eq("key", "exact-key")
                                                                .build();

        PostPolicyDocument doc = PostPolicyDocument.from(
            "b",
            "ignored",
            expiration,
            conditions,
            Collections.emptyMap(),
            "cred",
            "date",
            null);

        assertThat(doc.toJson()).contains("{\"key\":\"exact-key\"}");
        assertThat(doc.toJson()).doesNotContain("\"key\":\"ignored\"");
    }

    @Test
    public void sessionCredentials_addSecurityTokenCondition() {
        Instant expiration = Instant.parse("2015-12-30T12:00:00Z");
        PostPolicyDocument doc = PostPolicyDocument.from(
            "b",
            "k",
            expiration,
            PostPolicyConditions.empty(),
            Collections.emptyMap(),
            "cred",
            "date",
            "token");

        assertThat(doc.toJson()).contains("{\"x-amz-security-token\":\"token\"}");
    }
}
