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

package software.amazon.awssdk.codegen.model.config.customization;

import java.util.Map;

/**
 * Smithy-native equivalent of {@link CustomSdkShapes}. Defines custom shapes
 * using Smithy's JSON AST format instead of C2J Shape objects.
 *
 * <p>The {@code smithyAst} field holds a standard Smithy JSON AST document.
 * It is parsed using {@code ModelAssembler.addUnparsedModel()} and the
 * resulting shapes are merged into the existing model.
 */
public class SmithyCustomSdkShapes {

    /**
     * A Smithy JSON AST document. Must contain a "smithy" version key
     * and a "shapes" map with absolute ShapeIds as keys and standard
     * Smithy JSON AST shape definitions as values.
     */
    private Map<String, Object> smithyAst;

    public Map<String, Object> getSmithyAst() {
        return smithyAst;
    }

    public void setSmithyAst(Map<String, Object> smithyAst) {
        this.smithyAst = smithyAst;
    }
}
