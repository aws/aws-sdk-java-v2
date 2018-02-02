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

package software.amazon.awssdk.services.dynamodb.datamodeling.unmarshallers;

import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * An unmarshaller that unmarshals DynamoDB NumberSets into sets of Java
 * {@code Double}s.
 */
public class DoubleSetUnmarshaller extends NsUnmarshaller {

    private static final DoubleSetUnmarshaller INSTANCE =
            new DoubleSetUnmarshaller();

    private DoubleSetUnmarshaller() {
    }

    public static DoubleSetUnmarshaller instance() {
        return INSTANCE;
    }

    @Override
    public Object unmarshall(AttributeValue value) {
        Set<Double> result = new HashSet<Double>();
        for (String s : value.ns()) {
            result.add(Double.valueOf(s));
        }
        return result;
    }
}
