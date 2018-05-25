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

package software.amazon.awssdk.core.protocol.json;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkServiceException;

public class SdkJsonErrorUnmarshallerTest {

    private static final JsonNode JSON = new ObjectMapper().createObjectNode().put("message", "Some error message")
                                                           .put("foo", "value");

    private SdkJsonErrorUnmarshaller unmarshaller = new SdkJsonErrorUnmarshaller(CustomException.class, Optional.of(423));

    @Test
    public void matchingErrorCode_ReturnsTrueForMatches() {
        assertTrue(unmarshaller.matches(423));
    }

    @Test
    public void nonMatchingErrorCode_ReturnsFalseForMatches() {
        assertFalse(unmarshaller.matches(500));
    }

    @Test
    public void jsonObjectWithData_UnmarshallsIntoModeledException() throws Exception {
        SdkServiceException exception = unmarshaller.unmarshall(JSON);
        assertThat(((CustomException) exception).getFoo()).isEqualTo("value");
    }

    @Test
    public void emptyJsonObject_UnmarshallsIntoModeledException() throws Exception {
        final SdkServiceException exception = unmarshaller
            .unmarshall(new ObjectMapper().readTree("{}"));
        assertThat(exception).isInstanceOf(CustomException.class);
        assertThat(((CustomException) exception).getFoo()).isNull();
    }

    @Test
    public void nullHttpStatusCodeMatchesAllExceptions() {
        SdkJsonErrorUnmarshaller allMatchUnmarshaller = new SdkJsonErrorUnmarshaller(
            CustomException.class, Optional.empty());
        assertTrue(allMatchUnmarshaller.matches(400));
        assertTrue(allMatchUnmarshaller.matches(500));
    }

    private static class CustomException extends SdkServiceException {

        private static final long serialVersionUID = 4140670458615826397L;

        private String foo;

        public CustomException(BeanStyleBuilder builder) {
            super(builder.message);
            this.foo = builder.foo;
        }

        public String getFoo() {
            return foo;
        }


        public static Class<?> serializableBuilderClass() {
            return BeanStyleBuilder.class;
        }

        public interface Builder {
            Builder foo(String foo);

            CustomException build();
        }

        private static class BeanStyleBuilder implements Builder {
            private String foo;
            private String message;

            @Override
            public Builder foo(String foo) {
                this.foo = foo;
                return this;
            }

            @JsonProperty("foo")
            public void setFoo(String foo) {
                this.foo = foo;
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
