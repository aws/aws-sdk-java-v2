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
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
 * See respective subtype of {@link UpdateAction}, for example {@link SetAction}, for details on creating that action.
 */
@SdkPublicApi
public final class UpdateExpression {

    private final List<RemoveAction> removeActions;
    private final List<SetAction> setActions;
    private final List<DeleteAction> deleteActions;
    private final List<AddAction> addActions;

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

    public List<RemoveAction> removeActions() {
        return Collections.unmodifiableList(new ArrayList<>(removeActions));
    }

    public List<SetAction> setActions() {
        return Collections.unmodifiableList(new ArrayList<>(setActions));
    }

    public List<DeleteAction> deleteActions() {
        return Collections.unmodifiableList(new ArrayList<>(deleteActions));
    }

    public List<AddAction> addActions() {
        return Collections.unmodifiableList(new ArrayList<>(addActions));
    }

    /**
     * Merges two UpdateExpression, returning a new
     */
    public static UpdateExpression mergeExpressions(UpdateExpression expression1, UpdateExpression expression2) {
        if (expression1 == null) {
            return expression2;
        }
        if (expression2 == null) {
            return expression1;
        }
        Builder builder = builder();
        builder.removeActions = Stream.concat(expression1.removeActions.stream(),
                                              expression2.removeActions.stream()).collect(Collectors.toList());
        builder.setActions = Stream.concat(expression1.setActions.stream(),
                                           expression2.setActions.stream()).collect(Collectors.toList());
        builder.deleteActions = Stream.concat(expression1.deleteActions.stream(),
                                              expression2.deleteActions.stream()).collect(Collectors.toList());
        builder.addActions = Stream.concat(expression1.addActions.stream(),
                                           expression2.addActions.stream()).collect(Collectors.toList());
        return builder.build();
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

    public boolean isEmpty() {
        return removeActions().isEmpty() && setActions().isEmpty() && deleteActions.isEmpty() && addActions.isEmpty();
    }

    /**
     * A builder for {@link UpdateExpression}
     */
    public static final class Builder {

        private List<RemoveAction> removeActions = new ArrayList<>();
        private List<SetAction> setActions = new ArrayList<>();
        private List<DeleteAction> deleteActions = new ArrayList<>();
        private List<AddAction> addActions = new ArrayList<>();

        private Builder() {
        }

        /**
         * Add an action of type {@link RemoveAction}
         */
        public Builder addAction(RemoveAction action) {
            removeActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link SetAction}
         */
        public Builder addAction(SetAction action) {
            setActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link DeleteAction}
         */
        public Builder addAction(DeleteAction action) {
            deleteActions.add(action);
            return this;
        }

        /**
         * Add an action of type {@link AddAction}
         */
        public Builder addAction(AddAction action) {
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
            if (action instanceof RemoveAction) {
                addAction((RemoveAction) action);
            } else if (action instanceof SetAction) {
                addAction((SetAction) action);
            } else if (action instanceof DeleteAction) {
                addAction((DeleteAction) action);
            } else if (action instanceof AddAction) {
                addAction((AddAction) action);
            } else {
                throw new IllegalArgumentException(
                    String.format("Do not recognize UpdateAction: %s", action.getClass()));
            }
        }
    }
}
