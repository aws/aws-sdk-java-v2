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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.DateUtils;

/**
 * Thin wrapper around Jackson's JSON generator.
 */
@SdkProtectedApi
public class SdkJsonGenerator implements StructuredJsonGenerator {

    /**
     * Default buffer size for the BAOS. Chosen somewhat arbitrarily. Should be large enough to
     * prevent frequent resizings but small enough to avoid wasted allocations for small requests.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
    private final JsonGenerator generator;
    private final String contentType;

    public SdkJsonGenerator(JsonFactory factory, String contentType) {
        try {
            /**
             * A {@link JsonGenerator} created is by default enabled with
             * UTF-8 encoding
             */
            this.generator = factory.createGenerator(baos);
            this.contentType = contentType;
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
    }

    @Override
    public StructuredJsonGenerator writeStartArray() {
        try {
            generator.writeStartArray();
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeEndArray() {
        try {
            generator.writeEndArray();
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeNull() {
        try {
            generator.writeNull();
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeStartObject() {
        try {
            generator.writeStartObject();
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeEndObject() {
        try {
            generator.writeEndObject();
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeFieldName(String fieldName) {
        try {
            generator.writeFieldName(fieldName);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(String val) {
        try {
            generator.writeString(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(boolean bool) {
        try {
            generator.writeBoolean(bool);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(long val) {
        try {
            generator.writeNumber(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(double val) {
        try {
            generator.writeNumber(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(float val) {
        try {
            generator.writeNumber(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(short val) {
        try {
            generator.writeNumber(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(int val) {
        try {
            generator.writeNumber(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(ByteBuffer bytes) {
        try {
            generator.writeBinary(BinaryUtils.copyBytesFrom(bytes));
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    //TODO: This date formatting is coupled to AWS's format. Should generalize it
    public StructuredJsonGenerator writeValue(Instant instant) {
        try {
            generator.writeNumber(DateUtils.formatUnixTimestampInstant(instant));
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(BigDecimal value) {
        try {
            /**
             * Note that this is not how the backend represents BigDecimal types. On the wire
             * it's normally a JSON number but this causes problems with certain JSON implementations
             * that parse JSON numbers as floating points automatically. (See API-433)
             */
            generator.writeString(value.toString());
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(BigInteger value) {
        try {
            generator.writeNumber(value);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeNumber(String number) {
        try {
            generator.writeNumber(number);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    /**
     * Closes the generator and flushes to write. Must be called when finished writing JSON
     * content.
     */
    private void close() {
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
    @Override
    public byte[] getBytes() {
        close();
        return baos.toByteArray();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    protected JsonGenerator getGenerator() {
        return generator;
    }

    /**
     * Indicates an issue writing JSON content.
     */
    public static class JsonGenerationException extends SdkClientException {

        public JsonGenerationException(Throwable t) {
            super(SdkClientException.builder().cause(t));
        }
    }
}
