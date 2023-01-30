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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A representation of a single {@link UpdateExpression} REMOVE action.
 * <p>
 * At a minimum, this action must contain a path string referencing the attribute that should be removed when applying this
 * action. Consult the DynamoDB UpdateExpression documentation for details on this action.
 * <p>
 * Optionally, attribute names can be substituted with tokens using the '#name_token' syntax. If tokens are used in the
 * expression then the names associated with those tokens must be explicitly added to the expressionNames map
 * that is also stored on this object.
 * <p>
 * Example:-
 * <pre>
 * {@code
 * RemoveUpdateAction removeAction = RemoveUpdateAction.builder()
 *                                                     .path("#a")
 *                                                     .putExpressionName("#a", "attributeA")
 *                                                     .build();
 * }
 * </pre>
 */
@SdkPublicApi
public final class RemoveAction implements UpdateAction, ToCopyableBuilder<RemoveAction.Builder, RemoveAction> {

    private final String path;
    private final Map<String, String> expressionNames;

    private RemoveAction(Builder builder) {
        this.path = Validate.paramNotNull(builder.path, "path");
        this.expressionNames = wrapSecure(builder.expressionNames != null ? builder.expressionNames : new HashMap<>());
    }

    private static <T, U> Map<T, U> wrapSecure(Map<T, U> map) {
        return Collections.unmodifiableMap(new HashMap<>(map));
    }

    /**
     * Constructs a new builder for {@link RemoveAction}.
     *
     * @return a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().path(path)
                        .expressionNames(expressionNames);
    }

    public String path() {
        return path;
    }

    public Map<String, String> expressionNames() {
        return expressionNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RemoveAction that = (RemoveAction) o;

        if (path != null ? ! path.equals(that.path) : that.path != null) {
            return false;
        }
        return expressionNames != null ? expressionNames.equals(that.expressionNames) : that.expressionNames == null;
    }

    @Override
    public int hashCode() {
        int result = path != null ? path.hashCode() : 0;
        result = 31 * result + (expressionNames != null ? expressionNames.hashCode() : 0);
        return result;
    }

    /**
     * A builder for {@link RemoveAction}
     */
    public static final class Builder implements CopyableBuilder<Builder, RemoveAction> {

        private String path;
        private Map<String, String> expressionNames;

        private Builder() {
        }

        /**
         * A string expression representing the attribute to remove
         */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /**
         * Sets the optional 'expression names' token map, overriding any existing values. Use if the attribute
         * references in the path expression are token ('expression attribute names') prepended with the
         * '#' (pound) sign. It should map from token name to real attribute name.
         *
         * @see #putExpressionName(String, String)
         */
        public Builder expressionNames(Map<String, String> expressionNames) {
            this.expressionNames = expressionNames == null ? null : new HashMap<>(expressionNames);
            return this;
        }

        /**
         * Adds a single element to the optional 'expression names' token map.
         *
         * @see #expressionNames(Map)
         */
        public Builder putExpressionName(String key, String value) {
            if (this.expressionNames == null) {
                this.expressionNames = new HashMap<>();
            }
            this.expressionNames.put(key, value);
            return this;
        }

        /**
         * Builds an {@link RemoveAction} based on the values stored in this builder.
         */
        public RemoveAction build() {
            return new RemoveAction(this);
        }
    }
}
