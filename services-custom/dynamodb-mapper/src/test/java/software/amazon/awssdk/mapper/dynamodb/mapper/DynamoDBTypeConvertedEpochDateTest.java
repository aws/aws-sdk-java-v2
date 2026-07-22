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
package software.amazon.awssdk.mapper.dynamodb.mapper;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBHashKey;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBMapper;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTable;
import software.amazon.awssdk.mapper.dynamodb.DynamoDBTypeConvertedEpochDate;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import software.amazon.awssdk.mapper.dynamodb.test.AWSIntegrationTestBase;
import com.amazonaws.util.ImmutableMapParameter;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests to ensure {@link Date}, {@link Calendar}, and {@link DateTime} objects can be coerced to a numeric Dynamo DB attribute
 * via the {@link DynamoDBTypeConvertedEpochDate} annontation.
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamoDBTypeConvertedEpochDateTest extends AWSIntegrationTestBase {

    private static final String HASH_KEY = "1234";

    private DynamoDBMapper mapper;

    @Mock
    private AmazonDynamoDB ddb;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mapper = new DynamoDBMapper(ddb);
        // Just stub dummy response for all save related tests
        when(ddb.updateItem(any(UpdateItemRequest.class))).thenReturn(new UpdateItemResult());
    }

    @Test
    public void saveItem_DateSentAsNumericUpdate() {
        Date date = new Date();
        mapper.save(new PojoWithDate()
                            .setHashKey(UUID.randomUUID().toString())
                            .setDate(date));
        verifyAttributeUpdatedWithValue("date", new AttributeValue().withN(String.valueOf(date.getTime())));
    }

    @Test
    public void saveItem_CalendarSentAsNumericUpdate() {
        Calendar calendar = Calendar.getInstance();
        mapper.save(new PojoWithDate()
                            .setHashKey(UUID.randomUUID().toString())
                            .setCalendar(calendar));
        verifyAttributeUpdatedWithValue("calendar", new AttributeValue().withN(String.valueOf(calendar.getTime().getTime())));
    }

    @Test
    public void saveItem_DateTimeSentAsNumericUpdate() {
        DateTime dateTime = new DateTime();
        mapper.save(new PojoWithDate()
                            .setHashKey(UUID.randomUUID().toString())
                            .setDateTime(dateTime));
        verifyAttributeUpdatedWithValue("dateTime", new AttributeValue().withN(String.valueOf(dateTime.toDate().getTime())));
    }

    @Test
    public void getItem_WithNumericDateInResponse_UnmarshalledCorrectly() {
        stubGetItemRequest("date", new AttributeValue().withN("1234"));
        final PojoWithDate pojo = loadPojo();
        assertThat(pojo.getDate().getTime(), equalTo(1234L));
    }

    @Test
    public void getItem_WithNumericCalendarInResponse_UnmarshalledCorrectly() {
        stubGetItemRequest("calendar", new AttributeValue().withN("1234"));
        final PojoWithDate pojo = loadPojo();
        assertThat(pojo.getCalendar().getTime().getTime(), equalTo(1234L));
    }

    @Test
    public void getItem_WithNumericDateTimeInResponse_UnmarshalledCorrectly() {
        stubGetItemRequest("dateTime", new AttributeValue().withN("1234"));
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
     * Create a {@link GetItemResult} with the hash key value ({@value #HASH_KEY} and the additional attribute.
     *
     * @param attributeName  Additional attribute to include in created {@link GetItemResult}.
     * @param attributeValue Value of additional attribute.
     */
    private GetItemResult createGetItemResult(String attributeName, AttributeValue attributeValue) {
        return new GetItemResult().withItem(
                ImmutableMapParameter.of("hashKey", new AttributeValue(HASH_KEY),
                                         attributeName, attributeValue));
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
        assertEquals(expected, updateItemRequestCaptor.getValue().getAttributeUpdates().get(attributeName).getValue());
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
