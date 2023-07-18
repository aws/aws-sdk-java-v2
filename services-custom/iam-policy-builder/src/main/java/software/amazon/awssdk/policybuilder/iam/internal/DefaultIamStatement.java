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
import software.amazon.awssdk.policybuilder.iam.IamAction;
import software.amazon.awssdk.policybuilder.iam.IamCondition;
import software.amazon.awssdk.policybuilder.iam.IamConditionKey;
import software.amazon.awssdk.policybuilder.iam.IamConditionOperator;
import software.amazon.awssdk.policybuilder.iam.IamEffect;
import software.amazon.awssdk.policybuilder.iam.IamPrincipal;
import software.amazon.awssdk.policybuilder.iam.IamPrincipalType;
import software.amazon.awssdk.policybuilder.iam.IamResource;
import software.amazon.awssdk.policybuilder.iam.IamStatement;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Default implementation of {@link IamStatement}.
 *
 * @see IamStatement#builder
 */
@SdkInternalApi
public final class DefaultIamStatement implements IamStatement {
    private final String sid;
    @NotNull private final IamEffect effect;
    @NotNull private final List<IamPrincipal> principals;
    @NotNull private final List<IamPrincipal> notPrincipals;
    @NotNull private final List<IamAction> actions;
    @NotNull private final List<IamAction> notActions;
    @NotNull private final List<IamResource> resources;
    @NotNull private final List<IamResource> notResources;
    @NotNull private final List<IamCondition> conditions;

    public DefaultIamStatement(Builder builder) {
        this.sid = builder.sid;
        this.effect = Validate.paramNotNull(builder.effect, "statementEffect");
        this.principals = new ArrayList<>(builder.principals);
        this.notPrincipals = new ArrayList<>(builder.notPrincipals);
        this.actions = new ArrayList<>(builder.actions);
        this.notActions = new ArrayList<>(builder.notActions);
        this.resources = new ArrayList<>(builder.resources);
        this.notResources = new ArrayList<>(builder.notResources);
        this.conditions = new ArrayList<>(builder.conditions);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String sid() {
        return sid;
    }

    @Override
    public IamEffect effect() {
        return effect;
    }

    @Override
    public List<IamPrincipal> principals() {
        return Collections.unmodifiableList(principals);
    }

    @Override
    public List<IamPrincipal> notPrincipals() {
        return Collections.unmodifiableList(notPrincipals);
    }

    @Override
    public List<IamAction> actions() {
        return Collections.unmodifiableList(actions);
    }

    @Override
    public List<IamAction> notActions() {
        return Collections.unmodifiableList(notActions);
    }

    @Override
    public List<IamResource> resources() {
        return Collections.unmodifiableList(resources);
    }

    @Override
    public List<IamResource> notResources() {
        return Collections.unmodifiableList(notResources);
    }

    @Override
    public List<IamCondition> conditions() {
        return Collections.unmodifiableList(conditions);
    }

    @Override
    public IamStatement.Builder toBuilder() {
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

        DefaultIamStatement that = (DefaultIamStatement) o;

        if (!Objects.equals(sid, that.sid)) {
            return false;
        }
        if (!effect.equals(that.effect)) {
            return false;
        }
        if (!principals.equals(that.principals)) {
            return false;
        }
        if (!notPrincipals.equals(that.notPrincipals)) {
            return false;
        }
        if (!actions.equals(that.actions)) {
            return false;
        }
        if (!notActions.equals(that.notActions)) {
            return false;
        }
        if (!resources.equals(that.resources)) {
            return false;
        }
        if (!notResources.equals(that.notResources)) {
            return false;
        }
        if (!conditions.equals(that.conditions)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = sid != null ? sid.hashCode() : 0;
        result = 31 * result + effect.hashCode();
        result = 31 * result + principals.hashCode();
        result = 31 * result + notPrincipals.hashCode();
        result = 31 * result + actions.hashCode();
        result = 31 * result + notActions.hashCode();
        result = 31 * result + resources.hashCode();
        result = 31 * result + notResources.hashCode();
        result = 31 * result + conditions.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return ToString.builder("IamStatement")
                       .add("sid", sid)
                       .add("effect", effect)
                       .add("principals", principals.isEmpty() ? null : principals)
                       .add("notPrincipals", notPrincipals.isEmpty() ? null : notPrincipals)
                       .add("actions", actions.isEmpty() ? null : actions)
                       .add("notActions", notActions.isEmpty() ? null : notActions)
                       .add("resources", resources.isEmpty() ? null : resources)
                       .add("notResources", notResources.isEmpty() ? null : notResources)
                       .add("conditions", conditions.isEmpty() ? null : conditions)
                       .build();
    }

    public static class Builder implements IamStatement.Builder {
        private String sid;
        private IamEffect effect;
        private final List<IamPrincipal> principals = new ArrayList<>();
        private final List<IamPrincipal> notPrincipals = new ArrayList<>();
        private final List<IamAction> actions = new ArrayList<>();
        private final List<IamAction> notActions = new ArrayList<>();
        private final List<IamResource> resources = new ArrayList<>();
        private final List<IamResource> notResources = new ArrayList<>();
        private final List<IamCondition> conditions = new ArrayList<>();

        private Builder() {
        }

        private Builder(DefaultIamStatement statement) {
            this.sid = statement.sid;
            this.effect = statement.effect;
            this.principals.addAll(statement.principals);
            this.notPrincipals.addAll(statement.notPrincipals);
            this.actions.addAll(statement.actions);
            this.notActions.addAll(statement.notActions);
            this.resources.addAll(statement.resources);
            this.notResources.addAll(statement.notResources);
            this.conditions.addAll(statement.conditions);

        }

        @Override
        public IamStatement.Builder sid(String sid) {
            this.sid = sid;
            return this;
        }

        @Override
        public IamStatement.Builder effect(IamEffect effect) {
            this.effect = effect;
            return this;
        }

        @Override
        public IamStatement.Builder effect(String effect) {
            this.effect = IamEffect.create(effect);
            return this;
        }

        @Override
        public IamStatement.Builder principals(Collection<IamPrincipal> principals) {
            this.principals.clear();
            if (principals != null) {
                this.principals.addAll(principals);
            }
            return this;
        }

        @Override
        public IamStatement.Builder addPrincipal(IamPrincipal principal) {
            Validate.paramNotNull(principal, "principal");
            this.principals.add(principal);
            return this;
        }

        @Override
        public IamStatement.Builder addPrincipal(Consumer<IamPrincipal.Builder> principal) {
            Validate.paramNotNull(principal, "principal");
            this.principals.add(IamPrincipal.builder().applyMutation(principal).build());
            return this;
        }

        @Override
        public IamStatement.Builder addPrincipal(IamPrincipalType iamPrincipalType, String principal) {
            return addPrincipal(IamPrincipal.create(iamPrincipalType, principal));
        }

        @Override
        public IamStatement.Builder addPrincipal(String iamPrincipalType, String principal) {
            return addPrincipal(IamPrincipal.create(iamPrincipalType, principal));
        }

        @Override
        public IamStatement.Builder addPrincipals(IamPrincipalType principalType, Collection<String> principals) {
            Validate.paramNotNull(principalType, "principals");
            for (String principal : principals) {
                this.principals.add(IamPrincipal.create(principalType, principal));
            }
            return this;
        }

        @Override
        public IamStatement.Builder addPrincipals(String principalType, Collection<String> principals) {
            return addPrincipals(IamPrincipalType.create(principalType), principals);
        }

        @Override
        public IamStatement.Builder notPrincipals(Collection<IamPrincipal> notPrincipals) {
            this.notPrincipals.clear();
            if (notPrincipals != null) {
                this.notPrincipals.addAll(notPrincipals);
            }
            return this;
        }

        @Override
        public IamStatement.Builder addNotPrincipal(IamPrincipal notPrincipal) {
            Validate.paramNotNull(notPrincipal, "notPrincipal");
            this.notPrincipals.add(notPrincipal);
            return this;
        }

        @Override
        public IamStatement.Builder addNotPrincipal(Consumer<IamPrincipal.Builder> notPrincipal) {
            Validate.paramNotNull(notPrincipal, "notPrincipal");
            this.notPrincipals.add(IamPrincipal.builder().applyMutation(notPrincipal).build());
            return this;
        }

        @Override
        public IamStatement.Builder addNotPrincipal(IamPrincipalType iamPrincipalType, String principal) {
            return addNotPrincipal(IamPrincipal.create(iamPrincipalType, principal));
        }

        @Override
        public IamStatement.Builder addNotPrincipal(String iamPrincipalType, String principal) {
            return addNotPrincipal(IamPrincipal.create(iamPrincipalType, principal));
        }

        @Override
        public IamStatement.Builder addNotPrincipals(IamPrincipalType notPrincipalType, Collection<String> notPrincipals) {
            Validate.paramNotNull(notPrincipals, "notPrincipals");
            for (String notPrincipal : notPrincipals) {
                this.notPrincipals.add(IamPrincipal.create(notPrincipalType, notPrincipal));
            }
            return this;
        }

        @Override
        public IamStatement.Builder addNotPrincipals(String notPrincipalType, Collection<String> notPrincipals) {
            return addNotPrincipals(IamPrincipalType.create(notPrincipalType), notPrincipals);
        }

        @Override
        public IamStatement.Builder actions(Collection<IamAction> actions) {
            this.actions.clear();
            if (actions != null) {
                this.actions.addAll(actions);
            }
            return this;
        }

        @Override
        public IamStatement.Builder actionIds(Collection<String> actions) {
            this.actions.clear();
            if (actions != null) {
                actions.forEach(this::addAction);
            }
            return this;
        }

        @Override
        public IamStatement.Builder addAction(IamAction action) {
            Validate.paramNotNull(action, "action");
            this.actions.add(action);
            return this;
        }

        @Override
        public IamStatement.Builder addAction(String action) {
            this.actions.add(IamAction.create(action));
            return this;
        }

        @Override
        public IamStatement.Builder notActions(Collection<IamAction> notActions) {
            this.notActions.clear();
            if (notActions != null) {
                this.notActions.addAll(notActions);
            }
            return this;
        }

        @Override
        public IamStatement.Builder notActionIds(Collection<String> notActions) {
            this.notActions.clear();
            if (notActions != null) {
                notActions.forEach(this::addNotAction);
            }
            return this;
        }

        @Override
        public IamStatement.Builder addNotAction(IamAction notAction) {
            Validate.paramNotNull(notAction, "notAction");
            this.notActions.add(notAction);
            return this;
        }

        @Override
        public IamStatement.Builder addNotAction(String notAction) {
            this.notActions.add(IamAction.create(notAction));
            return this;
        }

        @Override
        public IamStatement.Builder resources(Collection<IamResource> resources) {
            this.resources.clear();
            if (resources != null) {
                this.resources.addAll(resources);
            }
            return this;
        }

        @Override
        public IamStatement.Builder resourceIds(Collection<String> resources) {
            this.resources.clear();
            if (resources != null) {
                resources.forEach(this::addResource);
            }
            return this;
        }

        @Override
        public IamStatement.Builder addResource(IamResource resource) {
            Validate.paramNotNull(resource, "resource");
            this.resources.add(resource);
            return this;
        }

        @Override
        public IamStatement.Builder addResource(String resource) {
            this.resources.add(IamResource.create(resource));
            return this;
        }

        @Override
        public IamStatement.Builder notResources(Collection<IamResource> notResources) {
            this.notResources.clear();
            if (notResources != null) {
                this.notResources.addAll(notResources);
            }
            return this;
        }

        @Override
        public IamStatement.Builder notResourceIds(Collection<String> notResources) {
            this.notResources.clear();
            if (notResources != null) {
                notResources.forEach(this::addNotResource);
            }
            return this;
        }

        @Override
        public IamStatement.Builder addNotResource(IamResource notResource) {
            this.notResources.add(notResource);
            return this;
        }

        @Override
        public IamStatement.Builder addNotResource(String notResource) {
            this.notResources.add(IamResource.create(notResource));
            return this;
        }

        @Override
        public IamStatement.Builder conditions(Collection<IamCondition> conditions) {
            this.conditions.clear();
            if (conditions != null) {
                this.conditions.addAll(conditions);
            }
            return this;
        }

        @Override
        public IamStatement.Builder addCondition(IamCondition condition) {
            Validate.paramNotNull(condition, "condition");
            this.conditions.add(condition);
            return this;
        }

        @Override
        public IamStatement.Builder addCondition(Consumer<IamCondition.Builder> condition) {
            Validate.paramNotNull(condition, "condition");
            this.conditions.add(IamCondition.builder().applyMutation(condition).build());
            return this;
        }

        @Override
        public IamStatement.Builder addCondition(IamConditionOperator operator, IamConditionKey key, String value) {
            this.conditions.add(IamCondition.create(operator, key, value));
            return this;
        }

        @Override
        public IamStatement.Builder addCondition(IamConditionOperator operator, String key, String value) {
            return addCondition(operator, IamConditionKey.create(key), value);
        }

        @Override
        public IamStatement.Builder addCondition(String operator, String key, String value) {
            this.conditions.add(IamCondition.create(operator, key, value));
            return this;
        }

        @Override
        public IamStatement.Builder addConditions(IamConditionOperator operator,
                                                 IamConditionKey key,
                                                 Collection<String> values) {
            Validate.paramNotNull(values, "values");
            for (String value : values) {
                this.conditions.add(IamCondition.create(operator, key, value));
            }
            return this;
        }

        @Override
        public IamStatement.Builder addConditions(IamConditionOperator operator, String key, Collection<String> values) {
            return addConditions(operator, IamConditionKey.create(key), values);
        }

        @Override
        public IamStatement.Builder addConditions(String operator, String key, Collection<String> values) {
            return addConditions(IamConditionOperator.create(operator), IamConditionKey.create(key), values);
        }

        @Override
        public IamStatement build() {
            return new DefaultIamStatement(this);
        }
    }
}
