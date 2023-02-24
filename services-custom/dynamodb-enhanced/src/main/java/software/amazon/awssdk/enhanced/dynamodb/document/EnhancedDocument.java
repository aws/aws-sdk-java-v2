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
import java.util.UUID;
import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;

/**
 * Interface representing Document API for DynamoDB. Document API operations are used to carry open content i.e. data with no
 * fixed schema, data that can't be modeled using rigid types, or data that has a schema. This interface specifies all the
 * methods to access a Document, also provides constructor methods for instantiating Document that can be used to read and write
 * to DynamoDB using EnhancedDynamoDB client.
 *
 * TODO : Add some examples in the Java Doc after API Surface review.
 */

/**
 * The Document API interface for DynamoDB provides operations for working with unstructured data. This includes data with no
 * fixed schema, data that can't be modeled using rigid types, or data that has a flexible schema. This interface defines
 * methods for accessing a Document and provides constructor methods for instantiating a Document object. You can use the
 * Document object to read and write data to DynamoDB using the EnhancedDynamoDB client.
 * se the methods defined in this interface to perform operations such as adding, retrieving, updating, and deleting data.  For
 * example, you can use the {@code putItem} method to add an item to a DynamoDB table, or the {@code getItem} method to
 * retrieve  an item from a table.
 * To use the Document API, you must first create an instance of the EnhancedDynamoDB client. Then, you can use the methods
 * defined in this interface to interact with the data stored in DynamoDB.
 * {@code Example Usage:}
 * <pre>{@code
 * EnhancedDynamoDbClient enhancedClient = EnhancedDynamoDbClient.builder()
 *
 * markdown
 *
 *        .dynamoDbClient(dynamoDbClient)
 *
 * markdown
 *
 *        .build();
 *
 * // Create a new Document object
 * Document document = Document.create();
 * // Add an item to the Document
 * document.put("key", "value");
 * // Write the item to DynamoDB using the EnhancedDynamoDB client
 * enhancedClient.putItem(PutItemEnhancedRequest.builder("tableName").item(document).build());
 * // Retrieve the item from DynamoDB
 * GetItemEnhancedResponse<Document> response = enhancedClient.getItem(GetItemEnhancedRequest.builder("tableName")
 *
 * less
 *
 *        .key(Collections.singletonMap("partitionKey", "partitionValue"))
 *
 * markdown
 *
 *        .build());
 *
 * // Get the item from the response
 * Document retrievedItem = response.item();
 * }</pre>
 * */
@SdkPublicApi
public interface EnhancedDocument {

    /**
     * Convenience factory method - instantiates an <code>EnhancedDocument</code> from the given JSON String.
     *
     * @param json The JSON string representation of DynamoDB Item.
     * @return A new instance of EnhancedDocument.
     */
    static EnhancedDocument fromJson(String json, List<AttributeConverterProvider> attributeConverterProviders) {
        Validate.paramNotNull(json, "json");
        return DefaultEnhancedDocument.builder()
                                      .json(json)
                                      .attributeConverterProviders(attributeConverterProviders)
                                      .build();
    }

    /**
     * Convenience factory method - instantiates an <code>EnhancedDocument</code> from the given AttributeValueMap.
     * @param attributeValueMap - Map with Attributes as String keys and AttributeValue as Value.
     * @return A new instance of EnhancedDocument.
     */
    static EnhancedDocument fromAttributeValueMap(Map<String, AttributeValue> attributeValueMap) {
        Validate.paramNotNull(attributeValueMap, "attributeValueMap");
        return DefaultEnhancedDocument.builder()
                                      .attributeValueMap(attributeValueMap)
                                      .attributeConverterProviders(DefaultAttributeConverterProvider.create())
                                      .build();
    }

    /**
     * Convenience factory method - instantiates an <code>EnhancedDocument</code> from the given Map
     *
     * @param attributeMap Map of item attributeMap where each attribute should be a simple Java type, not DynamoDB type.
     * @return A new instance of EnhancedDocument.
     */
    static EnhancedDocument fromMap(Map<String, Object> attributeMap) {
        Validate.paramNotNull(attributeMap, "attributeMap");
        DefaultEnhancedDocument.DefaultBuilder defaultBuilder = DefaultEnhancedDocument.builder();
        attributeMap.forEach((k, v) -> defaultBuilder.putObject(k, v));
        return defaultBuilder.addAttributeConverterProvider(DefaultAttributeConverterProvider.create())
                             .build();
    }

    /**
     * Creates a default builder for {@link EnhancedDocument}.
     */
    static Builder builder() {
        return DefaultEnhancedDocument.builder();
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
    SdkNumber getNumber(String attributeName);

    /**
     * Gets the {@link SdkBytes} value of specified attribute in the document.
     *
     * @param attributeName Name of the attribute.
     * @return the value of the specified attribute in the current document as SdkBytes; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    SdkBytes getBytes(String attributeName);

    /**
     * Gets the Set of String values of the given attribute in the current document.
     * This API only looks for {@link AttributeValueType#SS}. Thus, this API will return null for attribute values which are
     * represented as List {@link AttributeValueType#L} of Strings.
     *
     * @param attributeName the name of the attribute.
     * @return the value of the specified attribute in the current document as a set of strings; or null if the attribute either
     * does not exist or the attribute value is null.
     */
    Set<String> getStringSet(String attributeName);

    /**
     * Gets the Set of String values of the given attribute in the current document.
     * This API only looks for {@link AttributeValueType#NS}. Thus, this API will return null for attribute values which are
     * represented as List {@link AttributeValueType#L} of Numbers.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of SdkNumber; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<SdkNumber> getNumberSet(String attributeName);

    /**
     * Gets the Set of String values of the given attribute in the current document.
     * This API only looks for {@link AttributeValueType#BS}. Thus, this API will return null for attribute values which are
     * represented as List {@link AttributeValueType#L} of SdkBytes.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of SdkBytes; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<SdkBytes> getBytesSet(String attributeName);

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
     * Returns a map of a specific type based on the given attribute name, key type, and value type.
     * Example usage: When getting an attribute as a map of  {@link UUID} keys and {@link Integer} values, use this API as shown below:
     * {@snippet :
        Map<String, Integer> result = document.getMap("key", EnhancedType.of(UUID.class), EnhancedType.of(Integer.class));
     * }
     * @param attributeName The name of the attribute that needs to be get as Map.
     * @param keyType Enhanced Type of Key attribute, like String, UUID etc that can be represented as String Keys.
     * @param valueType Enhanced Type of Values , which have converters defineds in
     * {@link Builder#attributeConverterProviders(AttributeConverterProvider...)} for the document
     * @return Map of type K and V with the given attribute name, key type, and value type.
     * @param <K> The type of the Map keys.
     * @param <V> The type of the Map values.
     */
    <K, V> Map<K, V> getMapType(String attributeName, EnhancedType<K> keyType, EnhancedType<V> valueType);

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
    EnhancedDocument getEnhancedDocument(String attributeName);

    /**
     * Gets the JSON document value of the specified attribute.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a JSON string; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    String getJson(String attributeName);

    /**
     * Gets the {@link Boolean} value for the specified attribute.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a non-null Boolean.
     * @throws RuntimeException
     *             if either the attribute doesn't exist or if the attribute
     *             value cannot be converted into a boolean value.
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
     * Gets the current EnhancedDocument as Map.
     *
     * @return attributes of the current document as a map.
     */
    Map<String, Object> toMap();

    /**
     *
     * @return document as a JSON string. Note all binary data will become base-64 encoded in the resultant string.
     */
    String toJson();

    /**
     * Gets the current EnhancedDocument as a <String, AttributeValue> Map.
     * @return EnhancedDocument as a Map with Keys as String attributes and Values as AttributeValue.
     */
    Map<String, AttributeValue> toAttributeValueMap();

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
        Builder putObject(String attributeName, Object value);

        /**
         * Appends an attribute of name attributeName with specified  {@link String} value to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The string value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putString(String attributeName, String value);

        /**
         * Appends an attribute of name attributeName with specified  {@link Number} value to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The number value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putNumber(String attributeName, Number value);

        /**
         * Appends an attribute of name attributeName with specified {@link SdkBytes} value to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The byte array value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putBytes(String attributeName, SdkBytes value);

        /**
         * Appends an attribute of name attributeName with specified boolean value to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The boolean value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putBoolean(String attributeName, boolean value);

        /**
         * Appends an attribute of name attributeName with a null value.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putNull(String attributeName);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link String} values to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param values        Set of String values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putStringSet(String attributeName, Set<String> values);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link Number} values to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param values        Set of Number values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putNumberSet(String attributeName, Set<Number> values);

        /**
         * Appends an attribute of name attributeName with specified Set of {@link SdkBytes} values to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param values        Set of SdkBytes values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putBytesSet(String attributeName, Set<SdkBytes> values);

        /**
         * Appends an attribute of name attributeName with specified list of values to the document builder.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The list of values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putObjectList(String attributeName, List<?> value);


        /**
         * Appends an attribute to the document builder, with the given name and a map of values to be set. The keys of the map
         * can be of any type T that can be converted to a string type.
         *
         * @param attributeName the name of the attribute to be added to the document
         * @param value         the map of values to be set
         * @param keyType       the class type of the keys in the map
         * @param <T>           the type parameter for the keys in the map
         * @return a builder instance to construct a {@link EnhancedDocument}
         */
        <T> Builder putMap(String attributeName, Map<T, ?> value, Class<T> keyType);

        /**
         * Appends an attribute to the document builder, with the given name and a map of values to be set.
         * @param attributeName the name of the attribute to be added to the document
         * @param value         the map of values to be set
         * @return a builder instance to construct a {@link EnhancedDocument}
         * @throws NullPointerException if the attributeName or value is null
         */
        Builder putMap(String attributeName, Map<String, ?> value);

        /**
         * Appends an attribute of name attributeName with specified value of the given JSON document in the form of a string.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param json          JSON document in the form of a string.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putJson(String attributeName, String json);

        /**
         * Appends an attribute of name attributeName with specified value of the given EnhancedDocument.
         * @param attributeName the name of the attribute to be added to the document.
         * @param enhancedDocument that needs to be added as a value to a key attribute.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putEnhancedDocument(String attributeName, EnhancedDocument enhancedDocument);

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
         * Sets the attributes of the document builder to those defined in the given JSON string, completely overwriting any
         * previously set attributes.
         *
         * @param json a JSON document represented as a string
         * @return a builder instance to construct a {@link EnhancedDocument}
         * @throws NullPointerException if the json parameter is null
         */
        Builder json(String json);

        /**
         * Builds an instance of {@link EnhancedDocument}.
         *
         * @return instance of {@link EnhancedDocument} implementation.
         */
        EnhancedDocument build();
    }


    /**
     *
     * @return List of AttributeConverterProvider defined for the given Documnent
     */
    List<AttributeConverterProvider> attributeConverterProviders();
}
