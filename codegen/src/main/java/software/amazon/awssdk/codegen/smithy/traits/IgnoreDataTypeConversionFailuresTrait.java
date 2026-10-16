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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AnnotationTrait;

/**
 * {@code software.amazon.awssdk.codegen#ignoreDataTypeConversionFailures}: the unmarshaller ignores a value that cannot
 * be converted to the member's type. Replaces the {@code customization.config} setting of the same name.
 */
@SdkInternalApi
public final class IgnoreDataTypeConversionFailuresTrait extends AnnotationTrait {

    public static final ShapeId ID = ShapeId.from("software.amazon.awssdk.codegen#ignoreDataTypeConversionFailures");

    public IgnoreDataTypeConversionFailuresTrait(ObjectNode node) {
        super(ID, node);
    }

    public IgnoreDataTypeConversionFailuresTrait() {
        this(Node.objectNode());
    }

    /**
     * Registered in {@code META-INF/services} so Smithy builds this class when it loads the trait.
     */
    public static final class Provider extends AnnotationTrait.Provider<IgnoreDataTypeConversionFailuresTrait> {
        public Provider() {
            super(ID, IgnoreDataTypeConversionFailuresTrait::new);
        }
    }
}
