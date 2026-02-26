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
import java.util.Map;

/**
 * Smithy-native equivalent of {@link ShapeModifier}. Uses ShapeId-based
 * references instead of C2J simple name strings.
 */
public class SmithyShapeModifier {

    private boolean excludeShape;
    private List<String> exclude;
    private List<Map<String, SmithyModifyShapeModifier>> modify;
    private List<SmithyMemberDefinition> inject;
    private Boolean union;

    /**
     * @return true if the whole shape should be excluded.
     */
    public boolean isExcludeShape() {
        return excludeShape;
    }

    public void setExcludeShape(boolean excludeShape) {
        this.excludeShape = excludeShape;
    }

    /**
     * @return A list of member names that should be excluded when processing
     *         the given shape.
     */
    public List<String> getExclude() {
        return exclude;
    }

    public void setExclude(List<String> exclude) {
        this.exclude = exclude;
    }

    /**
     * @return List of singleton maps, each containing the name of a shape
     *         member, and the modifications that we want to apply to it.
     */
    public List<Map<String, SmithyModifyShapeModifier>> getModify() {
        return modify;
    }

    public void setModify(List<Map<String, SmithyModifyShapeModifier>> modify) {
        this.modify = modify;
    }

    /**
     * Members to inject. Each definition specifies a member name and a
     * ShapeId target (e.g., "com.amazonaws.s3#BucketName") instead of
     * a C2J Member object.
     */
    public List<SmithyMemberDefinition> getInject() {
        return inject;
    }

    public void setInject(List<SmithyMemberDefinition> inject) {
        this.inject = inject;
    }

    public Boolean isUnion() {
        return union;
    }

    public void setUnion(Boolean union) {
        this.union = union;
    }
}
