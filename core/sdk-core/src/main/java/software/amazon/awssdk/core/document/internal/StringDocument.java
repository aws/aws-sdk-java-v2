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
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
@Immutable
public final class StringDocument implements Document {

    private static final long serialVersionUID = 1L;

    private final String value;

    /**
     * Create a New {@link StringDocument} with boolean value as passed in constructor
     * @param string boolean value.
     */
    public StringDocument(String string) {
        Validate.notNull(string, "String cannot be null");
        this.value = string;
    }

    /**
     * Unwraps the Document Boolean to a String Object.
     * @return  string value.
     */
    @Override
    public Object unwrap() {
        return value;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A Document String cannot be converted to a Boolean.");
    }

    /**
     * @return true, since this is a Document String.
     */
    @Override
    public boolean isString() {
        return true;
    }

    /**
     * Gets the String value of the Document.
     * @return string value.
     */
    @Override
    public String asString() {
        return value;
    }

    /**
     * @return true, since this is a Document String.
     */
    @Override
    public SdkNumber asNumber() {
        throw new UnsupportedOperationException("A Document String cannot be converted to a Number.");
    }

    /**
     * @return true, since this is a Document String.
     */
    @Override
    public Map<String, Document> asMap() {
        throw new UnsupportedOperationException("A Document String cannot be converted to a Map.");
    }

    /**
     * @return true, since this is a Document String.
     */
    @Override
    public List<Document> asList() {
        throw new UnsupportedOperationException("A Document String cannot be converted to a List.");
    }

    /**
     * Accepts a visitor with the Document.
     * @param <R> visitor return type.
     * @param visitor Visitor to dispatch to.
     * @return Returns the accepted result by calling visitString of visitor.
     */
    @Override
    public <R> R accept(DocumentVisitor<? extends R> visitor) {
        return visitor.visitString(asString());
    }

    /**
     * Accepts a visitor with the Document. Calls visitString of visitor.
     * @param visitor Visitor to dispatch to.
     */
    @Override
    public void accept(VoidDocumentVisitor visitor) {
        visitor.visitString(asString());
    }


    @Override
    public String toString() {
        // Does not handle unicode control characters
        return "\"" +
                value.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                + "\"";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StringDocument)) {
            return false;
        }
        StringDocument that = (StringDocument) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
