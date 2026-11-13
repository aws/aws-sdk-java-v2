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

package software.amazon.awssdk.codegen.smithy.customization.shapename;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;

class QAppsSplitActionEnumTest {
    private static final String QAPPS = "$version: \"2.0\"\nnamespace com.amazonaws.qapps\n"
                                        + "service QAppsService { version: \"2023-11-27\" }\n"
                                        + "enum Action {\n  READ = \"read\"\n  WRITE = \"write\"\n}\n"
                                        + "enum UserType {\n  OWNER = \"owner\"\n}\n"
                                        + "structure PermissionInput { action: Action }\n"
                                        + "structure PermissionOutput { action: Action }\n"
                                        + "structure PrincipalOutput { userType: UserType }\n";
    private static final CustomizationConfig CONFIG = CustomizationConfig.create();

    @Test
    void inputGetsACopyOfTheEnum() {
        Model model = new QAppsSplitActionEnum().preprocess(qapps(), CONFIG);

        assertThat(target(model, "PermissionInput$action")).isEqualTo(id("PermissionInputActionEnum"));
        assertThat(target(model, "PermissionOutput$action")).isEqualTo(id("Action"));
        assertThat(model.expectShape(id("PermissionInputActionEnum"), EnumShape.class).getEnumValues())
            .isEqualTo(model.expectShape(id("Action"), EnumShape.class).getEnumValues());
    }

    @Test
    void splitBeforeOrAfterTheRenames_givesTheSameModel() {
        QAppsSplitActionEnum split = new QAppsSplitActionEnum();
        RenameShapesToC2jNames rename = new RenameShapesToC2jNames();

        Model splitFirst = rename.preprocess(split.preprocess(qapps(), CONFIG), CONFIG);
        Model renameFirst = split.preprocess(rename.preprocess(qapps(), CONFIG), CONFIG);

        assertThat(target(splitFirst, "PermissionInput$action")).isEqualTo(id("PermissionInputActionEnum"));
        assertThat(target(splitFirst, "PermissionOutput$action")).isEqualTo(id("PermissionOutputActionEnum"));
        assertThat(renameFirst).isEqualTo(splitFirst);
    }

    @Test
    void anInputWithItsOwnEnum_isUnchanged() {
        Model once = new QAppsSplitActionEnum().preprocess(qapps(), CONFIG);

        assertThat(new QAppsSplitActionEnum().preprocess(once, CONFIG)).isSameAs(once);
    }

    private static Model qapps() {
        return Model.assembler().addUnparsedModel("qapps.smithy", QAPPS).assemble().unwrap();
    }

    private static ShapeId target(Model model, String member) {
        return model.expectShape(id(member), MemberShape.class).getTarget();
    }

    private static ShapeId id(String name) {
        return ShapeId.from("com.amazonaws.qapps#" + name);
    }
}
