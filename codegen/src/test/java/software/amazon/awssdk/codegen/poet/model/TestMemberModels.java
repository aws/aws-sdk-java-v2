/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.model;

import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;

class TestMemberModels {
    private final IntermediateModel intermediateModel;
    private Map<String, MemberModel> shapeToMemberMap;

    public TestMemberModels(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
    }

    /**
     * Returns a mapping of c2jshapes to a single MemberModel in the intermediate model.
     *
     * @return
     */
    public Map<String, MemberModel> shapeToMemberMap() {
        if (shapeToMemberMap == null) {
            shapeToMemberMap = new HashMap<>();


            intermediateModel.getShapes().values().stream()
                    .flatMap(s -> s.getMembers().stream())
                    .forEach(m -> shapeToMemberMap.put(m.getC2jShape(), m));
        }

        return shapeToMemberMap;
    }
}
