/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.datamodeling;

import java.lang.reflect.Method;
import java.text.ParseException;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Unmarshaller interface to make it possible to cache the expensive
 * type-determination behavior necessary when turning a service result back
 * into an object.
 */
public interface ArgumentUnmarshaller {

    /**
     * Asserts that the value given can be processed using the setter given.
     */
    void typeCheck(AttributeValue value, Method setter);

    /**
     * Unmarshalls the {@link AttributeValue} given into an instance of the
     * appropriate type, as determined by  {@link DynamoDbMapper}
     *
     * @throws ParseException when unable to parse a date string
     */
    Object unmarshall(AttributeValue value) throws ParseException;
}
