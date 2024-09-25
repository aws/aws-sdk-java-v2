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

package software.amazon.awssdk.protocols.rpcv2.internal;

import java.io.IOException;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;
import software.amazon.awssdk.thirdparty.jackson.dataformat.cbor.CBORGenerator;

/**
 * Thin wrapper around Jackson's JSON generator for CBOR.
 */
@SdkInternalApi
public final class SdkRpcV2CborGenerator extends SdkJsonGenerator {

    private static final int CBOR_TAG_TIMESTAMP = 1;

    SdkRpcV2CborGenerator(JsonFactory factory, String contentType) {
        super(factory, contentType);
    }

    /**
     * Jackson doesn't have native support for timestamp. As per the RFC 7049 (https://tools.ietf.org/html/rfc7049#section-2.4.1)
     * we will need to write a tag and write the epoch.
     */
    @Override
    public StructuredJsonGenerator writeValue(Instant instant) {
        CBORGenerator generator = getGenerator();
        try {
            generator.writeTag(CBOR_TAG_TIMESTAMP);
            generator.writeNumber(instant.toEpochMilli() / 1000d);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeStartArray(int size) {
        CBORGenerator generator = getGenerator();
        try {
            generator.writeStartArray(null, size);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(double val) {
        if (canConvertToLong(val)) {
            return writeValue((long) val);
        }
        CBORGenerator generator = getGenerator();
        try {
            generator.writeNumber(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    public StructuredJsonGenerator writeValue(float val) {
        if (canConvertToLong(val)) {
            return writeValue((long) val);
        }
        CBORGenerator generator = getGenerator();
        try {
            generator.writeNumber(val);
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }

    @Override
    protected CBORGenerator getGenerator() {
        return (CBORGenerator) super.getGenerator();
    }

    /**
     * Checks if we can convert the floating point value to a long. If we can we then use the writeNumber(long), alongside with
     * having enabled Feature.WRITE_MINIMAL_INTS will allow us to represent the floating point values minimally.
     */
    private static boolean canConvertToLong(double value) {
        return ((double) (long) value) == value
            && value >= Long.MIN_VALUE
            && value <= Long.MAX_VALUE;
    }

    /**
     * Checks if we can convert the floating point value to a long. If we can we then use the writeNumber(long), alongside with
     * having enabled Feature.WRITE_MINIMAL_INTS will allow us to represent the floating point values minimally.
     */
    private static boolean canConvertToLong(float value) {
        return ((float) (long) value) == value
               && value >= Long.MIN_VALUE
               && value <= Long.MAX_VALUE;
    }
}
