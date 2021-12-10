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

package software.amazon.awssdk.enhanced.dynamodb.update;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Contains sets of {@link UpdateAction} that represent the four DynamoDB update actions: SET, ADD, REMOVE and DELETE.
 * <p>
 * Use this class to build an immutable UpdateExpression with one or more UpdateAction. An UpdateExpression may be merged
 * with another. When two UpdateExpression are merged, the actions of each group of UpdateAction, should they exist, are
 * combined; all SET actions from each expression are concatenated, all REMOVE actions etc.
 * <p>
 * DynamoDb Enhanced will convert the UpdateExpression to a format readable by DynamoDb,
 * <p>
 * Example:-
 * <pre>
 * {@code
 * RemoveUpdateAction removeAction = ...
 * SetUpdateAction setAction = ...
 * UpdateExpression.builder()
 *                 .addAction(removeAction)
 *                 .addAction(setAction)
 *                 .build();
 * }
 * </pre>
 *
 * See respective subtype of {@link UpdateAction}, for example {@link SetUpdateAction}, for details on creating that action.
 */
@SdkPublicApi
public final class UpdateExpression {

    private final List<RemoveUpdateAction> removeActions;
    private final List<SetUpdateAction> setActions;
    private final List<DeleteUpdateAction> deleteActions;
    private final List<AddUpdateAction> addActions;

    private UpdateExpression(Builder builder) {
        this.removeActions = builder.removeActions;
        this.setActions = builder.setActions;
        this.deleteActions = builder.deleteActions;
        this.addActions = builder.addActions;
    }

    /**
     * Constructs a new builder for {@link UpdateExpression}.
     *
     * @return a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public List<RemoveUpdateAction> removeActions() {
        return Collections.unmodifiableList(new ArrayList<>(removeActions));
    }

    public List<SetUpdateAction> setActions() {
        return Collections.unmodifiableList(new ArrayList<>(setActions));
    }

    public List<DeleteUpdateAction> deleteActions() {
        return Collections.unmodifiableList(new ArrayList<>(deleteActions));
    }

    public List<AddUpdateAction> addActions() {
        return Collections.unmodifiableList(new ArrayList<>(addActions));
    }

    /**
     * Merges the supplied UpdateExpression with the current
     */
    public void mergeExpression(UpdateExpression expression) {
        if (expression == null) {
            return;
        }
        if (!CollectionUtils.isNullOrEmpty(expression.removeActions)) {
            removeActions.addAll(expression.removeActions());
        }
        if (!CollectionUtils.isNullOrEmpty(expression.setActions)) {
            setActions.addAll(expression.setActions());
        }
        if (!CollectionUtils.isNullOrEmpty(expression.deleteActions)) {
            deleteActions.addAll(expression.deleteActions());
        }
        if (!CollectionUtils.isNullOrEmpty(expression.addActions)) {
            addActions.addAll(expression.addActions());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UpdateExpression that = (UpdateExpression) o;

        if (removeActions != null ? ! removeActions.equals(that.removeActions) : that.removeActions != null) {
            return false;
        }
        if (setActions != null ? ! setActions.equals(that.setActions) : that.setActions != null) {
            return false;
        }
        if (deleteActions != null ? ! deleteActions.equals(that.deleteActions) : that.deleteActions != null) {
            return false;
        }
        return addActions != null ? addActions.equals(that.addActions) : that.addActions == null;
    }

    @Override
    public int hashCode() {
        int result = removeActions != null ? removeActions.hashCode() : 0;
        result = 31 * result + (setActions != null ? setActions.hashCode() : 0);
        result = 31 * result + (deleteActions != null ? deleteActions.hashCode() : 0);
        result = 31 * result + (addActions != null ? addActions.hashCode() : 0);
        return result;
    }

    /**
     * A builder for {@link UpdateExpression}
     */
    public static final class Builder {

        private List<RemoveUpdateAction> removeActions = new ArrayList<>();
        private List<SetUpdateAction> setActions = new ArrayList<>();
        private List<DeleteUpdateAction> deleteActions = new ArrayList<>();
        private List<AddUpdateAction> addActions = new ArrayList<>();

        private Builder() {
        }

        /**
         * Add an action of type {@link RemoveUpdateAction}
         */
        public Builder addAction(RemoveUpdateAction action) {
            removeActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link SetUpdateAction}
         */
        public Builder addAction(SetUpdateAction action) {
            setActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link DeleteUpdateAction}
         */
        public Builder addAction(DeleteUpdateAction action) {
            deleteActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link AddUpdateAction}
         */
        public Builder addAction(AddUpdateAction action) {
            addActions.add(action);
            return this;
        }

        /**
         * Adds a list of {@link UpdateAction} of any subtype to the builder, overwriting any previous values.
         */
        public Builder actions(List<? extends UpdateAction> actions) {
            replaceActions(actions);
            return this;
        }

        /**
         * Adds a list of {@link UpdateAction} of any subtype to the builder, overwriting any previous values.
         */
        public Builder actions(UpdateAction... actions) {
            actions(Arrays.asList(actions));
            return this;
        }

        /**
         * Builds an {@link UpdateExpression} based on the values stored in this builder.
         */
        public UpdateExpression build() {
            return new UpdateExpression(this);
        }

        private void replaceActions(List<? extends UpdateAction> actions) {
            if (actions != null) {
                this.removeActions = new ArrayList<>();
                this.setActions = new ArrayList<>();
                this.deleteActions = new ArrayList<>();
                this.addActions = new ArrayList<>();
                actions.forEach(this::assignAction);
            }
        }

        private void assignAction(UpdateAction action) {
            if (action instanceof RemoveUpdateAction) {
                addAction((RemoveUpdateAction) action);
            } else if (action instanceof SetUpdateAction) {
                addAction((SetUpdateAction) action);
            } else if (action instanceof DeleteUpdateAction) {
                addAction((DeleteUpdateAction) action);
            } else if (action instanceof AddUpdateAction) {
                addAction((AddUpdateAction) action);
            } else {
                throw new IllegalArgumentException(
                    String.format("Do not recognize UpdateAction: %s", action.getClass()));
            }
        }
    }
}
