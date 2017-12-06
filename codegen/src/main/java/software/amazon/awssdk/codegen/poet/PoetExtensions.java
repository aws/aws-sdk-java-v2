/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet;

import com.squareup.javapoet.ClassName;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;

/**
 * Extension and convenience methods to Poet that use the intermediate model.
 */
public class PoetExtensions {

    private final IntermediateModel model;

    public PoetExtensions(IntermediateModel model) {
        this.model = model;
    }

    /**
     * @param className Simple name of class in model package.
     * @return A Poet {@link ClassName} for the given class in the model package.
     */
    public ClassName getModelClass(String className) {
        return ClassName.get(model.getMetadata().getFullModelPackageName(), className);
    }

    /**
     * @param className Simple name of class in transform package.
     * @return A Poet {@link ClassName} for the given class in the transform package.
     */
    public ClassName getTransformClass(String className) {
        return ClassName.get(model.getMetadata().getFullTransformPackageName(), className);
    }

    /**
     * @param className Simple name of class in transform package.
     * @return A Poet {@link ClassName} for the given class in the transform package.
     */
    public ClassName getRequestTransformClass(String className) {
        return ClassName.get(model.getMetadata().getFullRequestTransformPackageName(), className);
    }

    /**
     * @param className Simple name of class in base service package (i.e. software.amazon.awssdk.services.dynamodb).
     * @return A Poet {@link ClassName} for the given class in the base service package.
     */
    public ClassName getClientClass(String className) {
        return ClassName.get(model.getMetadata().getFullClientPackageName(), className);
    }

    /**
     * @param operationName Name of the operation
     * @return A Poet {@link ClassName} for the response type of a paginated operation in the base service package.
     *
     * Example: If operationName is "ListTables", then the response type of the paginated operation
     * will be "ListTablesPaginator" class in the base service package.
     */
    @ReviewBeforeRelease("Naming of response shape for paginated APIs")
    public ClassName getResponseClassForPaginatedSyncOperation(String operationName) {
        return ClassName.get(model.getMetadata().getFullPaginatorsPackageName(), operationName + "Paginator");
    }

}
