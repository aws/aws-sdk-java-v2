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

package software.amazon.awssdk.awscore.protocol.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.awscore.internal.protocol.json.AwsJsonErrorUnmarshaller;

public class AwsJsonErrorUnmarshallerTest {

    private static final String ERROR_TYPE = "CustomException";

    private static final JsonNode JSON = new ObjectMapper().createObjectNode().put("message", "Some error message")
                                                           .put("__type", "apiVersion#" + ERROR_TYPE).put("CustomField", "This is a customField").put("CustomInt", 42);

    private static final JsonNode INVALID_CASE_JSON = new ObjectMapper().createObjectNode()
                                                                        .put("message", "Some error message").put("__type", "apiVersion#" + ERROR_TYPE)
                                                                        .put("customField", "This is a customField").put("customInt", 42);

    private AwsJsonErrorUnmarshaller unmarshaller;

    @Before
    public void setup() {
        unmarshaller = new AwsJsonErrorUnmarshaller(CustomException.class, ERROR_TYPE);
    }

    @Test
    public void unmarshall_ValidJsonContent_UnmarshallsCorrectly() throws Exception {
        CustomException exception = (CustomException) unmarshaller.unmarshall(JSON);
        assertEquals("Some error message", exception.errorMessage());
        assertEquals("This is a customField", exception.getCustomField());
        assertEquals(Integer.valueOf(42), exception.getCustomInt());
    }

    @Test
    public void unmarshall_InvalidCaseJsonContent_DoesNotUnmarshallCustomFields() throws Exception {
        CustomException exception = (CustomException) unmarshaller.unmarshall(INVALID_CASE_JSON);
        assertEquals("Some error message", exception.errorMessage());
        assertNull(exception.getCustomField());
        assertNull(exception.getCustomInt());
    }

    @Test
    public void match_DefaultUnmarshaller_MatchesEverything() {
        unmarshaller = AwsJsonErrorUnmarshaller.DEFAULT_UNMARSHALLER;
        assertTrue(unmarshaller.matchErrorCode(null));
        assertTrue(unmarshaller.matchErrorCode(""));
        assertTrue(unmarshaller.matchErrorCode("someErrorCode"));
    }

    @Test
    public void match_MatchingErrorCode_ReturnsTrue() throws Exception {
        assertTrue(unmarshaller.matchErrorCode(ERROR_TYPE));
    }

    @Test
    public void match_NonMatchingErrorCode_ReturnsFalse() throws Exception {
        assertFalse(unmarshaller.matchErrorCode("NonMatchingErrorCode"));
    }

    @Test
    public void match_NullErrorCode_ReturnsFalse() throws Exception {
        assertFalse(unmarshaller.matchErrorCode(null));
    }

    private static class CustomException extends AwsServiceException {

        private static final long serialVersionUID = 4140670458615826397L;

        private String customField;
        private Integer customInt;

        public CustomException(BeanStyleBuilder builder) {
            super(builder.message);
            this.customField = builder.customField;
            this.customInt = builder.customInt;
        }

        public String getCustomField() {
            return customField;
        }


        public Integer getCustomInt() {
            return customInt;
        }

        public static Class<?> serializableBuilderClass() {
            return BeanStyleBuilder.class;
        }

        public interface Builder {
            Builder customField(String customField);
            Builder customInt(Integer customInt);

            CustomException build();
        }

        private static class BeanStyleBuilder implements Builder {
            private String customField;
            private Integer customInt;
            private String message;

            @Override
            public Builder customField(String customField) {
                this.customField = customField;
                return this;
            }

            public void setCustomField(String customField) {
                this.customField = customField;
            }

            @Override
            public Builder customInt(Integer customInt) {
                this.customInt = customInt;
                return this;
            }

            public void setCustomInt(Integer customInt) {
                this.customInt = customInt;
            }

            @JsonProperty("message")
            public void setMessage(String message) {
                this.message = message;
            }

            @Override
            public CustomException build() {
                return new CustomException(this);
            }
        }

    }
}
