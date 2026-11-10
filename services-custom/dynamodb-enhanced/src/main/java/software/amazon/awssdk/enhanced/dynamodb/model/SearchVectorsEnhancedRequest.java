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

package software.amazon.awssdk.enhanced.dynamodb.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.NestedAttributeName;
import software.amazon.awssdk.services.dynamodb.model.ReturnConsumedCapacity;
import software.amazon.awssdk.utils.Validate;

/**
 * Defines parameters used when performing a vector search via {@code table.vectorIndex(name).searchVectorsWithResponse(...)}.
 * <p>
 * The vector index name is bound on the vector index handle and is not part of this request. All parameters are optional; the
 * service validates required fields such as {@link #searchVector()}.
 */
@SdkPublicApi
@ThreadSafe
public final class SearchVectorsEnhancedRequest {
    private final float[] searchVector;
    private final Integer topK;
    private final Expression searchConditionExpression;
    private final List<NestedAttributeName> attributesToProject;
    private final String returnConsumedCapacity;

    private SearchVectorsEnhancedRequest(Builder builder) {
        this.searchVector = builder.searchVector == null ? null : builder.searchVector.clone();
        this.topK = builder.topK;
        this.searchConditionExpression = builder.searchConditionExpression;
        this.returnConsumedCapacity = builder.returnConsumedCapacity;
        if (builder.attributesToProject == null) {
            this.attributesToProject = null;
        } else {
            this.attributesToProject = Collections.unmodifiableList(new ArrayList<>(builder.attributesToProject));
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return builder().searchVector(searchVector)
                        .topK(topK)
                        .searchConditionExpression(searchConditionExpression)
                        .returnConsumedCapacity(returnConsumedCapacity)
                        .addNestedAttributesToProject(attributesToProject);
    }

    /**
     * The query embedding used for similarity search. Must match the dimensionality of the target vector index.
     */
    public float[] searchVector() {
        return searchVector == null ? null : searchVector.clone();
    }

    /**
     * The maximum number of similar items to return.
     */
    public Integer topK() {
        return topK;
    }

    /**
     * Filter expression applied together with vector similarity, including the required HASH attribute condition when configured
     * on the index.
     */
    public Expression searchConditionExpression() {
        return searchConditionExpression;
    }

    /**
     * Returns the list of projected attributes on this request object, or null if no projection is specified. Nested attributes
     * are represented using the '.' separator. Example : foo.bar is represented as "foo.bar" which is indistinguishable from a
     * non-nested attribute with the name "foo.bar". Use {@link #nestedAttributesToProject} if you have a use-case that requires
     * discrimination between these two cases.
     */
    public List<String> attributesToProject() {
        return attributesToProject != null ? attributesToProject.stream()
                                                                .map(item -> String.join(".", item.elements()))
                                                                .collect(Collectors.toList()) : null;
    }

    /**
     * Returns the list of projected attribute names, in the form of {@link NestedAttributeName} objects, for this request object,
     * or null if no projection is specified.
     *
     * @see NestedAttributeName
     */
    public List<NestedAttributeName> nestedAttributesToProject() {
        return attributesToProject;
    }

    /**
     * Whether to return the capacity consumed by this operation.
     *
     * @see software.amazon.awssdk.services.dynamodb.model.SearchVectorsRequest#returnConsumedCapacity()
     */
    public ReturnConsumedCapacity returnConsumedCapacity() {
        return ReturnConsumedCapacity.fromValue(returnConsumedCapacity);
    }

    /**
     * Whether to return the capacity consumed by this operation.
     * <p>
     * Similar to {@link #returnConsumedCapacity()} but return the value as a string. This is useful in situations where the value
     * is not defined in {@link ReturnConsumedCapacity}.
     */
    public String returnConsumedCapacityAsString() {
        return returnConsumedCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchVectorsEnhancedRequest that = (SearchVectorsEnhancedRequest) o;
        return Arrays.equals(searchVector, that.searchVector)
               && Objects.equals(topK, that.topK)
               && Objects.equals(searchConditionExpression, that.searchConditionExpression)
               && Objects.equals(attributesToProject, that.attributesToProject)
               && Objects.equals(returnConsumedCapacity, that.returnConsumedCapacity);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(searchVector);
        result = 31 * result + Objects.hashCode(topK);
        result = 31 * result + Objects.hashCode(searchConditionExpression);
        result = 31 * result + Objects.hashCode(attributesToProject);
        result = 31 * result + Objects.hashCode(returnConsumedCapacity);
        return result;
    }

    /**
     * A builder for {@link SearchVectorsEnhancedRequest}.
     */
    @NotThreadSafe
    public static final class Builder {
        private float[] searchVector;
        private Integer topK;
        private Expression searchConditionExpression;
        private List<NestedAttributeName> attributesToProject;
        private String returnConsumedCapacity;

        private Builder() {
        }

        /**
         * Sets the query embedding as a {@code float[]} (canonical representation).
         */
        public Builder searchVector(float[] searchVector) {
            this.searchVector = searchVector;
            return this;
        }

        /**
         * Sets the query embedding from a list of {@link Float} values.
         */
        public Builder searchVector(List<Float> searchVector) {
            if (searchVector == null) {
                this.searchVector = null;
                return this;
            }
            float[] values = new float[searchVector.size()];
            for (int i = 0; i < searchVector.size(); i++) {
                values[i] = searchVector.get(i);
            }
            this.searchVector = values;
            return this;
        }

        public Builder topK(Integer topK) {
            this.topK = topK;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder searchConditionExpression(Expression searchConditionExpression) {
            this.searchConditionExpression = searchConditionExpression;
            return this;
        }

        /**
         * Sets a collection of attribute names to be retrieved from the database. These attributes can include scalars, sets, or
         * elements of a JSON document.
         * <p>
         * If no attribute names are specified, then all attributes will be returned. If any of the requested attributes are not
         * found, they will not appear in the result.
         * <p>If there are nested attributes, use any of the addNestedAttributesToProject methods, such as
         * {@link #addNestedAttributesToProject(NestedAttributeName...)}.
         *
         * @param attributesToProject A collection of the attributes names to be retrieved from the database.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder attributesToProject(Collection<String> attributesToProject) {
            if (this.attributesToProject != null) {
                this.attributesToProject.clear();
            }
            if (attributesToProject != null) {
                addNestedAttributesToProject(new ArrayList<>(attributesToProject)
                                                 .stream()
                                                 .map(NestedAttributeName::create)
                                                 .collect(Collectors.toList()));
            }
            return this;
        }

        /**
         * Sets one or more attribute names to be retrieved from the database. These attributes can include scalars, sets, or
         * elements of a JSON document.
         * <p>
         * If no attribute names are specified, then all attributes will be returned. If any of the requested attributes are not
         * found, they will not appear in the result.
         * <p>If there are nested attributes, use any of the addNestedAttributesToProject methods, such as
         * {@link #addNestedAttributesToProject(NestedAttributeName...)}.
         *
         * @param attributesToProject One or more attributes names to be retrieved from the database.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder attributesToProject(String... attributesToProject) {
            return attributesToProject(Arrays.asList(attributesToProject));
        }

        /**
         * Adds a single attribute name to be retrieved from the database. The attribute can include scalars, sets, or elements of
         * a JSON document.
         * <p>If there are nested attributes, use any of the addNestedAttributesToProject methods, such as
         * {@link #addNestedAttributesToProject(NestedAttributeName...)}.
         *
         * @param attributeToProject An additional single attribute name to be retrieved from the database.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder addAttributeToProject(String attributeToProject) {
            if (attributeToProject != null) {
                addNestedAttributesToProject(NestedAttributeName.create(attributeToProject));
            }
            return this;
        }

        public Builder addNestedAttributeToProject(NestedAttributeName nestedAttributeName) {
            if (nestedAttributeName != null) {
                addNestedAttributesToProject(Arrays.asList(nestedAttributeName));
            }
            return this;
        }

        public Builder addNestedAttributesToProject(NestedAttributeName... nestedAttributeNames) {
            return addNestedAttributesToProject(Arrays.asList(nestedAttributeNames));
        }

        public Builder addNestedAttributesToProject(Collection<NestedAttributeName> nestedAttributeNames) {
            if (nestedAttributeNames != null) {
                Validate.noNullElements(nestedAttributeNames,
                                        "nestedAttributeNames list must not contain null elements");
                if (attributesToProject == null) {
                    this.attributesToProject = new ArrayList<>(nestedAttributeNames);
                } else {
                    this.attributesToProject.addAll(nestedAttributeNames);
                }
            }
            return this;
        }

        public Builder returnConsumedCapacity(ReturnConsumedCapacity returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity == null ? null : returnConsumedCapacity.toString();
            return this;
        }

        public Builder returnConsumedCapacity(String returnConsumedCapacity) {
            this.returnConsumedCapacity = returnConsumedCapacity;
            return this;
        }

        public SearchVectorsEnhancedRequest build() {
            return new SearchVectorsEnhancedRequest(this);
        }
    }
}
