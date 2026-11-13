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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

class RenameShapesToC2jNamesTest {

    @Test
    void renamesAShapeInItsNamespace() {
        Model model = rename("namespace com.amazonaws.deadline\n"
                             + "service Deadline { version: \"2023-10-12\" }\n"
                             + "string DeadlinePrincipalType\n"
                             + "structure Member { principalType: DeadlinePrincipalType }\n");

        assertThat(model.getShape(id("com.amazonaws.deadline#DeadlinePrincipalType"))).isEmpty();
        assertThat(target(model, "com.amazonaws.deadline#Member$principalType"))
            .isEqualTo(id("com.amazonaws.deadline#PrincipalType"));
    }

    @Test
    void movesAShapeWhoseNameAnOperationHas() {
        Model model = rename("namespace com.amazonaws.medialive\n"
                             + "service MediaLive { version: \"2017-10-14\", operations: [UpdateNodeState] }\n"
                             + "operation UpdateNodeState { input: UpdateNodeStateShape }\n"
                             + "structure UpdateNodeStateShape {}\n");

        ShapeId operation = id("com.amazonaws.medialive#UpdateNodeState");
        assertThat(model.expectShape(operation, OperationShape.class).getInputShape())
            .isEqualTo(id("com.amazonaws.medialive.sdk#UpdateNodeState"));
    }

    @Test
    void renamesEveryShapeOfTheService() {
        Model model = rename("namespace com.amazonaws.qapps\n"
                             + "service QAppsService { version: \"2023-11-27\" }\n"
                             + "enum Action {\n  READ = \"read\"\n}\n"
                             + "enum UserType {\n  OWNER = \"owner\"\n}\n"
                             + "structure PermissionOutput { action: Action }\n"
                             + "structure PrincipalOutput { userType: UserType }\n");

        assertThat(target(model, "com.amazonaws.qapps#PermissionOutput$action"))
            .isEqualTo(id("com.amazonaws.qapps#PermissionOutputActionEnum"));
        assertThat(target(model, "com.amazonaws.qapps#PrincipalOutput$userType"))
            .isEqualTo(id("com.amazonaws.qapps#PrincipalOutputUserTypeEnum"));
    }

    @Test
    void aRenamedShapeMissingFromTheModel_fails() {
        assertThatThrownBy(() -> rename("namespace com.amazonaws.notifications\n"
                                        + "service Notifications { version: \"2018-05-10\" }\n"))
            .hasMessageContaining("NotificationEventSchema");
    }

    private static Model rename(String smithy) {
        Model model = Model.assembler()
                           .addUnparsedModel("test.smithy", "$version: \"2.0\"\n" + smithy)
                           .assemble()
                           .unwrap();
        return new RenameShapesToC2jNames().preprocess(model, CustomizationConfig.create());
    }

    private static ShapeId target(Model model, String member) {
        return model.expectShape(id(member), MemberShape.class).getTarget();
    }

    private static ShapeId id(String id) {
        return ShapeId.from(id);
    }
}
