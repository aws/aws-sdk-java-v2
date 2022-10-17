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
public class NumberDocument implements Document {

    private static final long serialVersionUID = 1L;

    private final SdkNumber number;

    /**
     * Created a {{@link NumberDocument}} with the specified {{@link SdkNumber}}.
     * {{@link SdkNumber}} is provided as an input to NumberDocument to maintain arbitrary precision of any given number.
     * @param number
     */
    public NumberDocument(SdkNumber number) {
        Validate.notNull(number, "Number cannot be null.");
        this.number = number;
    }

    /**
     * Unwraps the Document Number to string value of the {{@link SdkNumber}}.
     * @return  {{@link SdkNumber}} value.
     */
    @Override
    public Object unwrap() {
        return number.stringValue();
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A Document Number cannot be converted to a Boolean.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public String asString() {
        throw new UnsupportedOperationException("A Document Number cannot be converted to a String.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public boolean isNumber() {
        return true;
    }

    /**
     * Returned as {{@link SdkNumber}}.
     * The number value can be extracted from the Document by using below methods
     * @see SdkNumber#intValue()
     * @see SdkNumber#longValue() ()
     * @see SdkNumber#floatValue() ()
     * @see SdkNumber#shortValue() ()
     * @see SdkNumber#bigDecimalValue() ()
     * @see SdkNumber#stringValue()
     * @see SdkNumber#floatValue() ()
     * @see SdkNumber#doubleValue() () ()
     * @return {{@link SdkNumber}} value.
     */
    @Override
    public SdkNumber asNumber() {
        return number;
    }

    @Override
    public Map<String, Document> asMap() {
        throw new UnsupportedOperationException("A Document Number cannot be converted to a Map.");
    }

    /**
     * {@inheritdoc}
     */
    @Override
    public List<Document> asList() {
        throw new UnsupportedOperationException("A Document Number cannot be converted to a List.");
    }

    /**
     * Accepts a visitor with the Document.
     * @param <R> visitor return type.
     * @param visitor Visitor to dispatch to.
     * @return Returns the accepted result by calling visitNumber of visitor.
     */
    @Override
    public <R> R accept(DocumentVisitor<? extends R> visitor) {
        return visitor.visitNumber(this.asNumber());
    }

    /**
     * Accepts a visitor with the Document. Calls visitNumber of visitor.
     * @param visitor Visitor to dispatch to.
     */
    @Override
    public void accept(VoidDocumentVisitor visitor) {
        visitor.visitNumber(this.asNumber());
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NumberDocument)) {
            return false;
        }
        NumberDocument that = (NumberDocument) o;
        return Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(number);
    }
}
