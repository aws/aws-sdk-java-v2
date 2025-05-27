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
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ShapeCustomizationInfo that = (ShapeCustomizationInfo) o;
        return skipGeneratingModelClass == that.skipGeneratingModelClass
               && skipGeneratingMarshaller == that.skipGeneratingMarshaller
               && skipGeneratingUnmarshaller == that.skipGeneratingUnmarshaller
               && staxTargetDepthOffset == that.staxTargetDepthOffset
               && hasStaxTargetDepthOffset == that.hasStaxTargetDepthOffset
               && Objects.equals(artificialResultWrapper, that.artificialResultWrapper);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(artificialResultWrapper);
        result = 31 * result + Boolean.hashCode(skipGeneratingModelClass);
        result = 31 * result + Boolean.hashCode(skipGeneratingMarshaller);
        result = 31 * result + Boolean.hashCode(skipGeneratingUnmarshaller);
        result = 31 * result + staxTargetDepthOffset;
        result = 31 * result + Boolean.hashCode(hasStaxTargetDepthOffset);
        return result;
    }
}
