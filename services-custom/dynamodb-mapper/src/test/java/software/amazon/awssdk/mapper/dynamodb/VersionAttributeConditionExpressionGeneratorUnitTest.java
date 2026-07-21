/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights
 * Reserved.
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
package software.amazon.awssdk.mapper.dynamodb;

import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getMultiVersionRangeKeyObject;
import static software.amazon.awssdk.mapper.dynamodb.TestObjectCreator.getUniqueRangeKeyObject;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import software.amazon.awssdk.mapper.dynamodb.pojos.MultiVersionRangeKeyClass;
import software.amazon.awssdk.mapper.dynamodb.pojos.RangeKeyClass;
import org.junit.Test;

public class VersionAttributeConditionExpressionGeneratorUnitTest {

    private static final DynamoDBMapperModelFactory factory = StandardModelFactories.of(S3Link.Factory.of(null));
    private static final DynamoDBMapperModelFactory.TableFactory models =
            factory.getTableFactory(DynamoDBMapperConfig.DEFAULT);

    @SuppressWarnings("unchecked")
    private static <T> DynamoDBMapperTableModel<T> getTable(T object) {
        return models.getTable((Class<T>)object.getClass());
    }

    @Test
    public void testExpressionGeneratedForOneNullValuedVersion() {
        VersionAttributeConditionExpressionGenerator versionAttributeConditionExpressionGenerator =
                new VersionAttributeConditionExpressionGenerator();
        Object obj = getUniqueRangeKeyObject();
        final DynamoDBMapperTableModel<Object> model = getTable((Object)obj);
        DynamoDBMapperFieldModel<Object,Object> versionedField = model.field("version");
        Object fieldValue = versionedField.get(obj);
        versionAttributeConditionExpressionGenerator.appendVersionAttributeToConditionExpression(versionedField,
                                                                                                 fieldValue);
        String expectedConditionExpression = "attribute_not_exists(#versionAttributeName1)";
        Map<String, String> expectedExpressionAttributeNames = new HashMap<String, String>();
        expectedExpressionAttributeNames.put("#versionAttributeName1", "version");
        assertVersionAttributeConditionExpression(versionAttributeConditionExpressionGenerator
                                                          .getVersionAttributeConditionExpression(),
                                                  expectedConditionExpression,
                                                  expectedExpressionAttributeNames,
                                                  null /* expectedExpressionAttributeValues */);
    }

    @Test
    public void testExpressionGeneratedForOneNonNullValuedVersion() {
        VersionAttributeConditionExpressionGenerator versionAttributeConditionExpressionGenerator =
                new VersionAttributeConditionExpressionGenerator();
        RangeKeyClass obj = getUniqueRangeKeyObject();
        final DynamoDBMapperTableModel<Object> model = getTable((Object)obj);
        obj.setVersion(1L);
        DynamoDBMapperFieldModel<Object,Object> versionedField = model.field("version");
        Object fieldValue = versionedField.get(obj);
        versionAttributeConditionExpressionGenerator.appendVersionAttributeToConditionExpression(versionedField,
                                                                                                 fieldValue);
        String expectedConditionExpression = "attribute_exists(#versionAttributeName1) " +
                                                     "AND #versionAttributeName1 = :versionAttributeValue1";
        Map<String, String> expectedExpressionAttributeNames = new HashMap<String, String>();
        expectedExpressionAttributeNames.put("#versionAttributeName1", "version");
        Map<String, AttributeValue> expectedExpressionAttributeValues = new HashMap<String, AttributeValue>();
        expectedExpressionAttributeValues.put(":versionAttributeValue1", versionedField.convert(fieldValue));
        assertVersionAttributeConditionExpression(versionAttributeConditionExpressionGenerator
                                                          .getVersionAttributeConditionExpression(),
                                                  expectedConditionExpression,
                                                  expectedExpressionAttributeNames,
                                                  expectedExpressionAttributeValues);
    }

    @Test
    public void testExpressionGeneratedForMultipleNullValuedVersions() {
        VersionAttributeConditionExpressionGenerator versionAttributeConditionExpressionGenerator =
                new VersionAttributeConditionExpressionGenerator();
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        final DynamoDBMapperTableModel<Object> model = getTable((Object)obj);
        DynamoDBMapperFieldModel<Object,Object> versionedField1 = model.field("version");
        Object fieldValue1 = versionedField1.get(obj);
        versionAttributeConditionExpressionGenerator.appendVersionAttributeToConditionExpression(versionedField1,
                                                                                                 fieldValue1);
        DynamoDBMapperFieldModel<Object,Object> versionedField2 = model.field("version2");
        Object fieldValue2 = versionedField2.get(obj);
        versionAttributeConditionExpressionGenerator.appendVersionAttributeToConditionExpression(versionedField2,
                                                                                                 fieldValue2);
        Map<String, String> expectedExpressionAttributeNames = new HashMap<String, String>();
        expectedExpressionAttributeNames.put("#versionAttributeName1", "version");
        expectedExpressionAttributeNames.put("#versionAttributeName2", "version2");
        String expectedConditionExpression = "(attribute_not_exists(#versionAttributeName1)) " +
                                                     "AND (attribute_not_exists(#versionAttributeName2))";
        assertVersionAttributeConditionExpression(versionAttributeConditionExpressionGenerator
                                                          .getVersionAttributeConditionExpression(),
                                                  expectedConditionExpression,
                                                  expectedExpressionAttributeNames,
                                                  null /* expectedExpressionAttributeValues */);
    }

    @Test
    public void testExpressionGeneratedForMultipleNonNullValuedVersion() {
        VersionAttributeConditionExpressionGenerator versionAttributeConditionExpressionGenerator =
                new VersionAttributeConditionExpressionGenerator();
        MultiVersionRangeKeyClass obj = getMultiVersionRangeKeyObject();
        final DynamoDBMapperTableModel<Object> model = getTable((Object)obj);
        obj.setVersion(1L);
        obj.setVersion2(1L);
        DynamoDBMapperFieldModel<Object,Object> versionedField1 = model.field("version");
        Object fieldValue1 = versionedField1.get(obj);
        versionAttributeConditionExpressionGenerator.appendVersionAttributeToConditionExpression(versionedField1,
                                                                                                 fieldValue1);
        DynamoDBMapperFieldModel<Object,Object> versionedField2 = model.field("version2");
        Object fieldValue2 = versionedField2.get(obj);
        versionAttributeConditionExpressionGenerator.appendVersionAttributeToConditionExpression(versionedField2,
                                                                                                 fieldValue2);
        Map<String, String> expectedExpressionAttributeNames = new HashMap<String, String>();
        expectedExpressionAttributeNames.put("#versionAttributeName1", "version");
        expectedExpressionAttributeNames.put("#versionAttributeName2", "version2");
        Map<String, AttributeValue> expectedExpressionAttributeValues = new HashMap<String, AttributeValue>();
        expectedExpressionAttributeValues.put(":versionAttributeValue1", versionedField1.convert(fieldValue1));
        expectedExpressionAttributeValues.put(":versionAttributeValue2", versionedField1.convert(fieldValue2));
        String expectedConditionExpression =
                "(attribute_exists(#versionAttributeName1) AND #versionAttributeName1 = :versionAttributeValue1) " +
                        "AND (attribute_exists(#versionAttributeName2) AND #versionAttributeName2 = :versionAttributeValue2)";
        assertVersionAttributeConditionExpression(versionAttributeConditionExpressionGenerator
                                                          .getVersionAttributeConditionExpression(),
                                                  expectedConditionExpression,
                                                  expectedExpressionAttributeNames,
                                                  expectedExpressionAttributeValues);
    }

    private void assertVersionAttributeConditionExpression(
            DynamoDBTransactionWriteExpression actualVersionAttributeConditionExpression,
            String expectedConditionExpression,
            Map<String, String> expectedExpressionAttributeNames,
            Map<String, AttributeValue> expectedExpressionAttributeValues) {
        assertEquals(expectedConditionExpression, actualVersionAttributeConditionExpression.getConditionExpression());
        assertEquals(expectedExpressionAttributeNames,
                     actualVersionAttributeConditionExpression.getExpressionAttributeNames());
        assertEquals(expectedExpressionAttributeValues,
                     actualVersionAttributeConditionExpression.getExpressionAttributeValues());
    }
}
