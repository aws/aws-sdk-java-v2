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

package software.amazon.awssdk.mapper.dynamodb.mapper;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConvertedEpochDate;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests to ensure {@link Date}, {@link Calendar}, and {@link DateTime} objects can be coerced to a numeric Dynamo DB attribute
 * via the {@link DynamoDBTypeConvertedEpochDate} annontation.
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamoDBTypeConvertedEpochDateTest {

    private static final String HASH_KEY = "1234";

    private DynamoDBMapper mapper;

    @Mock
    private DynamoDbClient ddb;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mapper = new DynamoDBMapper(ddb);
        // Just stub dummy response for all save related tests
        when(ddb.updateItem(any(UpdateItemRequest.class))).thenReturn(UpdateItemResponse.builder().build());
    }

    @Test
    public void saveItem_DateSentAsNumericUpdate() {
        Date date = new Date();
        mapper.save(new PojoWithDate()
                            .setHashKey(UUID.randomUUID().toString())
                            .setDate(date));
        verifyAttributeUpdatedWithValue("date", AttributeValue.builder().n(String.valueOf(date.getTime())).build());
    }

    @Test
    public void saveItem_CalendarSentAsNumericUpdate() {
        Calendar calendar = Calendar.getInstance();
        mapper.save(new PojoWithDate()
                            .setHashKey(UUID.randomUUID().toString())
                            .setCalendar(calendar));
        verifyAttributeUpdatedWithValue("calendar", AttributeValue.builder().n(String.valueOf(calendar.getTime().getTime())).build());
    }

    @Test
    public void saveItem_DateTimeSentAsNumericUpdate() {
        DateTime dateTime = new DateTime();
        mapper.save(new PojoWithDate()
                            .setHashKey(UUID.randomUUID().toString())
                            .setDateTime(dateTime));
        verifyAttributeUpdatedWithValue("dateTime", AttributeValue.builder().n(String.valueOf(dateTime.toDate().getTime())).build());
    }

    @Test
    public void getItem_WithNumericDateInResponse_UnmarshalledCorrectly() {
        stubGetItemRequest("date", AttributeValue.builder().n("1234").build());
        final PojoWithDate pojo = loadPojo();
        assertThat(pojo.getDate().getTime(), equalTo(1234L));
    }

    @Test
    public void getItem_WithNumericCalendarInResponse_UnmarshalledCorrectly() {
        stubGetItemRequest("calendar", AttributeValue.builder().n("1234").build());
        final PojoWithDate pojo = loadPojo();
        assertThat(pojo.getCalendar().getTime().getTime(), equalTo(1234L));
    }

    @Test
    public void getItem_WithNumericDateTimeInResponse_UnmarshalledCorrectly() {
        stubGetItemRequest("dateTime", AttributeValue.builder().n("1234").build());
        final PojoWithDate pojo = loadPojo();
        assertThat(pojo.getDateTime().toDate().getTime(), equalTo(1234L));
    }

    private PojoWithDate loadPojo() {
        return mapper.load(new PojoWithDate().setHashKey(HASH_KEY));
    }

    /**
     * Stub a call to getItem to return a result with the given attribute value in the item.
     *
     * @param attributeName  Attribute name to return in result (in addition to hash key)
     * @param attributeValue Attribute value to return in result (in addition to hash key)
     */
    private void stubGetItemRequest(String attributeName, AttributeValue attributeValue) {
        when(ddb.getItem(any(GetItemRequest.class))).thenReturn(createGetItemResult(attributeName, attributeValue));
    }

    /**
     * Create a {@link GetItemResponse} with the hash key value ({@value #HASH_KEY} and the additional attribute.
     *
     * @param attributeName  Additional attribute to include in created {@link GetItemResponse}.
     * @param attributeValue Value of additional attribute.
     */
    private GetItemResponse createGetItemResult(String attributeName, AttributeValue attributeValue) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("hashKey", AttributeValue.builder().s(HASH_KEY).build());
        item.put(attributeName, attributeValue);
        return GetItemResponse.builder().item(item).build();
    }

    /**
     * Verifies the mapper results in an update item call that has an update for the appropriate attribute.
     *
     * @param attributeName Attribute expected to be updated.
     * @param expected      Expected value of update action.
     */
    private void verifyAttributeUpdatedWithValue(String attributeName, AttributeValue expected) {
        ArgumentCaptor<UpdateItemRequest> updateItemRequestCaptor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(ddb).updateItem(updateItemRequestCaptor.capture());
        assertEquals(expected, updateItemRequestCaptor.getValue().attributeUpdates().get(attributeName).value());
    }

    @DynamoDBTable(tableName = "PojoWithDate")
    public static class PojoWithDate {

        @DynamoDBHashKey
        private String hashKey;

        @DynamoDBTypeConvertedEpochDate
        private Date date;

        @DynamoDBTypeConvertedEpochDate
        private Calendar calendar;

        @DynamoDBTypeConvertedEpochDate
        private DateTime dateTime;


        public String getHashKey() {
            return hashKey;
        }

        public PojoWithDate setHashKey(String hashKey) {
            this.hashKey = hashKey;
            return this;
        }

        public Date getDate() {
            return date;
        }

        public PojoWithDate setDate(Date date) {
            this.date = date;
            return this;
        }

        public Calendar getCalendar() {
            return calendar;
        }

        public PojoWithDate setCalendar(Calendar calendar) {
            this.calendar = calendar;
            return this;
        }

        public DateTime getDateTime() {
            return dateTime;
        }

        public PojoWithDate setDateTime(DateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }
    }
}
