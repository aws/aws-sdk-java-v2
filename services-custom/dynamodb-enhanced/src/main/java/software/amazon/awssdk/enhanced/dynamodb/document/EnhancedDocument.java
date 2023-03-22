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

import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;

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
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Validate;


/**
 * Interface representing the Document API for DynamoDB. The Document API operations are designed to work with open content,
 * such as data with no fixed schema, data that cannot be modeled using rigid types, or data that has a schema.
 * This interface provides all the methods required to access a Document, as well as constructor methods for creating a
 * Document that can be used to read and write to DynamoDB using the EnhancedDynamoDB client.
 * Additionally, this interface provides flexibility when working with data, as it allows you to work with data that is not
 * necessarily tied to a specific data model.
 * The EnhancedDocument interface provides two ways to use AttributeConverterProviders:
 * <p>Enhanced Document with default attribute Converter to convert the attribute of DDB item to basic default primitive types in
 * Java
 * {@snippet :
 * EnhancedDocument enhancedDocument = EnhancedDocument.builder().attributeConverterProviders(AttributeConverterProvider
 * .defaultProvider()).build();
 *}
 * <p>Enhanced Document with Custom attribute Converter to convert the attribute of DDB Item to Custom Type.
 * {@snippet :
 * // CustomAttributeConverterProvider.create() is an example for some Custom converter provider
 * EnhancedDocument enhancedDocumentWithCustomConverter = EnhancedDocument.builder().attributeConverterProviders
 * (CustomAttributeConverterProvider.create(), AttributeConverterProvide.defaultProvider()
 * .put("customObject", customObject, EnhancedType.of(CustomClass.class))
 * .build();
 *}
 * <p>Enhanced Document can be created with Json as input using Static factory method.In this case it used
 * defaultConverterProviders.
 * {@snippet :
 * EnhancedDocument enhancedDocumentWithCustomConverter = EnhancedDocument.fromJson("{\"k\":\"v\"}");
 *}
 * The attribute converter are always required to be provided, thus for default conversion
 * {@link AttributeConverterProvider#defaultProvider()} must be supplied.
 */
@SdkPublicApi
public interface EnhancedDocument {

    /**
     * Creates a new <code>EnhancedDocument</code> instance from a JSON string.
     * The {@link AttributeConverterProvider#defaultProvider()} is used as the default ConverterProvider.
     * To use a custom ConverterProvider, use the builder methods: {@link Builder#json(String)} to supply the JSON string,
     * then use {@link Builder#attributeConverterProviders(AttributeConverterProvider...)} to provide the custom
     * ConverterProvider.
     * {@snippet :
     * EnhancedDocument documentFromJson = EnhancedDocument.fromJson("{\"key\": \"Value\"}");
     *}
     * @param json The JSON string representation of a DynamoDB Item.
     * @return A new instance of EnhancedDocument.
     * @throws IllegalArgumentException if the json parameter is null
     */
    static EnhancedDocument fromJson(String json) {
        Validate.paramNotNull(json, "json");
        return DefaultEnhancedDocument.builder()
                                      .json(json)
                                      .attributeConverterProviders(defaultProvider())
                                      .build();
    }

    /**
     * Creates a new <code>EnhancedDocument</code> instance from a AttributeValue Map.
     * The {@link AttributeConverterProvider#defaultProvider()} is used as the default ConverterProvider.
     * Example usage:
     * {@snippet :
     * EnhancedDocument documentFromJson = EnhancedDocument.fromAttributeValueMap(stringKeyAttributeValueMao)});
     *}
     * @param attributeValueMap - Map with Attributes as String keys and AttributeValue as Value.
     * @return A new instance of EnhancedDocument.
     * @throws IllegalArgumentException if the json parameter is null
     */
    static EnhancedDocument fromAttributeValueMap(Map<String, AttributeValue> attributeValueMap) {
        Validate.paramNotNull(attributeValueMap, "attributeValueMap");
        return DefaultEnhancedDocument.builder()
                                      .attributeValueMap(attributeValueMap)
                                      .attributeConverterProviders(defaultProvider())
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
     * String resultCustom = document.get("key", EnhancedType.of(String.class));
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
     * Returns the value of the specified attribute in the current document as a specified class type; or null if the
     * attribute either doesn't exist or the attribute value is null.
     * <p>
     * <b>Retrieving String Type for a document</b>
     * {@snippet :
     * String resultCustom = document.get("key", String.class);
     * }
     * <b>Retrieving Custom Type for which Convertor Provider was defined while creating the document</b>
     * {@snippet :
     * Custom resultCustom = document.get("key", Custom.class);
     * }
     * <p>
     * Note :
     * This API should not be used to retrieve values of List and Map types.
     * Instead, getList and getMap APIs should be used to retrieve attributes of type List and Map, respectively.
     * </p>
     * @param attributeName Name of the attribute.
     * @param clazz         Class type of value.
     * @param <T>           The type of the attribute value.
     * @return Attribute value of type T
     * }
     */
    <T> T get(String attributeName, Class<T> clazz);

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
     * @param attributeName the name of the attribute.
     * @return the value of the specified attribute in the current document as a set of strings; or null if the attribute either
     * does not exist or the attribute value is null.
     */
    Set<String> getStringSet(String attributeName);

    /**
     * Gets the Set of String values of the given attribute in the current document.
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of SdkNumber; or null if the attribute either
     * doesn't exist or the attribute value is null.
     */
    Set<SdkNumber> getNumberSet(String attributeName);

    /**
     * Gets the Set of String values of the given attribute in the current document.
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a set of SdkBytes;
     * or null if the attribute doesn't exist.
     */
    Set<SdkBytes> getBytesSet(String attributeName);

    /**
     * Gets the List of values of type T for the given attribute in the current document.
     *
     * @param attributeName Name of the attribute.
     * @param type          {@link EnhancedType} of Type T.         
     * @param <T>           Type T of List elements
     * @return value of the specified attribute in the current document as a list of type T,
     * or null if the attribute does not exist.
     */

    <T> List<T> getList(String attributeName, EnhancedType<T> type);

    /**
     *  Returns a map of a specific Key-type and Value-type based on the given attribute name, key type, and value type.
     * Example usage: When getting an attribute as a map of  {@link UUID} keys and {@link Integer} values, use this API
     * as shown below:
     * {@snippet :
        Map<String, Integer> result = document.getMap("key", EnhancedType.of(String.class), EnhancedType.of(Integer.class));
     * }
     * @param attributeName The name of the attribute that needs to be get as Map.
     * @param keyType Enhanced Type of Key attribute, like String, UUID etc that can be represented as String Keys.
     * @param valueType Enhanced Type of Values , which have converters defineds in
     * {@link Builder#attributeConverterProviders(AttributeConverterProvider...)} for the document
     * @return Map of type K and V with the given attribute name, key type, and value type.
     * @param <K> The type of the Map keys.
     * @param <V> The type of the Map values.
     */
    <K, V> Map<K, V> getMap(String attributeName, EnhancedType<K> keyType, EnhancedType<V> valueType);

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
     * @return value of the specified attribute in the current document as a Boolean representation; or null if the attribute
     * either doesn't exist or the attribute value is null.
     * @throws RuntimeException
     *             if the attribute value cannot be converted to a Boolean representation.
     *             Note that the Boolean representation of 0 and 1 in Numbers and "0" and "1" in Strings is false and true,
     *             respectively.
     *
     */
    Boolean getBoolean(String attributeName);


    /**
     * Retrieves a list of {@link AttributeValue} objects for a specified attribute in a document.
     * This API should be used when the elements of the list are a combination of different types such as Strings, Maps,
     * and Numbers.
     * If all elements in the list are of a known fixed type, use {@link EnhancedDocument#getList(String, EnhancedType)} instead.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a List of {@link AttributeValue}
     */
    List<AttributeValue> getListOfUnknownType(String attributeName);

    /**
     * Retrieves a Map with String keys and corresponding AttributeValue objects as values for a specified attribute in a
     * document. This API is particularly useful when the values of the map are of different types such as strings, maps, and
     * numbers. However, if all the values in the map for a given attribute key are of a known fixed type, it is recommended to
     * use the method EnhancedDocument#getMap(String, EnhancedType, EnhancedType) instead.
     *
     * @param attributeName Name of the attribute.
     * @return value of the specified attribute in the current document as a {@link AttributeValue}
     */
    Map<String, AttributeValue> getMapOfUnknownType(String attributeName);

    /**
     *
     * @return document as a JSON string. Note all binary data will become base-64 encoded in the resultant string.
     */
    String toJson();

    /**
     * This method converts a document into a key-value map with the keys as String objects and the values as AttributeValue
     * objects. It can be particularly useful for documents with attributes of unknown types, as it allows for further processing
     * or manipulation of the document data in a AttributeValue format.
     * @return Document as a String AttributeValue Key-Value Map
     */
    Map<String, AttributeValue> toMap();

    /**
     *
     * @return List of AttributeConverterProvider defined for the given Document.
     */
    List<AttributeConverterProvider> attributeConverterProviders();

    @NotThreadSafe
    interface Builder {

        /**
         * Appends an attribute of name attributeName with specified  {@link String} value to the document builder.
         * Use this method when you need to add a string value to a document. If you need to add an attribute with a value of a
         * different type, such as a number or a map, use the appropriate put method instead
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The string value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putString(String attributeName, String value);

        /**
         * Appends an attribute of name attributeName with specified  {@link Number} value to the document builder.
         * Use this method when you need to add a number value to a document. If you need to add an attribute with a value of a
         * different type, such as a string or a map, use the appropriate put method instead
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The number value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putNumber(String attributeName, Number value);

        /**
         * Appends an attribute of name attributeName with specified  {@link SdkBytes} value to the document builder.
         * Use this method when you need to add a binary value to a document. If you need to add an attribute with a value of a
         * different type, such as a string or a map, use the appropriate put method instead
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The byte array value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putBytes(String attributeName, SdkBytes value);

        /**
         * Use this method when you need to add a boolean value to a document. If you need to add an attribute with a value of a
         * different type, such as a string or a map, use the appropriate put method instead.
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The boolean value that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putBoolean(String attributeName, boolean value);

        /**
         * Appends an attribute of name attributeName with a null value.
         * Use this method is the attribute needs to explicitly set to null in Dynamo DB table.
         *
         * @param attributeName the name of the attribute to be added to the document.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putNull(String attributeName);

        /**
         * Appends an attribute to the document builder with a Set of Strings as its value.
         * This method is intended for use in DynamoDB where attribute values are stored as Sets of Strings.
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
         * Appends an attribute with the specified name and a list of {@link EnhancedType}  T type elements to the document
         * builder.
         * Use {@link EnhancedType#of(Class)} to specify the class type of the list elements.
         * <p>For example, to insert a list of String type:
         * {@snippet :
         * EnhancedDocument.builder().putList(stringList, EnhancedType.of(String.class))
         * }
         * <p>Example for inserting a List of Custom type .
         * {@snippet :
         * EnhancedDocument.builder().putList(stringList, EnhancedType.of(CustomClass.class));
         * }
         * Note that the AttributeConverterProvider added to the DocumentBuilder should provide the converter for the class T that
         * is to be inserted.
         * @param attributeName the name of the attribute to be added to the document.
         * @param value         The list of values that needs to be set.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        <T> Builder putList(String attributeName, List<T> value, EnhancedType<T> type);

        /**
         * Appends an attribute named {@code attributeName} with a value of type {@link EnhancedType} T.
         * Use this method to insert attribute values of custom types that have attribute converters defined in a converter
         * provider.
         * Example:
         {@snippet :
          * EnhancedDocument.builder().put("customKey", customValue, EnhancedType.of(CustomClass.class));
          *}
         * Use {@link #putString(String, String)} or {@link #putNumber(String, Number)} for inserting simple value types of
         * attributes.
         * Use {@link #putList(String, List, EnhancedType)} or {@link #putMap(String, Map, EnhancedType, EnhancedType)} for
         * inserting collections of attribute values.
         * Note that the attribute converter provider added to the DocumentBuilder must provide the converter for the class T
         * that is to be inserted.
         @param attributeName the name of the attribute to be added to the document.
         @param value the value to set.
         @param type the Enhanced type of the value to set.
         @return a builder instance to construct a {@link EnhancedDocument}.
         @param <T> the type of the value to set.
         */
        <T> Builder put(String attributeName, T value, EnhancedType<T> type);

        /**
         * Appends an attribute named {@code attributeName} with a value of Class type T.
         * Use this method to insert attribute values of custom types that have attribute converters defined in a converter
         * provider.
         * Example:
         {@snippet :
          * EnhancedDocument.builder().put("customKey", customValue, CustomClass.class);
          *}
         * Use {@link #putString(String, String)} or {@link #putNumber(String, Number)} for inserting simple value types of
         * attributes.
         * Use {@link #putList(String, List, EnhancedType)} or {@link #putMap(String, Map, EnhancedType, EnhancedType)} for
         * inserting collections of attribute values.
         * Note that the attribute converter provider added to the DocumentBuilder must provide the converter for the class T
         * that is to be inserted.
         @param attributeName the name of the attribute to be added to the document.
         @param value the value to set.
         @param type the type of the value to set.
         @return a builder instance to construct a {@link EnhancedDocument}.
         @param <T> the type of the value to set.
         */
        <T> Builder put(String attributeName, T value, Class<T> type);

        /**
         * Appends an attribute with the specified name and a Map containing keys and values of {@link EnhancedType} K
         * and V types,
         * respectively, to the document builder. Use {@link EnhancedType#of(Class)} to specify the class type of the keys and
         * values.
         * <p>For example, to insert a map with String keys and Long values:
         * {@snippet :
         * EnhancedDocument.builder().putMap("stringMap", mapWithStringKeyNumberValue, EnhancedType.of(String.class),
         * EnhancedType.of(String.class), EnhancedType.of(Long.class))
         *}
         * <p>For example, to insert a map of String Key and Custom Values:
         * {@snippet :
         * EnhancedDocument.builder().putMap("customMap", mapWithStringKeyCustomValue, EnhancedType.of(String.class),
         * EnhancedType.of(String.class), EnhancedType.of(Custom.class))
         *}
         * Note that the AttributeConverterProvider added to the DocumentBuilder should provide the converter for the classes
         * K and V that
         * are to be inserted.
         * @param attributeName the name of the attribute to be added to the document
         * @param value         The Map of values that needs to be set.
         * @param keyType       Enhanced type of Key class
         * @param valueType     Enhanced type of Value class.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        <K, V> Builder putMap(String attributeName, Map<K, V> value, EnhancedType<K> keyType, EnhancedType<V> valueType);

        /**
         Appends an attribute to the document builder with the specified name and value of a JSON document in string format.
         * @param attributeName the name of the attribute to be added to the document.
         * @param json          JSON document in the form of a string.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder putJson(String attributeName, String json);


        /**
         * Removes a previously appended attribute.
         * This can be used where a previously added attribute to the Builder is no longer needed.
         * @param attributeName The attribute that needs to be removed.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder remove(String attributeName);

        /**
         * Appends collection of attributeConverterProvider to the document builder. These
         * AttributeConverterProvider will be used to convert any given key to custom type T.
         * The first matching converter from the given provider will be selected based on the order in which they are added.
         * @param attributeConverterProvider determining the {@link AttributeConverter} to use for converting a value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder addAttributeConverterProvider(AttributeConverterProvider attributeConverterProvider);

        /**
         * Sets the collection of attributeConverterProviders to the document builder. These AttributeConverterProvider will be
         * used to convert value of any given key to custom type T.
         * The first matching converter from the given provider will be selected based on the order in which they are added.
         * @param attributeConverterProviders determining the {@link AttributeConverter} to use for converting a value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder attributeConverterProviders(List<AttributeConverterProvider> attributeConverterProviders);

        /**
         * Sets collection of attributeConverterProviders to the document builder. These AttributeConverterProvider will be
         * used to convert any given key to custom type T.
         * The first matching converter from the given provider will be selected based on the order in which they are added.
         * @param attributeConverterProvider determining the {@link AttributeConverter} to use for converting a value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder attributeConverterProviders(AttributeConverterProvider... attributeConverterProvider);

        /**
         * Sets the attributes of the document builder to those specified in the provided JSON string, and completely replaces
         * any previously set attributes.
         *
         * @param json a JSON document represented as a string
         * @return a builder instance to construct a {@link EnhancedDocument}
         * @throws NullPointerException if the json parameter is null
         */
        Builder json(String json);

        /**
         * Sets the attributes of the document builder to those specified in the provided from a AttributeValue Map, and
         * completely replaces any previously set attributes.
         *
         * @param attributeValueMap - Map with Attributes as String keys and AttributeValue as Value.
         * @return Builder instance to construct a {@link EnhancedDocument}
         */
        Builder attributeValueMap(Map<String, AttributeValue> attributeValueMap);

        /**
         * Builds an instance of {@link EnhancedDocument}.
         *
         * @return instance of {@link EnhancedDocument} implementation.
         */
        EnhancedDocument build();
    }

}
