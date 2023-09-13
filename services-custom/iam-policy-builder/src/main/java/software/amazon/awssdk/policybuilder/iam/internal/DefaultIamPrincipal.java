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
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link IamPrincipal}.
 *
 * @see IamPrincipal#create
 */
@SdkInternalApi
public final class DefaultIamPrincipal implements IamPrincipal {
    @NotNull private final IamPrincipalType type;
    @NotNull private final String id;

    private DefaultIamPrincipal(Builder builder) {
        this.type = Validate.paramNotNull(builder.type, "principalType");
        this.id = Validate.paramNotNull(builder.id, "principalId");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public IamPrincipalType type() {
        return type;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultIamPrincipal that = (DefaultIamPrincipal) o;

        if (!type.equals(that.type)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("IamPrincipal")
                       .add("type", type.value())
                       .add("id", id)
                       .build();
    }

    public static class Builder implements IamPrincipal.Builder {
        private IamPrincipalType type;
        private String id;

        private Builder() {
        }

        private Builder(DefaultIamPrincipal principal) {
            this.type = principal.type;
            this.id = principal.id;
        }

        @Override
        public IamPrincipal.Builder type(IamPrincipalType type) {
            this.type = type;
            return this;
        }

        @Override
        public IamPrincipal.Builder type(String type) {
            this.type = type == null ? null : IamPrincipalType.create(type);
            return this;
        }

        @Override
        public IamPrincipal.Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public IamPrincipal build() {
            return new DefaultIamPrincipal(this);
        }
    }
}
