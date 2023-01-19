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

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.KeyAttributeMetadata;


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
     * <b>Retrieving different types of attribute values</b>
     * {@snippet :
     * <p>
     * //get EnhancedDocument of String Type for EnhancedDocument document
     * String resultString = document.get("key", EnhancedType.of(String.class));
     * <p>
     * //get EnhancedDocument of Custom Type for which Convertor Provider was defined for a document
     * Custom resultCustom = document.get("key", EnhancedType.of(Custom.class));
     * <p>
     * //get EnhancedDocument of List of String.
     * List<String> resultList = document.get("key", EnhancedType.listOf(String.class));
     * <p>
     * //get EnhancedDocument of nested Map with list string values.
     * Map<String, List<String>>> resultNested = document.get("key", new EnhancedType<Map<String, List<String>>>(){}); }
     * </p>
     * @param attributeName Name of the attribute.
     * @param type          EnhancedType of the value
     * @param <T>           The type of the attribute value.
     * @return Attribute value of type T
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
     * Gets the {@link Number} value of specified attribute in the document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a number; or null if the attribute either doesn't exist
     * or the attribute value is null
     */
    SdkNumber getSdkNumber(String attributeName);

    /**
     * Gets the byte array value of specified attribute in the document.
     *
     * @param attributeName Name of the attribute.
     * @return the value of the specified attribute in the current document as a byte array; or null if the attribute either
     * doesn't exist or the attribute value is null.
     * @throws UnsupportedOperationException If the attribute value involves a byte buffer which is not backed by an accessible
     *                                       array
     */
    byte[] getBinary(String attributeName);

    /**
     * Gets the value of the specified attribute in the current document as a
     * <code>ByteBuffer</code>; or null if the attribute either doesn't exist or the attribute value is null.
     *
     * @param attributeName Name of the attribute.
     * @return the value of the specified attribute in the current document as a ByteBuffer; or null if the attribute either
     * doesn't exist or the attribute value is null.
     * @throws UnsupportedOperationException If the attribute value involves a byte buffer which is not backed by an accessible
     *                                       array
     */
    ByteBuffer getByteBuffer(String attributeName);

    /**
     * Gets the Set of String values of the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of strings; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<String> getStringSet(String attributeName);

    /**
     * Gets the Set of Number values of the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of BigDecimal's; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<Number> getNumberSet(String attributeName);

    /**
     * Gets the Set of Binary values of the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of byte arrays; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<byte[]> getBinarySet(String attributeName);

    /**
     * Gets the Set of ByteBuffer values of the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of <code>ByteBuffer</code>; or null if the
     * attribute either doesn't exist or the attribute value is null.
     */
    Set<ByteBuffer> getByteBufferSet(String attributeName);

    /**
     * Gets the List of values of type T for the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @param type          {@link EnhancedType} of Type T.         
     * @param <T>           Type T of List elements
     * @return value of the specified attribute in the current document as a set of <code>ByteBuffer</code>; or null if the
     * attribute either doesn't exist or the attribute value is null.
     */

    <T> List<T> getList(String attributeName, EnhancedType<T> type);

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
     * string-to-<code>T</code>'s where T must be a subclass of <code>Number</code>; or null if the attribute doesn't exist.
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
    String getJSONPretty(String attributeName);

    /**
     * Gets the {@link Boolean} value for the specified attribute.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a non-null Boolean.
     */
    Boolean getBOOL(String attributeName);

    /**
     * Gets the boolean value for the specified attribute.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a primitive boolean
     */
    boolean getBoolean(String attributeName);

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
     * Iterable for all the attributes in the EnhancedDocument
     *
     * @return all attributes of the current item.
     */
    Iterable<Map.Entry<String, Object>> attributes();

    /**
     * Gets the current EnhancedDocument as Map.
     *
     * @return attributes of the current document as a map.
     */
    Map<String, Object> asMap();

    /**
     * Returns the number of attributes in the current EnhancedDocument.
     */
    int numberOfAttributes();

    /**
     *
     * @return document as a JSON string. Note all binary data will become base-64 encoded in the resultant string.
     */
    String toJson();

    /**
     * Convenience method to decode the designated binary attributes from base-64 encoding; converting binary lists into binary
     * sets
     *
     * @param binaryAttrNames names of binary attributes or binary set attributes currently base-64 encoded (typically when
     *                        converted from a JSON string.)
     * @return decoded binary attributes values from base-64 encoding values of the given binaryAttrNames.
     */
    EnhancedDocument base64Decode(String... binaryAttrNames);

    /**
     * Gets the entire enhanced document as a pretty JSON string.
     *
     * @return document as a pretty JSON string. Note all binary data will become base-64 encoded in the resultant string
     */
    String toJSONPretty();

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
         * Appends an attribute of name attributeName with specified byte array value to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The byte array value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addBinary(String attributeName, byte[] value);

        /**
         * Appends an attribute of name attributeName with specified {@link ByteBuffer} value to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The ByteBuffer value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addBinary(String attributeName, ByteBuffer value);

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
         * Appends an attribute of name attributeName with specified Set of {@link String} values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         Set of String values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addStringSet(String attributeName, String... value);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link Number} values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         Set of Number values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addNumberSet(String attributeName, Number... value);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link Number} values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        Set of Number values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addNumberSet(String attributeName, Set<Number> values);

        /**
         * Appends an attribute of name attributeName with specified Set of byte array values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        Set of byte array values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addBinarySet(String attributeName, Set<byte[]> values);

        /**
         * Appends an attribute of name attributeName with specified  {@link Set}  of {@link ByteBuffer} values to the document
         * builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        Set of ByteBuffer values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addByteBufferSet(String attributeName, Set<ByteBuffer> values);

        /**
         * Appends an attribute of name attributeName with specified binary byte array values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        The binary byte array values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addBinarySet(String attributeName, byte[]... values);

        /**
         * Appends an attribute of name attributeName with specified set of ByteBuffer values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        The ByteBuffer values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addBinarySet(String attributeName, ByteBuffer... values);

        /**
         * Appends an attribute of name attributeName with specified list of values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param value         The list of values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addList(String attributeName, List<?> value);

        /**
         * Appends an attribute of name attributeName with specified list of values to the document builder.
         *
         * @param attributeName Name of the attribute that needs to be added in the Document.
         * @param values        The list of values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addList(String attributeName, Object... values);

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
         * Convenience builder methods that sets the attributes of this documents from the specified KeyAttributeMetadata
         * components.
         *
         * @param components KeyAttributeMetadata of the attributes
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder keyComponents(KeyAttributeMetadata... components);

        /**
         * Convenience builder methods that sets an attribute of this document for the specified key attribute name and value.
         *
         * @param keyAttrName  Name of the attribute that needs to be added in the Document.
         * @param keyAttrValue The value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder keyComponent(KeyAttributeMetadata keyAttrName, Object keyAttrValue);

        /**
         * Appends collection of attributeConverterProvider to the document builder These
         * AttributeConverterProvider will be used to convert any given key to custom type T.
         *
         * @param attributeConverterProvider determining the {@link AttributeConverter} to use for converting a value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addAttributeConverterProviders(AttributeConverterProvider... attributeConverterProvider);

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
