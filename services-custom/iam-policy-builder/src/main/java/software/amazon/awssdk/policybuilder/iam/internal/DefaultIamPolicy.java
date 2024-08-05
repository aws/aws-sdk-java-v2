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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.NotNull;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.policybuilder.iam.IamPolicy;
import software.amazon.awssdk.policybuilder.iam.IamPolicyWriter;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link IamPolicy}.
 *
 * @see IamPolicy#create
 * @see IamPolicy#fromJson(String)
 * @see IamPolicy#builder()
 */
@SdkInternalApi
public final class DefaultIamPolicy implements IamPolicy {
    private final String id;
    @NotNull private final String version;
    @NotNull private final List<IamStatement> statements;

    public DefaultIamPolicy(Builder builder) {
        this.id = builder.id;
        this.version = builder.version != null ? builder.version : "2012-10-17";
        this.statements = new ArrayList<>(Validate.notEmpty(builder.statements,
                                                            "At least one policy statement is required."));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public List<IamStatement> statements() {
        return Collections.unmodifiableList(statements);
    }

    @Override
    public String toJson() {
        return toJson(IamPolicyWriter.create());
    }

    @Override
    public String toJson(IamPolicyWriter writer) {
        return writer.writeToString(this);
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

        DefaultIamPolicy that = (DefaultIamPolicy) o;

        if (!Objects.equals(id, that.id)) {
            return false;
        }
        if (!version.equals(that.version)) {
            return false;
        }
        if (!statements.equals(that.statements)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + version.hashCode();
        result = 31 * result + statements.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("IamPolicy")
                       .add("id", id)
                       .add("version", version)
                       .add("statements", statements.isEmpty() ? null : statements)
                       .build();
    }

    public static class Builder implements IamPolicy.Builder {
        private String id;
        private String version;
        private final List<IamStatement> statements = new ArrayList<>();

        private Builder() {
        }

        private Builder(DefaultIamPolicy policy) {
            this.id = policy.id;
            this.version = policy.version;
            this.statements.addAll(policy.statements);
        }

        @Override
        public IamPolicy.Builder id(String id) {
            this.id = id;
            return this;
        }

        @Override
        public IamPolicy.Builder version(String version) {
            this.version = version;
            return this;
        }

        @Override
        public IamPolicy.Builder statements(Collection<IamStatement> statements) {
            this.statements.clear();
            if (statements != null) {
                this.statements.addAll(statements);
            }
            return this;
        }

        @Override
        public IamPolicy.Builder addStatement(IamStatement statement) {
            Validate.paramNotNull(statement, "statement");
            this.statements.add(statement);
            return this;
        }

        @Override
        public IamPolicy.Builder addStatement(Consumer<IamStatement.Builder> statement) {
            Validate.paramNotNull(statement, "statement");
            this.statements.add(IamStatement.builder().applyMutation(statement).build());
            return this;
        }

        @Override
        public IamPolicy build() {
            return new DefaultIamPolicy(this);
        }
    }
}
