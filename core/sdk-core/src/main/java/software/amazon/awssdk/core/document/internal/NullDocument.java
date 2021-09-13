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
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.document.DocumentVisitor;
import software.amazon.awssdk.core.document.VoidDocumentVisitor;

@SdkInternalApi
@Immutable
public final class NullDocument implements Document {

    private static final long serialVersionUID = 1L;

    /**
     * Unwraps NullDocument as null.
     * @return null
     */
    @Override
    public Object unwrap() {
        return null;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A Document Null cannot be converted to a Boolean.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public String asString() {
        throw new UnsupportedOperationException("A Document Null cannot be converted to a String.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public SdkNumber asNumber() {
        throw new UnsupportedOperationException("A Document Null cannot be converted to a Number.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public Map<String, Document> asMap() {
        throw new UnsupportedOperationException("A Document Null cannot be converted to a Map.");
    }

    /**
     * @return true ,since this is a Document Null.
     */
    @Override
    public boolean isNull() {
        return true;
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public List<Document> asList() {
        throw new UnsupportedOperationException("A Document Null cannot be converted to a List.");
    }

    /**
     * Accepts a visitor with the Document.
     * @param <R> visitor return type.
     * @param visitor Visitor to dispatch to.
     * @return Returns the accepted result by calling visitNull of visitor.
     */
    @Override
    public <R> R accept(DocumentVisitor<? extends R> visitor) {
        return visitor.visitNull();
    }

    /**
     * Accepts a visitor with the Document. Calls visitNull of visitor.
     * @param visitor Visitor to dispatch to.
     */
    @Override
    public void accept(VoidDocumentVisitor visitor) {
        visitor.visitNull();
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NullDocument)) {
            return false;
        }
        NullDocument that = (NullDocument) obj;
        return that.isNull() == this.isNull();
    }
}
