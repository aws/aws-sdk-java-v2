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

package software.amazon.awssdk.codegen.model.intermediate;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class MemberModelTest {
    @Test
    public void equals_isCorrect() {
        ListModel redListModel = new ListModel();
        redListModel.setMemberLocationName("RedLocation");
        ListModel blueListModel = new ListModel();
        blueListModel.setMemberLocationName("BlueLocation");

        MemberModel redMemberModel = new MemberModel();
        redMemberModel.setC2jName("RedC2jName");
        MemberModel blueMemberModel = new MemberModel();
        blueMemberModel.setC2jName("BlueC2jName");

        EqualsVerifier.simple().forClass(MemberModel.class)
            .withPrefabValues(ListModel.class, redListModel, blueListModel)
            .withPrefabValues(MemberModel.class, redMemberModel, blueMemberModel)
            .usingGetClass()
            .verify();
    }
}
