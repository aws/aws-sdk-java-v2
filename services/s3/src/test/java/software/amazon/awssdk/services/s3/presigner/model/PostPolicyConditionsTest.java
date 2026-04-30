/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */

package software.amazon.awssdk.services.s3.presigner.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PostPolicyConditionsTest {

    @Test
    public void builderMethods_expectedConditions() {
        PostPolicyConditions conditions = PostPolicyConditions.builder()
                                                              .contentLengthRange(1, 2)
                                                              .startsWith("Content-Type", "image/")
                                                              .eq("acl", "public-read")
                                                              .build();

        assertThat(conditions.conditions()).hasSize(3);
        assertThat(conditions.conditions().get(0)).isInstanceOf(PostPolicyConditions.ContentLengthRange.class);
        assertThat(conditions.conditions().get(1)).isInstanceOf(PostPolicyConditions.StartsWith.class);
        assertThat(conditions.conditions().get(2)).isInstanceOf(PostPolicyConditions.Eq.class);

        PostPolicyConditions.StartsWith startsWith = (PostPolicyConditions.StartsWith) conditions.conditions().get(1);
        assertThat(startsWith.field()).isEqualTo("$Content-Type");
        assertThat(startsWith.prefix()).isEqualTo("image/");
    }

    @Test
    public void startsWith_normalizesKeyVariations() {
        PostPolicyConditions a = PostPolicyConditions.builder().startsWith("key", "p").build();
        PostPolicyConditions b = PostPolicyConditions.builder().startsWith("$key", "p").build();
        assertThat(a.conditions().get(0)).isEqualTo(b.conditions().get(0));
    }

    @Test
    public void eq_allowsKeyFieldName() {
        PostPolicyConditions conditions = PostPolicyConditions.builder().eq("key", "value").build();
        PostPolicyConditions.Eq eq = (PostPolicyConditions.Eq) conditions.conditions().get(0);
        assertThat(eq.field()).isEqualTo("key");
        assertThat(eq.value()).isEqualTo("value");
    }
}
