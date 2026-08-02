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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reserved");
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

    /**
     * Regression test: {@code addExactField} used to append a new {@code eq} condition on every call, so repeated calls for
     * the same field name produced conflicting policy conditions that S3 would later reject.
     */
    @Test
    public void addExactField_calledTwiceForSameName_keepsSingleEqCondition() {
        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("k")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .addExactField("acl", "public-read")
                                                                   .addExactField("acl", "private")
                                                                   .build();

        List<PostPolicyConditions.Eq> aclConditions =
            request.conditions().conditions().stream()
                   .filter(c -> c instanceof PostPolicyConditions.Eq)
                   .map(c -> (PostPolicyConditions.Eq) c)
                   .filter(eq -> "acl".equals(eq.field()))
                   .collect(Collectors.toList());

        assertThat(aclConditions).hasSize(1);
        assertThat(aclConditions.get(0).value()).isEqualTo("private");
        assertThat(request.fields().get("acl")).isEqualTo("private");
    }

    /**
     * Regression test: the S3 POST form requires the object key form field to be literally {@code "key"}. User-supplied case
     * variants used to slip through as standalone form fields without a matching condition, yielding a build-time error or a
     * policy/field mismatch that S3 would reject.
     */
    @Test
    public void addField_withCaseVariantKey_isNormalizedToLowercase() {
        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("user/eric/${filename}")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .conditions(c -> c.startsWith("$key", "user/eric/"))
                                                                   .addField("Key", "user/eric/photo.png")
                                                                   .build();

        assertThat(request.fields()).containsOnlyKeys("key");
        assertThat(request.fields().get("key")).isEqualTo("user/eric/photo.png");
    }

    @Test
    public void addExactField_withCaseVariantKey_isNormalizedToLowercase() {
        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .key("user/eric/obj.txt")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .addExactField("KEY", "user/eric/obj.txt")
                                                                   .build();

        assertThat(request.fields()).containsOnlyKeys("key");
        assertThat(request.conditions().conditions()).anySatisfy(c -> {
            assertThat(c).isInstanceOf(PostPolicyConditions.Eq.class);
            assertThat(((PostPolicyConditions.Eq) c).field()).isEqualTo("key");
        });
    }

    /**
     * Regression test: {@code addExactFields} used to perform field/condition validation using the current builder state,
     * which threw {@link NullPointerException} when called before {@code key(...)} because the validation helper indexed a
     * null object key.
     */
    @Test
    public void addExactFields_beforeKeySet_doesNotThrow() {
        Map<String, String> userFields = new LinkedHashMap<>();
        userFields.put("acl", "public-read");
        userFields.put("x-amz-meta-author", "eric");

        PresignPostObjectRequest request = PresignPostObjectRequest.builder()
                                                                   .bucket("b")
                                                                   .signatureDuration(Duration.ofMinutes(1))
                                                                   .addExactFields(userFields)
                                                                   .key("k")
                                                                   .build();

        assertThat(request.fields()).containsEntry("acl", "public-read")
                                    .containsEntry("x-amz-meta-author", "eric");
    }

    @Test
    public void toBuilder_roundtrip_producesEqualRequest() {
        PresignPostObjectRequest original = PresignPostObjectRequest.builder()
                                                                    .bucket("b")
                                                                    .key("k")
                                                                    .signatureDuration(Duration.ofMinutes(5))
                                                                    .conditions(c -> c.eq("acl", "public-read")
                                                                                      .contentLengthRange(0, 1024))
                                                                    .addExactField("acl", "public-read")
                                                                    .build();

        PresignPostObjectRequest roundtrip = original.toBuilder().build();

        assertThat(roundtrip).isEqualTo(original);
        assertThat(roundtrip.hashCode()).isEqualTo(original.hashCode());
        assertThat(roundtrip.fields()).isEqualTo(original.fields());
        assertThat(roundtrip.conditions()).isEqualTo(original.conditions());
    }
}
