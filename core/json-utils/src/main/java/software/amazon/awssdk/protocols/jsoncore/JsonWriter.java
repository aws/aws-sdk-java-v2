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

package software.amazon.awssdk.protocols.jsoncore;

import static software.amazon.awssdk.protocols.jsoncore.JsonNodeParser.DEFAULT_JSON_FACTORY;
import static software.amazon.awssdk.utils.DateUtils.formatUnixTimestampInstant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.core.JsonGenerator;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Thin wrapper around Jackson's JSON generator.
 */
@SdkProtectedApi
public class JsonWriter implements SdkAutoCloseable {

    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private final JsonFactory jsonFactory;
    private final ByteArrayOutputStream baos;
    private final JsonGenerator generator;

    private JsonWriter(Builder builder) {
        jsonFactory = builder.jsonFactory != null ? builder.jsonFactory : DEFAULT_JSON_FACTORY;
        try {
            baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
            generator = jsonFactory.createGenerator(baos);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
    }

    public static JsonWriter create() {
        return builder().build();
    }

    public static JsonWriter.Builder builder() {
        return new Builder();
    }

    public JsonWriter writeStartArray() {
        return unsafeWrite(generator::writeStartArray);
    }

    public JsonWriter writeEndArray() {
        return unsafeWrite(generator::writeEndArray);
    }

    public JsonWriter writeNull() {
        return unsafeWrite(generator::writeEndArray);
    }

    public JsonWriter writeStartObject() {
        return unsafeWrite(generator::writeStartObject);
    }

    public JsonWriter writeEndObject() {
        return unsafeWrite(generator::writeEndObject);
    }

    public JsonWriter writeFieldName(String fieldName) {
        return unsafeWrite(() -> generator.writeFieldName(fieldName));
    }

    public JsonWriter writeValue(String val) {
        return unsafeWrite(() -> generator.writeString(val));
    }

    public JsonWriter writeValue(boolean bool) {
        return unsafeWrite(() -> generator.writeBoolean(bool));
    }

    public JsonWriter writeValue(long val) {
        return unsafeWrite(() -> generator.writeNumber(val));
    }

    public JsonWriter writeValue(double val) {
        return unsafeWrite(() -> generator.writeNumber(val));
    }

    public JsonWriter writeValue(float val) {
        return unsafeWrite(() -> generator.writeNumber(val));
    }

    public JsonWriter writeValue(short val) {
        return unsafeWrite(() -> generator.writeNumber(val));
    }

    public JsonWriter writeValue(int val) {
        return unsafeWrite(() -> generator.writeNumber(val));
    }

    public JsonWriter writeValue(ByteBuffer bytes) {
        return unsafeWrite(() -> generator.writeBinary(BinaryUtils.copyBytesFrom(bytes)));
    }

    public JsonWriter writeValue(Instant instant) {
        return unsafeWrite(() -> generator.writeNumber(formatUnixTimestampInstant(instant)));
    }

    public JsonWriter writeValue(BigDecimal value) {
        return unsafeWrite(() -> generator.writeString(value.toString()));
    }

    public JsonWriter writeValue(BigInteger value) {
        return unsafeWrite(() -> generator.writeNumber(value));
    }

    public JsonWriter writeNumber(String number) {
        return unsafeWrite(() -> generator.writeNumber(number));
    }

    /**
     * Closes the generator and flushes to write. Must be called when finished writing JSON
     * content.
     */
    @Override
    public void close() {
        try {
            generator.close();
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
    }

    /**
     * Get the JSON content as a UTF-8 encoded byte array. It is recommended to hold onto the array
     * reference rather then making repeated calls to this method as a new array will be created
     * each time.
     *
     * @return Array of UTF-8 encoded bytes that make up the generated JSON.
     */
    public byte[] getBytes() {
        close();
        return baos.toByteArray();
    }

    private JsonWriter unsafeWrite(FunctionalUtils.UnsafeRunnable r) {
        try {
            r.run();
        } catch (Exception e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    /**
     * A builder for configuring and creating {@link JsonWriter}. Created via {@link #builder()}.
     */
    public static final class Builder {
        private JsonFactory jsonFactory;

        private Builder() {
        }

        /**
         * The {@link JsonFactory} implementation to be used when parsing the input. This allows JSON extensions like CBOR or
         * Ion to be supported.
         *
         * <p>It's highly recommended us use a shared {@code JsonFactory} where possible, so they should be stored statically:
         * http://wiki.fasterxml.com/JacksonBestPracticesPerformance
         *
         * <p>By default, this is {@link JsonNodeParser#DEFAULT_JSON_FACTORY}.
         */
        public JsonWriter.Builder jsonFactory(JsonFactory jsonFactory) {
            this.jsonFactory = jsonFactory;
            return this;
        }

        /**
         * Build a {@link JsonNodeParser} based on the current configuration of this builder.
         */
        public JsonWriter build() {
            return new JsonWriter(this);
        }
    }

    /**
     * Indicates an issue writing JSON content.
     */
    public static class JsonGenerationException extends RuntimeException {

        public JsonGenerationException(Throwable t) {
            super(t);
        }
    }
}
