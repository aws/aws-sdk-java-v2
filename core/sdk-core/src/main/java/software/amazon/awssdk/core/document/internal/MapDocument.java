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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.core.document.DocumentVisitor;
import software.amazon.awssdk.core.document.VoidDocumentVisitor;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
@Immutable
public final class MapDocument implements Document {

    private static final long serialVersionUID = 1L;

    private final Map<String, Document> value;

    /**
     * Create a New {@link MapDocument} with Map of Document value as passed in constructor
     * @param documentMap ListDocument documentList.
     */
    public MapDocument(Map<String, Document> documentMap) {
        Validate.notNull(documentMap, "Map cannot be null");
        this.value =  Collections.unmodifiableMap(documentMap);
    }

    /**
     * Create a {@link MapBuilderInternal} for generating a
     * {@link Document} by directly allowing user to put String Keys
     * and Document Values in the builder methods.
     * @return Builder to Construct Document with Map of Documents.
     */
    public static MapBuilder mapBuilder() {
        return new MapBuilderInternal();
    }

    /**
     * Gets the value of the document as a Java type that represents the
     * Loops through the individual Map Entries and unwarap each of the Document Value.
     * @return Returns the Map with Keys as String and  Values as {@code Map<Object>}.
     */
    @Override
    public Object unwrap() {
        Map<String, Object> unwrappedMap = new LinkedHashMap<>();
        value.entrySet().forEach(mapEntry -> unwrappedMap.put(mapEntry.getKey(), mapEntry.getValue().unwrap()));
        return Collections.unmodifiableMap(unwrappedMap);
    }

    /**
     * @return UnsupportedOperationException
     */
    @Override
    public boolean asBoolean() {
        throw new UnsupportedOperationException("A Document Map cannot be converted to a Boolean.");
    }

    /**
     * @return UnsupportedOperationException
     */
    @Override
    public String asString() {
        throw new UnsupportedOperationException("A Document Map cannot be converted to a String.");
    }

    /**
     * @return UnsupportedOperationException
     */
    @Override
    public SdkNumber asNumber() {
        throw new UnsupportedOperationException("A Document Map cannot be converted to a Number.");
    }

    /**
     * @return UnsupportedOperationException
     */
    @Override
    public boolean isMap() {
        return true;
    }

    /**
     *
     * @return unmodifiableMap of the Map of Documents in the {{@link MapDocument}}.
     */
    @Override
    public Map<String, Document> asMap() {
        return value;
    }

    /**
     * @return UnsupportedOperationException
     */
    @Override
    public List<Document> asList() {
        throw new UnsupportedOperationException("A Document Map cannot be converted to a List.");
    }

    /**
     * Accepts a visitor with the Document.
     * @param <R> visitor return type.
     * @param visitor Visitor to dispatch to.
     * @return Returns the accepted result by calling visitMap of visitor.
     */
    @Override
    public <R> R accept(DocumentVisitor<? extends R> visitor) {
        return visitor.visitMap(Collections.unmodifiableMap(this.asMap()));
    }

    /**
     * Accepts a visitor with the Document. Calls visitMap of visitor.
     * @param visitor Visitor to dispatch to.
     */
    @Override
    public void accept(VoidDocumentVisitor visitor) {
        visitor.visitMap(this.asMap());
    }

    public static class MapBuilderInternal implements MapBuilder {
        private final Map<String, Document> documentMap = new LinkedHashMap<>();

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putString(String key, String stringValue) {
            documentMap.put(key, Document.fromString(stringValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, SdkNumber numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, int numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, long numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, double numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, float numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, BigDecimal numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, BigInteger numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNumber(String key, String numberValue) {
            documentMap.put(key, Document.fromNumber(numberValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putBoolean(String key, boolean booleanValue) {
            documentMap.put(key, Document.fromBoolean(booleanValue));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putDocument(String key, Document document) {
            Validate.notNull(document, "Document cannot be null");
            documentMap.put(key, document);
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putNull(String key) {
            documentMap.put(key, Document.fromNull());
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putList(String key, List<Document> documentList) {
            documentMap.put(key, Document.fromList(documentList));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putList(String key, Consumer<ListBuilder> listBuilderConsumer) {
            ListBuilder listBuilder = ListDocument.listBuilder();
            listBuilderConsumer.accept(listBuilder);
            documentMap.put(key, listBuilder.build());
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putMap(String key, Map<String, Document> documentMap) {
            Validate.notNull(documentMap, "documentMap cannot be null");
            this.documentMap.put(key, Document.fromMap(documentMap));
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public MapBuilder putMap(String key, Consumer<MapBuilder> mapBuilderConsumer) {
            MapBuilder mapBuilder = MapDocument.mapBuilder();
            mapBuilderConsumer.accept(mapBuilder);
            this.documentMap.put(key, mapBuilder.build());
            return this;
        }

        /**
         * {@inheritdoc}
         */
        @Override
        public Document build() {
            return new MapDocument(documentMap);
        }
    }

    @Override
    public String toString() {
        if (value.isEmpty()) {
            return "{}";
        }
        StringBuilder output = new StringBuilder();
        output.append("{");
        value.forEach((k, v) -> output.append("\"").append(k).append("\": ")
                .append(v.toString()).append(","));
        output.setCharAt(output.length() - 1, '}');
        return output.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MapDocument)) {
            return false;
        }
        MapDocument that = (MapDocument) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
