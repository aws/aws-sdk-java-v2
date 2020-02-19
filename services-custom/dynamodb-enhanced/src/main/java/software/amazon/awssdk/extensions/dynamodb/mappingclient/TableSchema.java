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

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.util.Collection;
import java.util.Map;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Interface for a mapper that is capable of mapping a modelled Java object into a map of {@link AttributeValue} that is
 * understood by the DynamoDb low-level SDK and back again. This object is also expected to know about the
 * structure of the table it is modelling, which is stored in a {@link TableMetadata} object.
 *
 * @param <T> The type of model object that is being mapped to records in the DynamoDb table.
 */
@SdkPublicApi
public interface TableSchema<T> {
    /**
     * Takes a raw DynamoDb SDK representation of a record in a table and maps it to a Java object. A new object is
     * created to fulfil this operation.
     * <p>
     * If attributes are missing from the map, that will not cause an error, however if attributes are found in the
     * map which the mapper does not know how to map, an exception will be thrown.
     *
     * @param attributeMap A map of String to {@link AttributeValue} that contains all the raw attributes to map.
     * @return A new instance of a Java object with all the attributes mapped onto it.
     * @throws IllegalArgumentException if any attributes in the map could not be mapped onto the new model object.
     */
    T mapToItem(Map<String, AttributeValue> attributeMap);

    /**
     * Takes a modelled object and converts it into a raw map of {@link AttributeValue} that the DynamoDb low-level
     * SDK can work with.
     *
     * @param item The modelled Java object to convert into a map of attributes.
     * @param ignoreNulls If set to true; any null values in the Java object will not be added to the output map.
     *                    If set to false; null values in the Java object will be added as {@link AttributeValue} of
     *                    type 'nul' to the output map.
     * @return A map of String to {@link AttributeValue} representing all the modelled attributes in the model object.
     */
    Map<String, AttributeValue> itemToMap(T item, boolean ignoreNulls);

    /**
     * Takes a modelled object and extracts a specific set of attributes which are then returned as a map of
     * {@link AttributeValue} that the DynamoDb low-level SDK can work with. This method is typically used to extract
     * just the key attributes of a modelled item and will not ignore nulls on the modelled object.
     *
     * @param item The modelled Java object to extract the map of attributes from.
     * @param attributes A collection of attribute names to extract into the output map.
     * @return A map of String to {@link AttributeValue} representing the requested modelled attributes in the model
     * object.
     */
    Map<String, AttributeValue> itemToMap(T item, Collection<String> attributes);

    /**
     * Returns a single attribute value from the modelled object.
     *
     * @param item The modelled Java object to extract the attribute from.
     * @param key The attribute name describing which attribute to extract.
     * @return A single {@link AttributeValue} representing the requested modelled attribute in the model object or
     * null if the attribute has not been set with a value in the modelled object.
     */
    AttributeValue attributeValue(T item, String key);

    /**
     * Returns the object that describes the structure of the table being modelled by the mapper. This includes
     * information such as the table name, index keys and attribute tags.
     * @return A {@link TableMetadata} object that contains structural information about the table being modelled.
     */
    TableMetadata tableMetadata();
}
