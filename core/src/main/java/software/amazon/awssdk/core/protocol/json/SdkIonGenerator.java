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

package software.amazon.awssdk.core.protocol.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.ion.IonType;
import software.amazon.ion.IonWriter;
import software.amazon.ion.Timestamp;
import software.amazon.ion.system.IonWriterBuilder;

@SdkInternalApi
abstract class SdkIonGenerator implements StructuredJsonGenerator {
    protected final IonWriter writer;
    private final String contentType;

    private SdkIonGenerator(IonWriter writer, String contentType) {
        this.writer = writer;
        this.contentType = contentType;
    }

    public static SdkIonGenerator create(IonWriterBuilder builder, String contentType) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        IonWriter writer = builder.build(bytes);
        return new ByteArraySdkIonGenerator(bytes, writer, contentType);
    }

    @Override
    public StructuredJsonGenerator writeStartArray() {
        try {
            writer.stepIn(IonType.LIST);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeNull() {
        try {
            writer.writeNull();
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeEndArray() {
        try {
            writer.stepOut();
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeStartObject() {
        try {
            writer.stepIn(IonType.STRUCT);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeEndObject() {
        try {
            writer.stepOut();
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeFieldName(String fieldName) {
        writer.setFieldName(fieldName);
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(String val) {
        try {
            writer.writeString(val);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(boolean bool) {
        try {
            writer.writeBool(bool);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(long val) {
        try {
            writer.writeInt(val);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(double val) {
        try {
            writer.writeFloat(val);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(float val) {
        try {
            writer.writeFloat(val);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(short val) {
        try {
            writer.writeInt(val);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(int val) {
        try {
            writer.writeInt(val);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(ByteBuffer bytes) {
        try {
            writer.writeBlob(BinaryUtils.copyAllBytesFrom(bytes));
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(Instant instant) {
        try {
            Date d = instant != null ? Date.from(instant) : null;
            writer.writeTimestamp(Timestamp.forDateZ(d));
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(BigDecimal value) {
        try {
            writer.writeDecimal(value);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(BigInteger value) {
        try {
            writer.writeInt(value);
        } catch (IOException e) {
            throw new SdkClientException(e);
        }
        return this;
    }

    @Override
    public abstract byte[] getBytes();

    @Override
    public String getContentType() {
        return contentType;
    }

    private static class ByteArraySdkIonGenerator extends SdkIonGenerator {
        private final ByteArrayOutputStream bytes;

        ByteArraySdkIonGenerator(ByteArrayOutputStream bytes, IonWriter writer, String contentType) {
            super(writer, contentType);
            this.bytes = bytes;
        }

        @Override
        public byte[] getBytes() {
            try {
                writer.finish();
            } catch (IOException e) {
                throw new SdkClientException(e);
            }
            return bytes.toByteArray();
        }
    }
}
