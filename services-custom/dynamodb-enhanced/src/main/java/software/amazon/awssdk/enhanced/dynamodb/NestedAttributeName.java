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

package software.amazon.awssdk.enhanced.dynamodb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * High-level representation of a DynamoDB 'NestedAttributeName' that can be used in various situations where the API requires
 * or accepts an Nested Attribute Name.
 * Simple Attribute Name can be represented by passing just the name of the attribute.
 * Nested Attributes are represented by List of String where each index of list corresponds to Nesting level Names.
 * <p> While using attributeToProject in {@link software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest}
 * and {@link software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest} we need  way to represent Nested Attributes.
 * The normal DOT(.) separator is not recognized as a Nesting level separator by DynamoDB request,
 * thus we need to use NestedAttributeName
 * which can be used to represent Nested attributes.
 * <p> Example : NestedAttributeName.create("foo") corresponds to a NestedAttributeName with elements list
 * with single element foo which represents Simple attribute name "foo" without nesting.
 * <p>NestedAttributeName.create("foo", "bar") corresponds to a NestedAttributeName with elements list "foo", "bar"
 * respresenting nested attribute name "foo.bar".
 */
@SdkPublicApi
public final class NestedAttributeName {

    private final List<String> elements;

    private NestedAttributeName(List<String> nestedAttributeNames) {
        Validate.validState(nestedAttributeNames != null, "nestedAttributeNames must not be null.");
        Validate.notEmpty(nestedAttributeNames, "nestedAttributeNames must not be empty");
        Validate.noNullElements(nestedAttributeNames, "nestedAttributeNames must not contain null values");
        this.elements =  Collections.unmodifiableList(nestedAttributeNames);
    }

    /**
     * Creates a NestedAttributeName with a single element, which is effectively just a simple attribute name without nesting.
     * <p>
     * <b>Example:</b>create("foo") will create NestedAttributeName corresponding to Attribute foo.
     *
     * @param element Attribute Name. Single String represents just a simple attribute name without nesting.
     * @return NestedAttributeName with attribute name as specified element.
     */
    public static NestedAttributeName create(String element) {
        return new Builder().addElement(element).build();
    }

    /**
     * Creates a NestedAttributeName from a list of elements that compose the full path of the nested attribute.
     * <p>
     * <b>Example:</b>create("foo", "bar") will create NestedAttributeName which represents foo.bar nested attribute.
     *
     * @param elements Nested Attribute Names. Each of strings in varargs represent the nested attribute name
     *                 at subsequent levels.
     * @return NestedAttributeName with Nested attribute name set as specified in elements var args.
     */
    public static NestedAttributeName create(String... elements) {
        return new Builder().elements(elements).build();
    }

    /**
     * Creates a NestedAttributeName from a list of elements that compose the full path of the nested attribute.
     * <p>
     * <b>Example:</b>create(Arrays.asList("foo", "bar")) will create NestedAttributeName
     * which represents foo.bar nested attribute.
     *
     * @param elements List of Nested Attribute Names. Each of strings in List represent the nested attribute name
     *                 at subsequent levels.
     * @return NestedAttributeName with Nested attribute name set as specified in elements Collections.
     */
    public static NestedAttributeName create(List<String> elements) {
        return new Builder().elements(elements).build();
    }

    /**
     * Create a builder that can be used to create a {@link NestedAttributeName}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets elements of NestedAttributeName in the form of List. Each element in the list corresponds
     * to the subsequent Nested Attribute name.
     *
     * @return List of nested attributes, each entry in the list represent one level of nesting.
     * Example, A Two level Attribute name foo.bar will be represented as  ["foo", "bar"]
     */
    public List<String> elements() {
        return elements;
    }

    /**
     * Returns a builder initialized with all existing values on the request object.
     */
    public Builder toBuilder() {
        return builder().elements(elements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NestedAttributeName that = (NestedAttributeName) o;

        return elements != null
                ? elements.equals(that.elements) : that.elements == null;
    }

    @Override
    public int hashCode() {
        return elements != null ? elements.hashCode() : 0;
    }

    /**
     * A builder for {@link NestedAttributeName}.
     */
    public static class Builder {
        private List<String> elements = null;

        private Builder() {

        }

        /**
         * Adds a single element of NestedAttributeName.
         * Subsequent calls to this method can add attribute Names at subsequent nesting levels.
         * <p>
         * <b>Example:</b>builder().addElement("foo").addElement("bar")  will add elements in NestedAttributeName
         * which represent a Nested Attribute Name foo.bar
         *
         * @param element Attribute Name.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder addElement(String element) {
            if (elements == null) {
                elements = new ArrayList<>();
            }
            elements.add(element);
            return this;
        }

        /**
         * Adds a single element of NestedAttributeName.
         * Subsequent calls to this method will append the new elements to the end of the existing chain of elements
         * creating new levels of nesting.
         * <p>
         * <b>Example:</b>builder().addElements("foo","bar")  will add elements in NestedAttributeName
         * which represent a Nested Attribute Name foo.bar
         *
         * @param elements Nested Attribute Names. Each of strings in varargs represent the nested attribute name
         *                 at subsequent levels.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder addElements(String... elements) {
            if (this.elements == null) {
                this.elements = new ArrayList<>();
            }
            this.elements.addAll(Arrays.asList(elements));
            return this;
        }

        /**
         * Adds a List of elements to NestedAttributeName.
         * Subsequent calls to this method will append the new elements to the end of the existing chain of elements
         * creating new levels of nesting.
         * <p>
         * <b>Example:</b>builder().addElements(Arrays.asList("foo","bar"))  will add elements in NestedAttributeName
         * to represent a Nested Attribute Name foo.bar
         *
         * @param elements List of Strings where each string corresponds to subsequent nesting attribute name.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder addElements(List<String> elements) {
            if (this.elements == null) {
                this.elements = new ArrayList<>();
            }
            this.elements.addAll(elements);
            return this;
        }

        /**
         * Set elements of NestedAttributeName with list of Strings. Will overwrite any existing elements stored by this builder.
         * <p>
         * <b>Example:</b>builder().elements("foo","bar") will set the elements in NestedAttributeName
         * to represent a nested attribute name of 'foo.bar'
         *
         * @param elements a list of strings that correspond to the elements in a nested attribute name.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder elements(String... elements) {
            this.elements = new ArrayList<>(Arrays.asList(elements));
            return this;
        }

        /**
         * Sets the elements that compose a nested attribute name. Will overwrite any existing elements stored by this builder.
         * <p>
         * <b>Example:</b>builder().elements(Arrays.asList("foo","bar")) will add elements in NestedAttributeName
         * which represent a Nested Attribute Name foo.bar
         *
         * @param elements a list of strings that correspond to the elements in a nested attribute name.
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        public Builder elements(List<String> elements) {
            this.elements = new ArrayList<>(elements);
            return this;
        }


        public NestedAttributeName build() {
            return new NestedAttributeName(elements);
        }
    }
}
