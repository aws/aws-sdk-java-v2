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

package software.amazon.awssdk.core.document.internal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.document.DocumentVisitor;
import software.amazon.awssdk.core.document.VoidDocumentVisitor;

/**
 * Represents a Boolean Document.
 */
@SdkInternalApi
@Immutable
public final class BooleanDocument implements Document {

    private static final long serialVersionUID = 1L;

    private final boolean value;

    /**
     * Create a New {@link BooleanDocument} with boolean value as passed in constructor
     * @param value boolean value.
     */
    public BooleanDocument(boolean value) {
        this.value = value;
    }

    /**
     * Unwraps the Document Boolean to a Boolean Object.
     * @return  boolean value.
     */
    @Override
    public Object unwrap() {
        return value;
    }

    /**
     * Indicates this is a Boolean Document.
     * @return true since this is a Boolean Document.
     */
    @Override
    public boolean isBoolean() {
        return true;
    }

    /**
     * Gets the boolean value of the Document.
     * @return boolean value.
     */
    @Override
    public boolean asBoolean() {
        return value;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public String asString() {
        throw new UnsupportedOperationException("A Document Boolean cannot be converted to a String.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public SdkNumber asNumber() {
        throw new UnsupportedOperationException("A Document Boolean cannot be converted to a Number.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public Map<String, Document> asMap() {
        throw new UnsupportedOperationException("A Document Boolean cannot be converted to a Map.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public List<Document> asList() {
        throw new UnsupportedOperationException("A Document Boolean cannot be converted to a List.");
    }


    /**
     * Accepts a visitor with the Document.
     * @param <R> visitor return type.
     * @param visitor Visitor to dispatch to.
     * @return Returns the accepted result by calling visitBoolean of visitor.
     */
    @Override
    public <R> R accept(DocumentVisitor<? extends R> visitor) {
        return visitor.visitBoolean(asBoolean());
    }

    /**
     * Accepts a visitor with the Document. Calls visitBoolean of visitor.
     * @param visitor Visitor to dispatch to.
     */
    @Override
    public void accept(VoidDocumentVisitor visitor) {
        visitor.visitBoolean(asBoolean());
    }

    @Override
    public String toString() {
        return Boolean.toString(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BooleanDocument)) {
            return false;
        }
        BooleanDocument that = (BooleanDocument) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}