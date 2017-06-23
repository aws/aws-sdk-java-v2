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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDBMapperIntegrationTestBase;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbHashKey;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.ConsistentReads;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapperConfig.SaveBehavior;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.datamodeling.DynamoDbMapper;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.pojos.StringAttributeClass;

/**
 * Tests simple string attributes
 */
public class SimpleStringAttributesIntegrationTest extends DynamoDBMapperIntegrationTestBase {

    private static final String ORIGINAL_NAME_ATTRIBUTE = "originalName";
    private static final String STRING_ATTRIBUTE = "stringAttribute";
    private static final List<Map<String, AttributeValue>> attrs = new LinkedList<Map<String, AttributeValue>>();

    // Test data
    static {
        for (int i = 0; i < 5; i++) {
            Map<String, AttributeValue> attr = new HashMap<String, AttributeValue>();
            attr.put(KEY_NAME, AttributeValue.builder().s("" + startKey++).build());
            attr.put(STRING_ATTRIBUTE, AttributeValue.builder().s("" + startKey++).build());
            attr.put(ORIGINAL_NAME_ATTRIBUTE, AttributeValue.builder().s("" + startKey++).build());
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
            StringAttributeClass x = util.load(StringAttributeClass.class, attr.get(KEY_NAME).s());
            assertEquals(x.getKey(), attr.get(KEY_NAME).s());
            assertEquals(x.getStringAttribute(), attr.get(STRING_ATTRIBUTE).s());
            assertEquals(x.getRenamedAttribute(), attr.get(ORIGINAL_NAME_ATTRIBUTE).s());
        }

    }

    @Test
    public void testSave() {
        List<StringAttributeClass> objs = new ArrayList<StringAttributeClass>();
        for (int i = 0; i < 5; i++) {
            StringAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (StringAttributeClass obj : objs) {
            util.save(obj);
        }

        for (StringAttributeClass obj : objs) {
            StringAttributeClass loaded = util.load(StringAttributeClass.class, obj.getKey());
            assertEquals(obj, loaded);
        }
    }

    /**
     * Tests saving an incomplete object into DynamoDB
     */
    @Test
    public void testIncompleteObject() {
        StringAttributeClass obj = getUniqueObject();
        obj.setStringAttribute(null);
        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        util.save(obj);

        assertEquals(obj, util.load(StringAttributeClass.class, obj.getKey()));

        // test removing an attribute
        assertNotNull(obj.getRenamedAttribute());
        obj.setRenamedAttribute(null);
        util.save(obj);
        assertEquals(obj, util.load(StringAttributeClass.class, obj.getKey()));
    }

    @Test
    public void testUpdate() {
        List<StringAttributeClass> objs = new ArrayList<StringAttributeClass>();
        for (int i = 0; i < 5; i++) {
            StringAttributeClass obj = getUniqueObject();
            objs.add(obj);
        }

        DynamoDbMapper util = new DynamoDbMapper(dynamo);
        for (StringAttributeClass obj : objs) {
            util.save(obj);
        }

        for (StringAttributeClass obj : objs) {
            StringAttributeClass replacement = getUniqueObject();
            replacement.setKey(obj.getKey());
            util.save(replacement);

            assertEquals(replacement, util.load(StringAttributeClass.class, obj.getKey()));
        }
    }

    @Test
    public void testSaveOnlyKey() {
        KeyOnly obj = new KeyOnly();
        obj.setKey("" + startKey++);
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        mapper.save(obj);

        KeyOnly loaded = mapper.load(KeyOnly.class, obj.getKey(), new DynamoDbMapperConfig(ConsistentReads.CONSISTENT));
        assertEquals(obj, loaded);

        // saving again shouldn't be an error
        mapper.save(obj);
    }

    @Test
    public void testSaveOnlyKeyClobber() {
        KeyOnly obj = new KeyOnly();
        obj.setKey("" + startKey++);
        DynamoDbMapper mapper = new DynamoDbMapper(dynamo);
        mapper.save(obj, new DynamoDbMapperConfig(SaveBehavior.CLOBBER));

        KeyOnly loaded = mapper.load(KeyOnly.class, obj.getKey(), new DynamoDbMapperConfig(ConsistentReads.CONSISTENT));
        assertEquals(obj, loaded);

        // saving again shouldn't be an error
        mapper.save(obj, new DynamoDbMapperConfig(SaveBehavior.CLOBBER));
    }

    private StringAttributeClass getUniqueObject() {
        StringAttributeClass obj = new StringAttributeClass();
        obj.setKey(String.valueOf(startKey++));
        obj.setRenamedAttribute(String.valueOf(startKey++));
        obj.setStringAttribute(String.valueOf(startKey++));
        return obj;
    }

    @DynamoDbTable(tableName = "aws-java-sdk-util")
    public static final class KeyOnly {
        private String key;

        @DynamoDbHashKey
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            KeyOnly other = (KeyOnly) obj;
            if (key == null) {
                if (other.key != null) {
                    return false;
                }
            } else if (!key.equals(other.key)) {
                return false;
            }
            return true;
        }
    }

}
