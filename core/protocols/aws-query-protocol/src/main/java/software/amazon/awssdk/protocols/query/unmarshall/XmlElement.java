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

package software.amazon.awssdk.protocols.query.unmarshall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Represents an element in an XML document.
 */
@SdkProtectedApi
public final class XmlElement {

    private static final XmlElement EMPTY = XmlElement.builder().elementName("eof").build();

    private final String elementName;
    private final HashMap<String, List<XmlElement>> childrenByElement;
    private final List<XmlElement> children;
    private final String textContent;
    private final Map<String, String> attributes;

    private XmlElement(Builder builder) {
        this.elementName = builder.elementName;
        this.childrenByElement = new HashMap<>(builder.childrenByElement);
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
        this.textContent = builder.textContent;
        this.attributes = Collections.unmodifiableMap(new HashMap<>(builder.attributes));
    }

    /**
     * @return Tag name of the element.
     */
    public String elementName() {
        return elementName;
    }

    /**
     * @return The list of direct children of this element. May be empty.
     */
    public List<XmlElement> children() {
        return children;
    }

    /**
     * @return The first child element of this element. Null if this element has no children.
     */
    public XmlElement getFirstChild() {
        return children.isEmpty() ? null : children.get(0);
    }

    /**
     * Get all child elements by the given tag name. This only returns direct children elements.
     *
     * @param tagName Tag name of elements to retrieve.
     * @return List of elements or empty list of no elements found with given name.
     */
    public List<XmlElement> getElementsByName(String tagName) {
        return childrenByElement.getOrDefault(tagName, Collections.emptyList());
    }

    /**
     * Retrieves a single child element by tag name. If more than one element is found then this method will throw an exception.
     *
     * @param tagName Tag name of element to get.
     * @return XmlElement with the matching tag name or null if no element exists.
     * @throws SdkClientException If more than one element with the given tag name is found.
     */
    public XmlElement getElementByName(String tagName) {
        List<XmlElement> elementsByName = getElementsByName(tagName);
        if (elementsByName.size() > 1) {
            throw SdkClientException.create(
                String.format("Did not expect more than one element with the name %s in the XML event %s",
                              tagName, this.elementName));
        }
        return elementsByName.size() == 1 ? elementsByName.get(0) : null;
    }

    /**
     * Retrieves a single child element by tag name. If more than one element is found then this method will throw an exception.
     *
     * @param tagName Tag name of element to get.
     * @return Fulfilled {@link Optional} of XmlElement with the matching tag name or empty {@link Optional} if no element exists.
     * @throws SdkClientException If more than one element with the given tag name is found.
     */
    public Optional<XmlElement> getOptionalElementByName(String tagName) {
        return Optional.ofNullable(getElementByName(tagName));
    }

    /**
     * @return Text content of this element.
     */
    public String textContent() {
        return textContent;
    }

    /**
     * Retrieves an optional attribute by attribute name.
     */
    public Optional<String> getOptionalAttributeByName(String attribute) {
        return Optional.ofNullable(attributes.get(attribute));
    }

    /**
     * Retrieves the attributes associated with the element
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * @return New {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return An empty {@link XmlElement} (<eof/>).
     */
    public static XmlElement empty() {
        return EMPTY;
    }

    /**
     * Builder for {@link XmlElement}.
     */
    public static final class Builder {

        private String elementName;
        private final Map<String, List<XmlElement>> childrenByElement = new HashMap<>();
        private final List<XmlElement> children = new ArrayList<>();
        private String textContent = "";
        private Map<String, String> attributes = new HashMap<>();

        private Builder() {
        }

        public Builder elementName(String elementName) {
            this.elementName = elementName;
            return this;
        }

        public Builder addChildElement(XmlElement childElement) {
            this.childrenByElement.computeIfAbsent(childElement.elementName(), s -> new ArrayList<>());
            this.childrenByElement.get(childElement.elementName()).add(childElement);
            this.children.add(childElement);
            return this;
        }

        public Builder textContent(String textContent) {
            this.textContent = textContent;
            return this;
        }

        public Builder attributes(Map<String, String> attributes) {
            this.attributes = attributes;
            return this;
        }

        public XmlElement build() {
            return new XmlElement(this);
        }
    }

}
