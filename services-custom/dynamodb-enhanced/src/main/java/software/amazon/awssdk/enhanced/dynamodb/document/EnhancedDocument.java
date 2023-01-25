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

package software.amazon.awssdk.enhanced.dynamodb.document;

import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

/**
 * Interface representing Document API for DynamoDB. Document API operations are used to carry open content i.e. data with no
 * fixed schema, data that can't be modeled using rigid types, or data that has a schema. This interface specifies all the
 * methods to access a Document, also provides constructor methods for instantiating Document that can be used to read and write
 * to DynamoDB using EnhancedDynamoDB client.
 *
 * TODO : Add some examples in the Java Doc after API Surface review.
 */
@SdkPublicApi
public interface EnhancedDocument {

    /**
     * Convenience factory method - instantiates an <code>EnhancedDocument</code> from the given JSON String.
     *
     * @param json The JSON string representation of DynamoDB Item.
     * @return A new instance of EnhancedDocument.
     */
    static EnhancedDocument fromJson(String json) {
        // TODO : return default implementation
        return null;
    }

    /**
     * Convenience factory method - instantiates an <code>EnhancedDocument</code> from the given Map
     *
     * @param attributes Map of item attributes where each attribute should be a simple Java type, not DynamoDB type.
     * @return A new instance of EnhancedDocument.
     */
    static EnhancedDocument fromMap(Map<String, Object> attributes) {
        // TODO : return default implementation
        return null;
    }

    /**
     * Creates a default builder for {@link EnhancedDocument}.
     */
    static Builder builder() {
        // TODO : return default implementation
        return null;
    }

    /**
     * Converts an existing EnhancedDocument into a builder object that can be used to modify its values and then create a new
     * EnhancedDocument.
     *
     * @return A {@link EnhancedDocument.Builder} initialized with the values of this EnhancedDocument.
     */
    Builder toBuilder();

    /**
     * Checks if the document is a {@code null} value.
     *
     * @param attributeName Name of the attribute that needs to be checked.
     * @return true if the specified attribute exists with a null value; false otherwise.
     */
    boolean isNull(String attributeName);

    /**
     * Checks if the attribute exists in the document.
     *
     * @param attributeName Name of the attribute that needs to be checked.
     * @return true if the specified attribute exists with a null/non-null value; false otherwise.
     */
    boolean isPresent(String attributeName);

    /**
     * Returns the value of the specified attribute in the current document as a specified {@link EnhancedType}; or null if the
     * attribute either doesn't exist or the attribute value is null.
     * <p>
     * <b>Retrieving String Type for a document</b>
     * {@snippet :
     * Custom resultCustom = document.get("key", EnhancedType.of(Custom.class));
     * }
     * <b>Retrieving Custom Type for which Convertor Provider was defined while creating the document</b>
     * {@snippet :
     * Custom resultCustom = document.get("key", EnhancedType.of(Custom.class));
     * }
     * <b>Retrieving list of strings in a document</b>
     * {@snippet :
     * List<String> resultList = document.get("key", EnhancedType.listOf(String.class));
     * }
     * <b>Retrieving a Map with List of strings in its values</b>
     * {@snippet :
     * Map<String, List<String>>> resultNested = document.get("key", new EnhancedType<Map<String, List<String>>>(){});
     * }
     * </p>
     * @param attributeName Name of the attribute.
     * @param type          EnhancedType of the value
     * @param <T>           The type of the attribute value.
     * @return Attribute value of type T
     * }
     */
    <T> T get(String attributeName, EnhancedType<T> type);

    /**
     * Gets the String value of specified attribute in the document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a string; or null if the attribute either doesn't exist
     * or the attribute value is null
     */
    String getString(String attributeName);

    /**
     * Gets the {@link SdkNumber} value of specified attribute in the document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a number; or null if the attribute either doesn't exist
     * or the attribute value is null
     */
    SdkNumber getSdkNumber(String attributeName);

    /**
     * Gets the {@link SdkBytes} value of specified attribute in the document.
     *
     * @param attributeName Name of the attribute.
     * @return the value of the specified attribute in the current document as SdkBytes; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    SdkBytes getSdkBytes(String attributeName);

    /**
     * Gets the Set of String values of the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of strings; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<String> getStringSet(String attributeName);

    /**
     * Gets the Set of {@link SdkNumber} values of the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of SdkNumber; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<SdkNumber> getNumberSet(String attributeName);

    /**
     * Gets the Set of {@link SdkBytes} values of the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of SdkBytes; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<SdkBytes> getSdkBytesSet(String attributeName);

    /**
     * Gets the List of values of type T for the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @param type          {@link EnhancedType} of Type T.         
     * @param <T>           Type T of List elements
     * @return value of the specified attribute in the current document as a list of type T; or null if the
     * attribute either doesn't exist or the attribute value is null.
     */

    <T> List<T> getList(String attributeName, EnhancedType<T> type);


    /**
     * Gets the List of values for the given attribute in the current document.
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a list; or null if the
     * attribute either doesn't exist or the attribute value is null.
     */
    List<?> getList(String attributeName);

    /**
     * Gets the Map with Key as String and values as type T for the given attribute in the current document.
     * <p>Note that any numeric type of map is always canonicalized into {@link SdkNumber}, and therefore if <code>T</code>
     * referred to a <code>Number</code> type, it would need to be <code>SdkNumber</code> to avoid a class cast exception.
     * </p>
     *
     * @param attributeName Name of the attribute.
     * @param type          {@link EnhancedType} of Type T.              
     * @param <T>           Type T of List elements
     * @return value of the specified attribute in the current document as a map of string-to-<code>T</code>'s; or null if the
     * attribute either doesn't exist or the attribute value is null.
     */
    <T> Map<String, T> getMap(String attributeName, EnhancedType<T> type);

    /**
     * Convenience method to return the specified attribute in the current item as a (copy of) map of
     * string-to-<code>SdkNumber</code>'s where T must be a subclass of <code>Number</code>; or null if the attribute doesn't
     * exist.
     *
     * @param attributeName Name of the attribute.
     * @param valueType     the specific number type of the value to be returned.
     *                     Currently, the supported types are
     *                     <ul>
     *                      <li><code>Short</code></li>
     *                      <li><code>Integer</code></li>
     *                      <li><code>Long</code></li>
     *                      <li><code>Float</code></li>
     *                      <li><code>Double</code></li>
     *                      <li><code>Number</code></li>
     *                      <li><code>BigDecimal</code></li>
     *                      <li><code>BigInteger</code></li>
     *                     </ul>
     * @return value of the specified attribute in the current item as a (copy of) map
     */
    <T extends Number> Map<String, T> getMapOfNumbers(String attributeName,
                                                      Class<T> valueType);

    /**
     * Convenience method to return the value of the specified attribute in the current document as a map of
     * string-to-<code>Object</code>'s; or null if the attribute either doesn't exist or the attribute value is null. Note that
     * any numeric type of the map will be returned as <code>SdkNumber</code>.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a raw map.
     */
    Map<String, Object> getRawMap(String attributeName);

    /**
     * Gets the Map value of the specified attribute as an EnhancedDocument.
     *
     * @param attributeName Name of the attribute.
     * @return Map value of the specified attribute in the current document as EnhancedDocument or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    EnhancedDocument getMapAsDocument(String attributeName);

    /**
     * Gets the JSON document value of the specified attribute.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a JSON string; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    String getJson(String attributeName);

    /**
     * Gets the JSON document value as pretty Json string for the specified attribute.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a JSON string with pretty indentation; or null if the
     * attribute either doesn't exist or the attribute value is null.
     */
    String getJsonPretty(String attributeName);

    /**
     * Gets the {@link Boolean} value for the specified attribute.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a non-null Boolean.
     * @throws RuntimeException
     *             if either the attribute doesn't exist or if the attribute
     *             value cannot be converted into a boolean value.
     */
    Boolean getBoolean(String attributeName);

    /**
     * Gets the value as Object for a given attribute in the current document.
     * An attribute value can be a
     * <ul>
     * <li>Number</li>
     * <li>String</li>
     * <li>Binary (ie byte array or byte buffer)</li>
     * <li>Boolean</li>
     * <li>Null</li>
     * <li>List (of any of the types on this list)</li>
     * <li>Map (with string key to value of any of the types on this list)</li>
     * <li>Set (of any of the types on this list)</li>
     * </ul>
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as an object; or null if the attribute either doesn't
     * exist or the attribute value is null.
     */
    Object get(String attributeName);

    /**
     * Gets the EnhancedType for the specified attribute key
     *
     * @param attributeName Name of the attribute.
     * @return type of the specified attribute in the current item; or null if the attribute either doesn't exist or the attribute
     * value is null.
     */
    EnhancedType<?> getTypeOf(String attributeName);

    /**
     * Gets the current EnhancedDocument as Map.
     *
     * @return attributes of the current document as a map.
     */
    Map<String, Object> asMap();

    /**
     *
     * @return document as a JSON string. Note all binary data will become base-64 encoded in the resultant string.
     */
    String toJson();

    /**
     * Gets the entire enhanced document as a pretty JSON string.
     *
     * @return document as a pretty JSON string. Note all binary data will become base-64 encoded in the resultant string
     */
    String toJsonPretty();

    @NotThreadSafe
    interface Builder {
        /**
         * Adds key attribute with the given value to the Document. An attribute value can be a
         * <ul>
         *  <li>Number</li>
         *  <li>String</li>
         *  <li>Binary (ie byte array or byte buffer)</li>
         *  <li>Boolean</li>
         *  <li>Null</li>
         *  <li>List (of any of the types on this list)</li>
         *  <li>Map (with string key to value of any of the types on this list)</li>
         *  <li>Set (of any of the types on this list)</li>
         * </ul>
         *
         * @param attributeName Name of the attribute that needs to be added in the Document builder.
         * @param value         Value of the specified attribute
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder add(String attributeName, Object value);

        /**
         * Appends an attribute of name attributeName with specified  {@link String} value to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The string value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addString(String attributeName, String value);

        /**
         * Appends an attribute of name attributeName with specified  {@link Number} value to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The number value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addNumber(String attributeName, Number value);

        /**
         * Appends an attribute of name attributeName with specified {@link SdkBytes} value to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The byte array value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addSdkBytes(String attributeName, SdkBytes value);

        /**
         * Appends an attribute of name attributeName with specified boolean value to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The boolean value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addBoolean(String attributeName, boolean value);

        /**
         * Appends an attribute of name attributeName with a null value.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addNull(String attributeName);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link String} values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        Set of String values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addStringSet(String attributeName, Set<String> values);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link Number} values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        Set of Number values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addNumberSet(String attributeName, Set<Number> values);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link SdkBytes} values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        Set of SdkBytes values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addSdkBytesSet(String attributeName, Set<SdkBytes> values);

        /**
         * Appends an attribute of name attributeName with specified list of values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The list of values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addList(String attributeName, List<?> value);

        /**
         * Appends an attribute of name attributeName with specified map values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The map that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addMap(String attributeName, Map<String, ?> value);

        /**
         * Appends an attribute of name attributeName with specified value of the given JSON document in the form of a string.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param json          JSON document in the form of a string.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addJson(String attributeName, String json);

        /**
         * Appends an attribute of name attributeName with specified value of the give EnhancedDocument.
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param enhancedDocument that needs to be added as a value to a key attribute.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addEnhancedDocument(String attributeName, EnhancedDocument enhancedDocument);

        /**
         * Appends collection of attributeConverterProvider to the document builder. These
         * AttributeConverterProvider will be used to convert any given key to custom type T.
         * @param attributeConverterProvider determining the {@link AttributeConverter} to use for converting a value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addAttributeConverterProvider(AttributeConverterProvider attributeConverterProvider);

        /**
         * Sets the collection of attributeConverterProviders to the document builder. These AttributeConverterProvider will be
         * used to convert value of any given key to custom type T.
         *
         * @param attributeConverterProviders determining the {@link AttributeConverter} to use for converting a value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders);

        /**
         * Sets collection of attributeConverterProviders to the document builder. These AttributeConverterProvider will be
         * used to convert any given key to custom type T.
         *
         * @param attributeConverterProvider determining the {@link AttributeConverter} to use for converting a value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder attributeConverterProviders(AttributeConverterProvider... attributeConverterProvider);

        /**
         * Sets the entire JSON document in the form of a string to the document builder.
         *
         * @param json JSON document in the form of a string.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder json(String json);

        /**
         * Builds an instance of {@link EnhancedDocument}.
         *
         * @return instance of {@link EnhancedDocument} implementation.
         */
        EnhancedDocument build();
    }
}
