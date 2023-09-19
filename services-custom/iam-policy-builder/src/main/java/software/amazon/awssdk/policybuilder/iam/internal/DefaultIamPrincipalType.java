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

package software.amazon.awssdk.policybuilder.iam.internal;

import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link IamPrincipalType}.
 *
 * @see IamPrincipalType#create
 */
@SdkInternalApi
public final class DefaultIamPrincipalType implements IamPrincipalType {
    @NotNull private final String value;

    public DefaultIamPrincipalType(String value) {
        this.value = Validate.paramNotNull(value, "principalTypeValue");
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultIamPrincipalType that = (DefaultIamPrincipalType) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return ToString.builder("IamPrincipalType")
                       .add("value", value)
                       .build();
    }
}
