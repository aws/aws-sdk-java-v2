/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.awssdk.services.s3.presigner.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class PresignPostObjectRequestTest {

    @Test
    public void missingBucket_throws() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder()
                                                       .key("k")
                                                       .signatureDuration(Duration.ofMinutes(1))
                                                       .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("bucket");
    }

    @Test
    public void missingKey_throws() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder()
                                                       .bucket("b")
                                                       .signatureDuration(Duration.ofMinutes(1))
                                                       .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("key");
    }

    @Test
    public void bothSignatureDurationAndExpiration_throws() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder()
                                                       .bucket("b")
                                                       .key("k")
                                                       .signatureDuration(Duration.ofMinutes(1))
                                                       .expiration(Instant.now())
                                                       .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only one");
    }

    @Test
    public void neitherSignatureDurationNorExpiration_throws() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder().bucket("b").key("k").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("One of");
    }

    @Test
    public void reservedFieldName_rejected() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder()
                                                         .bucket("b")
                                                         .key("k")
                                                         .signatureDuration(Duration.ofMinutes(1))
                                                         .addField("bucket", "nope")
                                                         .build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("bucket");
    }

    @Test
    public void reservedFieldName_rejected_caseInsensitive() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder()
                                                         .bucket("b")
                                                         .key("k")
                                                         .signatureDuration(Duration.ofMinutes(1))
                                                         .addField("X-Amz-Date", "nope")
                                                         .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void keyField_isNotReserved() {
        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("user/eric/${filename}")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .conditions(c -> c.startsWith("$key", "user/eric/"))
                                                                   .addField("key", "user/eric/${filename}")
                                                                   .build();

        assertThat(request.fields().get("key")).isEqualTo("user/eric/${filename}");
    }

    @Test
    public void addField_withoutCondition_throws() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder()
                                                         .bucket("b")
                                                         .key("k")
                                                         .signatureDuration(Duration.ofMinutes(1))
                                                         .addField("X", "v")
                                                         .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("matching policy condition");
    }

    @Test
    public void addField_withEqCondition_accepted() {
        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("k")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .conditions(c -> c.eq("acl", "public-read"))
                                                                   .addField("acl", "public-read")
                                                                   .build();

        assertThat(request.fields().get("acl")).isEqualTo("public-read");
    }

    @Test
    public void addField_withStartsWithCondition_acceptedWhenValueMatches() {
        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("k")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .conditions(c -> c.startsWith("$Content-Type", "image/"))
                                                                   .addField("Content-Type", "image/jpeg")
                                                                   .build();

        assertThat(request.fields().get("Content-Type")).isEqualTo("image/jpeg");
    }

    @Test
    public void addField_withStartsWithCondition_throwsWhenValueViolatesPrefix() {
        assertThatThrownBy(() -> PresignPostObjectRequest.builder()
                                                         .bucket("b")
                                                         .key("k")
                                                         .signatureDuration(Duration.ofMinutes(1))
                                                         .conditions(c -> c.startsWith("$Content-Type", "image/"))
                                                         .addField("Content-Type", "text/plain")
                                                         .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("matching policy condition");
    }

    @Test
    public void addExactField_addsFieldAndCondition() {
        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("k")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .addExactField("acl", "public-read")
                                                                   .build();

        assertThat(request.fields().get("acl")).isEqualTo("public-read");
        assertThat(request.conditions().conditions()).anySatisfy(c -> {
            assertThat(c).isInstanceOf(PostPolicyConditions.Eq.class);
            PostPolicyConditions.Eq eq = (PostPolicyConditions.Eq) c;
            assertThat(eq.field()).isEqualTo("acl");
            assertThat(eq.value()).isEqualTo("public-read");
        });
    }

    @Test
    public void addExactFields_atomicOnFailure() {
        Map<String, String> bad = new HashMap<>();
        bad.put("policy", "nope");

        PresignPostObjectRequest.Builder builder = PresignPostObjectRequest.builder()
                                                                           .bucket("b")
                                                                           .key("k")
                                                                           .signatureDuration(Duration.ofMinutes(1))
                                                                           .addExactField("acl", "public-read");

        assertThatThrownBy(() -> builder.addExactFields(bad))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("policy");

        PresignPostObjectRequest partial = builder.build();
        assertThat(partial.fields()).containsOnlyKeys("acl");
    }
}
