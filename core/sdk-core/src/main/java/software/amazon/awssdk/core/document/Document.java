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


package software.amazon.awssdk.core.document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.internal.BooleanDocument;
import software.amazon.awssdk.core.document.internal.ListDocument;
import software.amazon.awssdk.core.document.internal.MapDocument;
import software.amazon.awssdk.core.document.internal.NullDocument;
import software.amazon.awssdk.core.document.internal.NumberDocument;
import software.amazon.awssdk.core.document.internal.StringDocument;

/**
 * Interface for Document Types.
 * Document types are used to carry open content that is Data with no fixed schema, data that can't be modeled using rigid types,
 * or data that has a schema that evolves outside the purview of a service
 * without requiring techniques like embedding JSON inside JSON strings.
 * Document type value is serialized using the same format as its surroundings and requires no additional encoding or escaping.
 * This interface specifies all the methods to access a Document, also provides constructor methods for
 * instantiating Document.
 *
 */
@SdkPublicApi
@Immutable
public interface Document extends Serializable {


    /**
     * Create {@link Document} from a string, using the provided String.
     * @param string String value.
     * @return Implementation of Document that stores a String.
     */
    static Document fromString(String string) {
        return new StringDocument(string);
    }

    /**
     * Create {@link Document} from a boolean.
     * @param booleanValue Boolean value.
     * @return Implementation of Document that stores a Boolean.
     */
    static Document fromBoolean(boolean booleanValue) {
        return new BooleanDocument(booleanValue);
    }

    /**
     * Create {@link Document} from a {@link SdkNumber}.
     * @param number {@link SdkNumber} sdkNumber with the given precision type.
     * @return Implementation of Document that stores a {@link SdkNumber}.
     */
    static Document fromNumber(SdkNumber number) {
        return new NumberDocument(number);
    }


    /**
     * Create {@link Document} from a int.
     * @param number int type number.
     * @return Implementation of Document that stores a {@link SdkNumber} constructed {@link SdkNumber#fromInteger(int)}.
     */
    static Document fromNumber(int number) {
        return new NumberDocument(SdkNumber.fromInteger(number));
    }

    /**
     * Create {@link Document} from a long.
     * @param number long type number.
     * @return Implementation of Document that stores a {@link SdkNumber} constructed {@link SdkNumber#longValue()}.
     */
    static Document fromNumber(long number) {
        return new NumberDocument(SdkNumber.fromLong(number));
    }

    /**
     * Create {@link Document} from a float.
     * @param number float type number.
     * @return Implementation of Document that stores a {@link SdkNumber} constructed {@link SdkNumber#floatValue()}
     */
    static Document fromNumber(float number) {
        return new NumberDocument(SdkNumber.fromFloat(number));
    }

    /**
     * Create {@link Document} from a double.
     * @param number double type number.
     * @return Implementation of Document that stores a {@link SdkNumber} constructed {@link SdkNumber#fromDouble(double)}
     */
    static Document fromNumber(double number) {
        return new NumberDocument(SdkNumber.fromDouble(number));
    }

    /**
     * Create {@link Document} from a BigDecimal.
     * @param number BigDecimal type number.
     * @return Implementation of Document that stores a {@link SdkNumber} constructed {@link SdkNumber#fromBigDecimal(BigDecimal)}
     */
    static Document fromNumber(BigDecimal number) {
        return new NumberDocument(SdkNumber.fromBigDecimal(number));
    }

    /**
     * Create {@link Document} from a BigInteger.
     * @param number BigInteger type number.
     * @return Implementation of Document that stores a {@link SdkNumber} constructed {@link SdkNumber#fromBigInteger(BigInteger)}
     */
    static Document fromNumber(BigInteger number) {
        return new NumberDocument(SdkNumber.fromBigInteger(number));
    }


    /**
     * Create {@link Document} from a String.
     * @param number String representation of a number.
     * @return Implementation of Document that stores a {@link SdkNumber} constructed {@link SdkNumber#fromString(String)}
     * @throws ParseException Throws ParseException when the inputString is not of Number format.
     */
    static Document fromNumber(String number) {
        return new NumberDocument(SdkNumber.fromString(number));
    }


    /**
     * Creates a Document from a Map of Documents.
     * @param documentMap Map with Keys of Type Strinb and Value of Document type.
     * @return Implementation of Document that stores a Map with String Keys and Document Values.
     */
    static Document fromMap(Map<String, Document> documentMap) {
        return new MapDocument(documentMap);
    }

    /**
     * Create a {@link ListBuilder} for generating a {@link Document} by directly allowing user add Documents.
     * @param documentList List of Documents.
     * @return Implementation of Document that stores a Lists of Documents.
     */
    static Document fromList(List<Document> documentList) {
        return new ListDocument(documentList);
    }

    /**
     * Create a {@link MapBuilder} for generating a {@link Document} by directly allowing user to put String Keys
     * and Document Values in the builder methods.
     * @return Builder to Construct Document with Map of Documents.
     */
    static MapBuilder mapBuilder() {
        return MapDocument.mapBuilder();
    }

    /**
     * Provides Builder methods of {@link ListBuilder} to directly create Document with List of Documents
     * @return Builder methods to Construct Document with List of Documents.
     */
    static ListBuilder listBuilder() {
        return ListDocument.listBuilder();
    }

    /**
     * Creates a document is a {@code null} value.
     *
     * @return Implementation of a Null Document.
     */
    static Document fromNull() {
        return new NullDocument();
    }

    /**
     * Gets the value of the document as a Java type that represents the
     * document type data model: {@code boolean}, {@code String} for Strings and Numbers,
     * {@code null}, {@code List<Object>}, or
     * {@code Map<String, Object>}.
     * @return Returns the document as one of a fixed set of Java types.
     */
    Object unwrap();

    /**
     * Checks if the document is a {@code null} value.
     * @return Returns true if the document is a {@code null} value.
     */
    default boolean isNull() {
        return false;
    }

    /**
     * @return Returns true if this document is a boolean value.
     */
    default boolean isBoolean() {
        return false;
    }

    /**
     * Gets the document as a {@code boolean} if it is a boolean.
     * @return Returns the boolean value.
     * @throws UnsupportedOperationException if the document is not a boolean.
     */
    boolean asBoolean();

    /**
     * @return Returns true if this document is a string value.
     */
    default boolean isString() {
        return false;
    }

    /**
     * Gets the document as a {@code String}.
     *
     * @return Returns the string value.
     * @throws UnsupportedOperationException if the document is not a string.
     */
    String asString();

    /**
     * @return Returns true if this document is a number value.
     */
    default boolean isNumber() {
        return false;
    }

    /**
     * Gets the document as a {@link SdkNumber} if it is a {@link SdkNumber}.
     * @return Returns the {@link SdkNumber}.
     * @throws UnsupportedOperationException if the document is not a number.
     */
    SdkNumber asNumber();

    /**
     * @return Returns true if this document is a Map.
     */
    default boolean isMap() {
        return false;
    }

    /**
     * Gets the document as a {@code Map}.
     * <p>Each value contained in the {@code Map} is the same as how the value
     * would be represented by {@link Document}.
     * @return Returns the Document map.
     * @throws UnsupportedOperationException if the document is not an Map.
     */
    Map<String, Document> asMap();

    /**
     * @return Returns true if this document is a document type List.
     */
    default boolean isList() {
        return false;
    }

    /**
     * Gets the document as a {@code List} if it is a document type array.
     * <p>Each value contained in the {@code List} is the same as how the
     * value would be represented by {@link Document}.
     *
     * @return Returns the lists of Document.
     * @throws UnsupportedOperationException if the document is not an List.
     */
    List<Document> asList();


    /**
     * Accepts a visitor to the Document.
     * @param <R> visitor return type.
     * @param visitor Visitor to dispatch to.
     * @return Returns the accepted result.
     */
    <R> R accept(DocumentVisitor<? extends R> visitor);

    /**
     * Accepts a visitor with the Document.
     * @param visitor Visitor to dispatch to.
     */
    void accept(VoidDocumentVisitor visitor);

    interface MapBuilder {
        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given String.
         * @param key Map Key for the Document.
         * @param stringValue String value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putString(String key, String stringValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given Number.
         * @param key Map Key for the Document.
         * @param numberValue Number value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, SdkNumber numberValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given integer.
         * @param key Map Key for the Document.
         * @param numberValue Integer value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, int numberValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given long.
         * @param key Map Key for the Document.
         * @param numberValue long value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, long numberValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given double.
         * @param key Map Key for the Document.
         * @param numberValue double value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, double numberValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given float.
         * @param key Map Key for the Document.
         * @param numberValue float value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, float numberValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given BigDecimal.
         * @param key Map Key for the Document.
         * @param numberValue BigDecimal value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, BigDecimal numberValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given BigInteger.
         * @param key Map Key for the Document.
         * @param numberValue BigInteger value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, BigInteger numberValue);

        /**
         *
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given String.
         * @param key Map Key for the Document.
         * @param numberValue String value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNumber(String key, String numberValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and a Document created from the given boolean.
         * @param key Map Key for the Document.
         * @param booleanValue Boolean value which will be used to create a Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putBoolean(String key, boolean booleanValue);

        /**
         * Inserts a Key Value pair to a Document Map with String key and the given Document.
         * @param key Map Key for the Document.
         * @param document Document to be inserted in a DocumentMap.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putDocument(String key, Document document);

        /**
         * Inserts a Key Value pair to a Document Map with String key and value with Null Document.
         * @param key Map Key for the Document.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putNull(String key);

        /**
         * Inserts a Key Value pair to a Document Map with String key and value as List of Document.
         * @param key Map Key for the Document.
         * @param documentList List of Documents.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putList(String key, List<Document> documentList);

        /**
         * Inserts a Key Value pair to a Document Map with String key and value constructed from Consumer of
         * {@link ListBuilder}.
         * @param key Map Key for the Document.
         * @param listBuilderConsumer Consumer that accepts {@link ListBuilder}
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putList(String key, Consumer<ListBuilder> listBuilderConsumer);

        /**
         * Inserts a Key Value pair to a Document Map with String key and Document constructed from Document Map.
         * @param key Map Key for the Document.
         * @param documentMap Map of Document.
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putMap(String key, Map<String, Document> documentMap);

        /**
         * Inserts a Key Value pair to a Document Map with String key and value constructed from Consumer of
         * {@link MapBuilder}.
         * @param key Map Key for the Document.
         * @param mapBuilderConsumer Consumer that accepts {@link ListBuilder}
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        MapBuilder putMap(String key, Consumer<MapBuilder> mapBuilderConsumer);

        /**
         * Creates a new {@link Document} with the key value pair pair inserted using put  method.
         * @return The new {@link Document}.
         */
        Document build();
    }

    interface ListBuilder {

        /**
         * Adds a Document which is constructed from the given stringValue..
         * @param stringValue String Value from which the Document to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addString(String stringValue);

        /**
         * Adds a Document which is constructed from the given boolean.
         * @param booleanValue Boolean value from which the  Document to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addBoolean(boolean booleanValue);

        /**
         * Adds a Document which is constructed from the given {@link SdkNumber}.
         * @param numberValue {@link SdkNumber} from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(SdkNumber numberValue);

        /**
         * Adds a Document which is constructed from the given integer.
         * @param numberValue integer from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(int numberValue);

        /**
         * Adds a Document which is constructed from the given long.
         * @param numberValue long from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(long numberValue);

        /**
         * Adds a Document which is constructed from the given float.
         * @param numberValue float from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(float numberValue);

        /**
         * Adds a Document which is constructed from the given double.
         * @param numberValue double from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(double numberValue);

        /**
         * Adds a Document which is constructed from the given BigDecimal.
         * @param numberValue BigDecimal from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(BigDecimal numberValue);

        /**
         * Adds a Document which is constructed from the given BigInteger.
         * @param numberValue BigInteger from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(BigInteger numberValue);

        /**
         *
         * Adds a Document which is constructed from the given String.
         * @param numberValue String from which the Document  to be added is created.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNumber(String numberValue);


        /**
         * Adds a Document to the constructed Document List.
         * @param document Document that will be added to a Document List.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addDocument(Document document);

        /**
         * Inserts a Document Value constructed from Consumer of {@link MapBuilder}.
         * @param mapBuilderConsumer Consumer that accepts {@link ListBuilder}
         * @return Builder which provides APIs to put Key Value pair to a Document Map.
         */
        ListBuilder addMap(Consumer<MapBuilder> mapBuilderConsumer);

        /**
         * Inserts a Null Document to the constructed Document List.
         * @return Builder which provides APIs to add Document to a Document List.
         */
        ListBuilder addNull();

        /**
         * Creates a new {@link Document} with the List members as added with add method.
         * @return The new {@link Document}.

         */
        Document build();
    }
}
