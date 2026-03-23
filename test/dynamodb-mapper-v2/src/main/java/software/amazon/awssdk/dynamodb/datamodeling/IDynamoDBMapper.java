/*
 * Copyright 2015-2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package software.amazon.awssdk.dynamodb.datamodeling;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;

/**
 * Interface for DynamoDBMapper operations. Stripped to load path for POC.
 */
public interface IDynamoDBMapper {

    <T> DynamoDBMapperTableModel<T> getTableModel(Class<T> clazz);

    <T> DynamoDBMapperTableModel<T> getTableModel(Class<T> clazz, DynamoDBMapperConfig config);

    <T> T load(Class<T> clazz, Object hashKey, DynamoDBMapperConfig config);

    <T> T load(Class<T> clazz, Object hashKey);

    <T> T load(Class<T> clazz, Object hashKey, Object rangeKey);

    <T> T load(Class<T> clazz, Object hashKey, Object rangeKey, DynamoDBMapperConfig config);

    <T> T load(T keyObject);

    <T> T load(T keyObject, DynamoDBMapperConfig config);

    <T> T marshallIntoObject(Class<T> clazz, Map<String, AttributeValue> itemAttributes);
}
