/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.dynamodb.mapper;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.pojos.StringSetAttributeClass;


/**
 * Tests string set attributes
 */
public class StringSetAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String ORIGINAL_NAME_ATTRIBUTE = "originalName";
    private static final String STRING_SET_ATTRIBUTE = "stringSetAttribute";
    private static final String EXTRA_ATTRIBUTE = "extra";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();

    // Test data
    static {
        for (int i = 0; i < 5; i++) {
            Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
            attr.put(KEY_NAME, AttributeValue.builder().s("" + startKey++).build());
            attr.put(STRING_SET_ATTRIBUTE, AttributeValue.builder().ss("" + ++startKey, "" + ++startKey, "" + ++startKey).build());
            attr.put(ORIGINAL_NAME_ATTRIBUTE, AttributeValue.builder().ss("" + ++startKey, "" + ++startKey, "" + ++startKey).build());
            attr.put(EXTRA_ATTRIBUTE, AttributeValue.builder().ss("" + ++startKey, "" + ++startKey, "" + ++startKey).build());
            attrs.add(attr);
        }
    }

    ;

    @BeforeClass
    public static void setUp() throws Exception {
        DynamoDBMapperIntegrationTestBase.setUp();

        // Insert the data
        for (Map<String, AttributeValue> attr : attrs) {
            dynamo.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(attr).build());
        }
    }

    @Test
    public void testLoad() throws Exception {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        for (Map<String, AttributeValue> attr : attrs) {
            StringSetAttributeClass x = util.load(StringSetAttributeClass.class, attr.get(KEY_NAME).s());
            assertEquals(x.getKey(), attr.get(KEY_NAME).s());
            assertSetsEqual(x.getStringSetAttribute(), toSet(attr.get(STRING_SET_ATTRIBUTE).ss()));
            assertSetsEqual(x.getStringSetAttributeRenamed(), toSet(attr.get(ORIGINAL_NAME_ATTRIBUTE).ss()));
        }
    }

    /**
     * Tests saving only some attributes of an object.
     */
    @Test
    public void testIncompleteObject() {
        DynamoDbMapper util = new DynamoDbMapper(dynamo);

        StringSetAttributeClass obj = getUniqueObject();
        obj.setStringSetAttribute(null);
        util.save(obj);

        assertEquals(obj, util.load(StringSetAttributeClass.class, obj.getKey()));

        obj.setStringSetAttributeRenamed(null);
        util.save(obj);
        assertEquals(obj, util.load(StringSetAttributeClass.class, obj.getKey()));
    }

    @Test
    public void testSave() throws Exception {
        List<StringSetAttributeClass> objs = new ArrayList<StringSetAttributeClass>();
        for (int i = 0; i < 5; i++) {
            StringSetAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (StringSetAttributeClass obj : objs) {
            util.save(obj);
        }

        for (StringSetAttributeClass obj : objs) {
            StringSetAttributeClass loaded = util.load(StringSetAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }

    @Test
    public void testUpdate() throws Exception {
        List<StringSetAttributeClass> objs = new ArrayList<StringSetAttributeClass>();
        for (int i = 0; i < 5; i++) {
            StringSetAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (StringSetAttributeClass obj : objs) {
            util.save(obj);
        }

        for (StringSetAttributeClass obj : objs) {
            StringSetAttributeClass replacement = getUniqueObject();
            replacement.setKey(obj.getKey());
            util.save(replacement);

            assertEquals(replacement, util.load(StringSetAttributeClass.class, obj.getKey()));
        }
    }

    private StringSetAttributeClass getUniqueObject() {
        StringSetAttributeClass obj = new StringSetAttributeClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setStringSetAttribute(toSet(String.valueOf(startKey++), String.valueOf(startKey++), String.valueOf(startKey++)));
        obj.setStringSetAttributeRenamed(
                toSet(String.valueOf(startKey++), String.valueOf(startKey++), String.valueOf(startKey++)));
        return obj;
    }

}
