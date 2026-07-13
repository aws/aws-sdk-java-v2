/*
 * Copyright 2011-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.services.dynamodbv2.mapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedJson;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.util.ImmutableMapParameter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TypeConvertedJsonTest {

    private static final String HASH_KEY = "1234";

    @Mock
    private AmazonDynamoDB ddb;

    @Test
    public void responseWithUnmappedField_IgnoresUnknownFieldAndUnmarshallsCorrectly() {
        final DynamoDBMapper mapper = new DynamoDBMapper(ddb);
        when(ddb.getItem(any(GetItemRequest.class)))
                .thenReturn(new GetItemResult().withItem(
                        ImmutableMapParameter.of("hashKey", new AttributeValue(HASH_KEY),
                                                 "jsonMappedPojo", new AttributeValue(
                                        "{\"knownField\": \"knownValue\", \"unknownField\": \"unknownValue\"}")
                        )));

        final TopLevelPojo pojo = mapper.load(new TopLevelPojo().setHashKey(HASH_KEY));
        assertEquals("knownValue", pojo.getJsonMappedPojo().getKnownField());
    }

    @DynamoDBTable(tableName = "TestTable")
    public static class TopLevelPojo {

        @DynamoDBHashKey
        private String hashKey;

        @DynamoDBTypeConvertedJson
        private JsonMappedPojo jsonMappedPojo;

        public String getHashKey() {
            return hashKey;
        }

        public TopLevelPojo setHashKey(String hashKey) {
            this.hashKey = hashKey;
            return this;
        }

        public JsonMappedPojo getJsonMappedPojo() {
            return jsonMappedPojo;
        }

        public void setJsonMappedPojo(JsonMappedPojo jsonMappedPojo) {
            this.jsonMappedPojo = jsonMappedPojo;
        }
    }

    public static class JsonMappedPojo {

        private String knownField;

        public String getKnownField() {
            return knownField;
        }

        public void setKnownField(String knownField) {
            this.knownField = knownField;
        }
    }

}
