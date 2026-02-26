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

/**
 * Smithy-native equivalent of {@link OperationModifier}. Uses full ShapeId
 * strings instead of simple shape names.
 */
public class SmithyOperationModifier {

    private boolean exclude;
    private boolean useWrappingResult;

    /**
     * Full ShapeId of the wrapped result shape
     * (e.g., "com.amazonaws.ec2#Reservation" instead of "Reservation").
     */
    private String wrappedResultShape;

    /**
     * Member name in the wrapper that holds the result.
     */
    private String wrappedResultMember;

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public boolean isUseWrappingResult() {
        return useWrappingResult;
    }

    public void setUseWrappingResult(boolean useWrappingResult) {
        this.useWrappingResult = useWrappingResult;
    }

    public String getWrappedResultShape() {
        return wrappedResultShape;
    }

    public void setWrappedResultShape(String wrappedResultShape) {
        this.wrappedResultShape = wrappedResultShape;
    }

    public String getWrappedResultMember() {
        return wrappedResultMember;
    }

    public void setWrappedResultMember(String wrappedResultMember) {
        this.wrappedResultMember = wrappedResultMember;
    }
}
