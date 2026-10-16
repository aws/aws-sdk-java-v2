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

package software.amazon.awssdk.codegen.smithy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.EnumModel;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.smithy.model.Model;

/**
 * Covers the translation of the codegen traits that the smithy-build.json generator writes for Java-only
 * {@code customization.config} settings. The model below is what the transforms produce; the config carries none of
 * the settings, so everything asserted comes from the traits.
 */
class SmithyCodegenTraitsTranslationTest {

    private static final String PREFIX =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\nuse aws.auth#sigv4\nuse aws.protocols#restJson1\n"
        + "use software.amazon.awssdk.codegen#alternateBeanPropertyName\n"
        + "use software.amazon.awssdk.codegen#deprecatedName\n"
        + "use software.amazon.awssdk.codegen#enumConstantName\n"
        + "use software.amazon.awssdk.codegen#ignoreDataTypeConversionFailures\n"
        + "use software.amazon.awssdk.codegen#synthetic\n"
        + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n@sigv4(name: \"demo\")\n@restJson1\n"
        + "service DemoService { version: \"2024-01-01\", operations: [Copy] }\n"
        + "@http(method: \"POST\", uri: \"/copy\")\n"
        + "operation Copy { input: CopyRequest, output: CopyResponse }\n"
        + "structure CopyResponse {}\n";

    private static final String MODEL =
        PREFIX
        + "structure CopyRequest {\n"
        + "  @jsonName(\"Bucket\") @deprecatedName(\"Bucket\") DestinationBucket: String\n"
        + "  @jsonName(\"Key\") DestinationKey: String\n"
        + "  @alternateBeanPropertyName(\"Null\") NUL: Boolean\n"
        + "  SdkPartType: SdkPartType\n"
        + "  Filter: FilterRuleName\n"
        + "  Version: ClientVersion\n"
        + "  @ignoreDataTypeConversionFailures Expires: Timestamp\n"
        + "}\n"
        + "@synthetic enum SdkPartType {\n  DEFAULT\n  LAST\n}\n"
        + "enum FilterRuleName {\n"
        + "  @enumConstantName(\"PREFIX\") PREFIX = \"Prefix\"\n"
        + "  SUFFIX = \"suffix\"\n"
        + "}\n"
        + "enum ClientVersion {\n"
        + "  @enumConstantName(\"FIVE_ONE\") V5_1 = \"5.1\"\n"
        + "  V5_3 = \"5.3\"\n"
        + "}\n";

    @Test
    void deprecatedName_addsDeprecatedAccessorsUnderTheOldName() {
        MemberModel member = member(build(MODEL, CustomizationConfig.create()), "CopyRequest", "DestinationBucket");

        assertThat(member.getDeprecatedName()).isEqualTo("Bucket");
        assertThat(member.getDeprecatedFluentGetterMethodName()).isEqualTo("bucket");
        assertThat(member.getDeprecatedFluentSetterMethodName()).isEqualTo("bucket");
        assertThat(member.getDeprecatedBeanStyleSetterMethodName()).isEqualTo("setBucket");
        assertThat(member.getFluentGetterMethodName()).isEqualTo("destinationBucket");
    }

    @Test
    void renamedMemberWithoutDeprecatedName_hasNoDeprecatedName() {
        assertThat(member(build(MODEL, CustomizationConfig.create()), "CopyRequest", "DestinationKey")
                       .getDeprecatedName()).isNull();
    }

    @Test
    void alternateBeanPropertyName_addsTheBeanStyleSetter() {
        IntermediateModel model = build(MODEL, CustomizationConfig.create());

        assertThat(member(model, "CopyRequest", "NUL").getAdditionalBeanStyleSetterName()).isEqualTo("setNull");
        assertThat(member(model, "CopyRequest", "Expires").getAdditionalBeanStyleSetterName()).isNull();
    }

    @Test
    void synthetic_marksMembersTargetingTheShape() {
        IntermediateModel model = build(MODEL, CustomizationConfig.create());

        assertThat(member(model, "CopyRequest", "SdkPartType").isSynthetic()).isTrue();
        assertThat(member(model, "CopyRequest", "Filter").isSynthetic()).isFalse();
    }

    @Test
    void ignoreDataTypeConversionFailures_isSetOnTheMember() {
        IntermediateModel model = build(MODEL, CustomizationConfig.create());

        assertThat(member(model, "CopyRequest", "Expires").ignoreDataTypeConversionFailures()).isTrue();
        assertThat(member(model, "CopyRequest", "NUL").ignoreDataTypeConversionFailures()).isFalse();
    }

    @Test
    void enumConstantName_overridesTheDerivedName() {
        IntermediateModel model = build(MODEL, CustomizationConfig.create());

        assertThat(Utils.findShapeModelByC2jName(model, "ClientVersion").getEnums())
            .extracting(EnumModel::getName, EnumModel::getValue)
            .containsExactly(tuple("FIVE_ONE", "5.1"),
                             tuple("VALUE_5_3", "5.3"));
    }

    @Test
    void contentType_reachesTheMetadata() throws IOException {
        IntermediateModel model = build(MODEL, Jackson.load(CustomizationConfig.class,
                                                            "{\"customServiceMetadata\": {\"contentType\": "
                                                            + "\"application/json\"}}"));

        assertThat(model.getMetadata().getContentType()).isEqualTo("application/json");
    }

    private static MemberModel member(IntermediateModel model, String shape, String member) {
        return Utils.findShapeModelByC2jName(model, shape).findMemberModelByC2jName(member);
    }

    private static IntermediateModel build(String smithy, CustomizationConfig config) {
        Model model = Model.assembler()
                           .discoverModels(SmithyCodegenTraitsTranslationTest.class.getClassLoader())
                           .addUnparsedModel("test.smithy", smithy)
                           .assemble()
                           .unwrap();
        return new SmithyIntermediateModelBuilder(SmithyModels.builder()
                                                              .model(model)
                                                              .customizationConfig(config)
                                                              .build())
            .build();
    }
}
