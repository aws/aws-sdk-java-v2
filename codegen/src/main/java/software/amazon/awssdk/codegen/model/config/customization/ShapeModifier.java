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
import software.amazon.awssdk.codegen.model.service.Member;

/**
 * Use shapeModifiers customization to add/remove shape members or to modify the
 * properties of a member.
 */
public class ShapeModifier {

    private boolean excludeShape;
    private List<String> exclude;
    private List<Map<String, ModifyModelShapeModifier>> modify;
    private List<Map<String, Member>> inject;
    private Integer staxTargetDepthOffset;

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
    public List<Map<String, ModifyModelShapeModifier>> getModify() {
        return modify;
    }

    public void setModify(List<Map<String, ModifyModelShapeModifier>> modify) {
        this.modify = modify;
    }

    /**
     * @return A list of singleton maps, each containing a custom member that we want to inject to this shape.
     */
    public List<Map<String, Member>> getInject() {
        return inject;
    }

    public void setInject(List<Map<String, Member>> inject) {
        this.inject = inject;
    }

    /**
     * @return the depth offset to use during staxUnmarshalling
     */
    public Integer getStaxTargetDepthOffset() {
        return staxTargetDepthOffset;
    }

    public void setStaxTargetDepthOffset(Integer staxTargetDepthOffset) {
        this.staxTargetDepthOffset = staxTargetDepthOffset;
    }
}
