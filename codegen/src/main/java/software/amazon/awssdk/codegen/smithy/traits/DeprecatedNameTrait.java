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

package software.amazon.awssdk.codegen.smithy.traits;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * {@code software.amazon.awssdk.codegen#deprecatedName}: the member's name before a rename. The generated class keeps
 * deprecated accessors under this name. Replaces {@code existingNameDeprecated} in {@code customization.config}.
 */
@SdkInternalApi
public final class DeprecatedNameTrait extends StringTrait {

    public static final ShapeId ID = ShapeId.from("software.amazon.awssdk.codegen#deprecatedName");

    public DeprecatedNameTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public DeprecatedNameTrait(String value) {
        this(value, SourceLocation.NONE);
    }

    /**
     * Registered in {@code META-INF/services} so Smithy builds this class when it loads the trait.
     */
    public static final class Provider extends StringTrait.Provider<DeprecatedNameTrait> {
        public Provider() {
            super(ID, DeprecatedNameTrait::new);
        }
    }
}
