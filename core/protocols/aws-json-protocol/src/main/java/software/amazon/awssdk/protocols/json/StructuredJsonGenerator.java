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

package software.amazon.awssdk.protocols.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Interface for generating a JSON
 */
@SdkProtectedApi
public interface StructuredJsonGenerator {

    /**
     * No-op implementation that ignores all calls and returns empty bytes from getBytes.
     */
    StructuredJsonGenerator NO_OP = new StructuredJsonGenerator() {

        @Override
        public StructuredJsonGenerator writeStartArray() {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeEndArray() {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeNull() {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeStartObject() {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeEndObject() {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeFieldName(String fieldName) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(String val) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(boolean bool) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(long val) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(double val) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(float val) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(short val) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(int val) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(ByteBuffer bytes) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(Instant instant) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(BigDecimal value) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeValue(BigInteger value) {
            return this;
        }

        @Override
        public StructuredJsonGenerator writeNumber(String number) {
            return this;
        }

        @Override
        public byte[] getBytes() {
            return null;
        }

        @Override
        public String getContentType() {
            return null;
        }
    };

    StructuredJsonGenerator writeStartArray();

    StructuredJsonGenerator writeEndArray();

    StructuredJsonGenerator writeNull();

    StructuredJsonGenerator writeStartObject();

    StructuredJsonGenerator writeEndObject();

    StructuredJsonGenerator writeFieldName(String fieldName);

    StructuredJsonGenerator writeValue(String val);

    StructuredJsonGenerator writeValue(boolean bool);

    StructuredJsonGenerator writeValue(long val);

    StructuredJsonGenerator writeValue(double val);

    StructuredJsonGenerator writeValue(float val);

    StructuredJsonGenerator writeValue(short val);

    StructuredJsonGenerator writeValue(int val);

    StructuredJsonGenerator writeValue(ByteBuffer bytes);

    StructuredJsonGenerator writeValue(Instant instant);

    StructuredJsonGenerator writeNumber(String number);

    StructuredJsonGenerator writeValue(BigDecimal value);

    StructuredJsonGenerator writeValue(BigInteger value);

    byte[] getBytes();

    /**
     * New clients use {@link SdkJsonProtocolFactory#getContentType()}.
     */
    @Deprecated
    String getContentType();
}
