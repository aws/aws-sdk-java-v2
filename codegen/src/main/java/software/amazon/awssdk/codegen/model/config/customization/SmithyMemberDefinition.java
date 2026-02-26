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
 * Defines a member to inject into a Smithy shape. Uses ShapeId-based
 * target references instead of C2J Member objects.
 */
public class SmithyMemberDefinition {

    /**
     * The member name to add to the structure.
     */
    private String name;

    /**
     * Full ShapeId of the target shape (e.g., "com.amazonaws.s3#BucketName").
     */
    private String target;

    /**
     * Optional Smithy traits to apply to the member, keyed by trait ShapeId.
     */
    private Map<String, Object> traits;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Map<String, Object> getTraits() {
        return traits;
    }

    public void setTraits(Map<String, Object> traits) {
        this.traits = traits;
    }
}
