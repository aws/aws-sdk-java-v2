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

import java.util.List;

/**
 * Smithy-native equivalent of {@link ShapeSubstitution}. Uses full ShapeId
 * strings instead of simple shape names.
 */
public class SmithyShapeSubstitution {

    /**
     * Full ShapeId of the shape to substitute with
     * (e.g., "com.amazonaws.ec2#Value" instead of "Value").
     */
    private String emitAsShape;

    /**
     * Emit as a different primitive type.
     */
    private String emitAsType;

    /**
     * Member name to use as data source.
     */
    private String emitFromMember;

    /**
     * Full ShapeId strings for shapes where the additional marshalling
     * path should not be added.
     */
    private List<String> skipMarshallPathForShapes;

    public String getEmitAsShape() {
        return emitAsShape;
    }

    public void setEmitAsShape(String emitAsShape) {
        this.emitAsShape = emitAsShape;
    }

    public String getEmitAsType() {
        return emitAsType;
    }

    public void setEmitAsType(String emitAsType) {
        this.emitAsType = emitAsType;
    }

    public String getEmitFromMember() {
        return emitFromMember;
    }

    public void setEmitFromMember(String emitFromMember) {
        this.emitFromMember = emitFromMember;
    }

    public List<String> getSkipMarshallPathForShapes() {
        return skipMarshallPathForShapes;
    }

    public void setSkipMarshallPathForShapes(List<String> skipMarshallPathForShapes) {
        this.skipMarshallPathForShapes = skipMarshallPathForShapes;
    }
}
