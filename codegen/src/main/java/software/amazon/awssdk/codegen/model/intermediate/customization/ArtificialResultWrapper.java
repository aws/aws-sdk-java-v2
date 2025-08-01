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

import java.util.Objects;

public class ArtificialResultWrapper {
    private String wrappedMemberName;
    private String wrappedMemberSimpleType;

    public String getWrappedMemberName() {
        return wrappedMemberName;
    }

    public void setWrappedMemberName(String wrappedMemberName) {
        this.wrappedMemberName = wrappedMemberName;
    }

    public String getWrappedMemberSimpleType() {
        return wrappedMemberSimpleType;
    }

    public void setWrappedMemberSimpleType(String wrappedMemberSimpleType) {
        this.wrappedMemberSimpleType = wrappedMemberSimpleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArtificialResultWrapper that = (ArtificialResultWrapper) o;
        return Objects.equals(wrappedMemberName, that.wrappedMemberName)
               && Objects.equals(wrappedMemberSimpleType, that.wrappedMemberSimpleType);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(wrappedMemberName);
        result = 31 * result + Objects.hashCode(wrappedMemberSimpleType);
        return result;
    }
}
