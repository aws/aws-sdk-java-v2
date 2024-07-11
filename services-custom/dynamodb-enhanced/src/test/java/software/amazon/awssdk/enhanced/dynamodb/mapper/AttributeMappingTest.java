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

package software.amazon.awssdk.enhanced.dynamodb.mapper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeMapping.NESTED;
import static software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeMapping.SHALLOW;

import org.junit.jupiter.api.Test;

public class AttributeMappingTest {
    private static String SHALLOW_MAPPING = "SHALLOW";
    private static String NESTED_MAPPING = "NESTED";

    @Test
    public void whenPassedString_returnsAttributeMapping() {
        assertEquals(SHALLOW, AttributeMapping.fromValue(SHALLOW_MAPPING));
        assertEquals(NESTED, AttributeMapping.fromValue(NESTED_MAPPING));
    }

    @Test
    public void whenPassedAttributeMapping_returnsString() {
        assertEquals(SHALLOW_MAPPING, AttributeMapping.toString(SHALLOW));
        assertEquals(NESTED_MAPPING, AttributeMapping.toString(NESTED));
    }
}
