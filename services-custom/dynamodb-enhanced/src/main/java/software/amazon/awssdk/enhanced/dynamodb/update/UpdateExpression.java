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
import java.util.Collection;
import java.util.Collections;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Contains sets of {@link UpdateAction} that represent the four DynamoDB update actions: SET, ADD, REMOVE and DELETE.
 * <p>
 * Use this class to build an immutable UpdateExpression with one or more UpdateAction. An UpdateExpression may be merged
 * with another. When two UpdateExpression are merged, the actions of each group of UpdateAction, should they exist, are
 * combined; all SET actions from each expression are concatenated, all REMOVE actions etc.
 * <p>
 * DynamoDb Enhanced will convert the UpdateExpression to a format readable by DynamoDb,
 * <p>
 * Examples of adding actions for attributes directly:
 * <pre>
 * {@code
 * RemoveUpdateAction removeAction = ...
 * List<SetUpdateAction> setActions = ...
 * UpdateExpression.builder()
 *                 .addAction(removeAction)
 *                 .actions(setActions)
 *                 .build();
 * }
 * </pre>
 *
 * See respective subtype of {@link UpdateAction}, for example {@link SetUpdateAction}, for details on creating that action.
 */
@SdkPublicApi
public final class UpdateExpression {

    private final Collection<RemoveUpdateAction> removeActions;
    private final Collection<SetUpdateAction> setActions;
    private final Collection<DeleteUpdateAction> deleteActions;
    private final Collection<AddUpdateAction> addActions;

    private UpdateExpression(Builder builder) {
        this.removeActions = builder.removeActions != null ? builder.removeActions : new ArrayList<>();
        this.setActions = builder.setActions != null ? builder.setActions : new ArrayList<>();
        this.deleteActions = builder.deleteActions != null ? builder.deleteActions : new ArrayList<>();
        this.addActions = builder.addActions != null ? builder.addActions : new ArrayList<>();
    }

    /**
     * Constructs a new builder for {@link UpdateExpression}.
     *
     * @return a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public Collection<RemoveUpdateAction> removeActions() {
        return Collections.unmodifiableCollection(removeActions);
    }

    public Collection<SetUpdateAction> setActions() {
        return Collections.unmodifiableCollection(setActions);
    }

    public Collection<DeleteUpdateAction> deleteActions() {
        return Collections.unmodifiableCollection(deleteActions);
    }

    public Collection<AddUpdateAction> addActions() {
        return Collections.unmodifiableCollection(addActions);
    }

    /**
     * Merges the supplied UpdateExpression with the current
     */
    public void mergeExpression(UpdateExpression expression) {
        if (expression == null) {
            return;
        }
        removeActions.addAll(expression.removeActions());
        setActions.addAll(expression.setActions());
        deleteActions.addAll(expression.deleteActions());
        addActions.addAll(expression.addActions());
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

        private Collection<RemoveUpdateAction> removeActions;
        private Collection<SetUpdateAction> setActions;
        private Collection<DeleteUpdateAction> deleteActions;
        private Collection<AddUpdateAction> addActions;

        private Builder() {
        }

        /**
         * Add an action of type {@link RemoveUpdateAction}
         */
        public Builder addAction(RemoveUpdateAction action) {
            if (this.removeActions == null) {
                this.removeActions = new ArrayList<>();
            }
            removeActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link SetUpdateAction}
         */
        public Builder addAction(SetUpdateAction action) {
            if (this.setActions == null) {
                this.setActions = new ArrayList<>();
            }
            setActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link DeleteUpdateAction}
         */
        public Builder addAction(DeleteUpdateAction action) {
            if (this.deleteActions == null) {
                this.deleteActions = new ArrayList<>();
            }
            deleteActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link AddUpdateAction}
         */
        public Builder addAction(AddUpdateAction action) {
            if (this.addActions == null) {
                this.addActions = new ArrayList<>();
            }
            addActions.add(action);
            return this;
        }

        /**
         * Adds a collection of {@link UpdateAction} of any subtype to the builder. Calling this operation
         * repeatedly will add the new collection to existing items and not overwrite them.
         */
        public Builder actions(Collection<UpdateAction> actions) {
            addActions(actions);
            return this;
        }

        /**
         * Adds a collection of {@link UpdateAction} of any subtype to the builder. Calling this operation
         * repeatedly will add the new collection to existing items and not overwrite them.
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

        private void addActions(Collection<UpdateAction> actions) {
            if (actions != null) {
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
                throw new IllegalArgumentException("Do not recognize this UpdateAction");
            }
        }
    }
}
