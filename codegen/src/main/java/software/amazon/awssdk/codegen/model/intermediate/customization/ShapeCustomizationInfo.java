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

package software.amazon.awssdk.codegen.model.intermediate.customization;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ShapeCustomizationInfo {

    private ArtificialResultWrapper artificialResultWrapper;
    private boolean skipGeneratingModelClass;
    private boolean skipGeneratingMarshaller;
    private boolean skipGeneratingUnmarshaller;
    private int staxTargetDepthOffset;
    private boolean hasStaxTargetDepthOffset = false;

    public ArtificialResultWrapper getArtificialResultWrapper() {
        return artificialResultWrapper;
    }

    public void setArtificialResultWrapper(
            ArtificialResultWrapper artificialResultWrapper) {
        this.artificialResultWrapper = artificialResultWrapper;
    }

    public boolean isSkipGeneratingModelClass() {
        return skipGeneratingModelClass;
    }

    public void setSkipGeneratingModelClass(boolean skipGeneratingModelClass) {
        this.skipGeneratingModelClass = skipGeneratingModelClass;
    }

    public boolean isSkipGeneratingMarshaller() {
        return skipGeneratingMarshaller;
    }

    public void setSkipGeneratingMarshaller(boolean skipGeneratingMarshaller) {
        this.skipGeneratingMarshaller = skipGeneratingMarshaller;
    }

    public boolean isSkipGeneratingUnmarshaller() {
        return skipGeneratingUnmarshaller;
    }

    public void setSkipGeneratingUnmarshaller(boolean skipGeneratingUnmarshaller) {
        this.skipGeneratingUnmarshaller = skipGeneratingUnmarshaller;
    }

    public Integer getStaxTargetDepthOffset() {
        return staxTargetDepthOffset;
    }

    public void setStaxTargetDepthOffset(int staxTargetDepthOffset) {
        hasStaxTargetDepthOffset = true;
        this.staxTargetDepthOffset = staxTargetDepthOffset;
    }

    @JsonIgnore
    public boolean hasStaxTargetDepthOffset() {
        return hasStaxTargetDepthOffset;
    }
}
