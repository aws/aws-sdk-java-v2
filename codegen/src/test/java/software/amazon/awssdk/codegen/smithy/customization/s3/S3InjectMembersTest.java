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

package software.amazon.awssdk.codegen.smithy.customization.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.NAMESPACE;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.S3_MODEL;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.s3Model;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.structure;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.HttpHeaderTrait;

class S3InjectMembersTest {
    @Test
    void preprocess_addsExpiresStringOnTheExpiresHeader() {
        Model model = new S3InjectMembers().preprocess(s3Model(), CustomizationConfig.create());

        for (String output : new String[] {"GetObjectOutput", "HeadObjectOutput"}) {
            MemberShape expires = structure(model, output).getMember("ExpiresString").get();
            assertThat(expires.getTarget()).isEqualTo(ShapeId.from(NAMESPACE + "Expiration"));
            assertThat(expires.expectTrait(HttpHeaderTrait.class).getValue()).isEqualTo("Expires");
            assertThat(expires.expectTrait(DocumentationTrait.class).getValue())
                .isEqualTo("<p>The date and time at which the object is no longer cacheable</p>");
        }
    }

    @Test
    void preprocess_addsSdkPartTypeAsAnUnboundMember() {
        Model model = new S3InjectMembers().preprocess(s3Model(), CustomizationConfig.create());

        MemberShape partType = structure(model, "UploadPartRequest").getMember("SdkPartType").get();
        assertThat(partType.getTarget()).isEqualTo(ShapeId.from(NAMESPACE + "SdkPartType"));
        assertThat(partType.hasTrait(HttpHeaderTrait.class)).isFalse();
    }

    @Test
    void preprocess_withoutTheSdkPartTypeShape_addsNoSdkPartType() {
        Model model = new S3InjectMembers().preprocess(s3Model(S3_MODEL), CustomizationConfig.create());

        assertThat(structure(model, "UploadPartRequest").getMember("SdkPartType")).isEmpty();
        assertThat(structure(model, "GetObjectOutput").getMember("ExpiresString")).isPresent();
    }
}
