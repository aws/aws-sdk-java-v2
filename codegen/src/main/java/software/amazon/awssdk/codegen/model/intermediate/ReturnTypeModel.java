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

package software.amazon.awssdk.codegen.model.intermediate;

import java.util.Objects;

public class ReturnTypeModel {

    private String returnType;

    private String documentation;

    public ReturnTypeModel() {
    }

    public ReturnTypeModel(String returnType) {
        setReturnType(returnType);
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public ReturnTypeModel withDocumentation(String documentation) {
        setDocumentation(documentation);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ReturnTypeModel that = (ReturnTypeModel) o;
        return Objects.equals(returnType, that.returnType) && Objects.equals(documentation, that.documentation);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(returnType);
        result = 31 * result + Objects.hashCode(documentation);
        return result;
    }
}
