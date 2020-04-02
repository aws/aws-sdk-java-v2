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

package software.amazon.awssdk.protocols.cbor.internal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import java.io.IOException;
import java.time.Instant;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.json.SdkJsonGenerator;
import software.amazon.awssdk.protocols.json.StructuredJsonGenerator;

/**
 * Thin wrapper around Jackson's JSON generator for CBOR.
 */
@SdkInternalApi
public final class SdkCborGenerator extends SdkJsonGenerator {

    private static final int CBOR_TAG_TIMESTAMP = 1;

    SdkCborGenerator(JsonFactory factory, String contentType) {
        super(factory, contentType);
    }

    /**
     * Jackson doesn't have native support for timestamp. As per the RFC 7049
     * (https://tools.ietf.org/html/rfc7049#section-2.4.1) we will need to
     * write a tag and write the epoch.
     */
    @Override
    public StructuredJsonGenerator writeValue(Instant instant) {
        if (!(getGenerator() instanceof CBORGenerator)) {
            throw new IllegalStateException("SdkCborGenerator is not created with a CBORGenerator.");
        }

        CBORGenerator generator = (CBORGenerator) getGenerator();
        try {
            generator.writeTag(CBOR_TAG_TIMESTAMP);
            generator.writeNumber(instant.toEpochMilli());
        } catch (IOException e) {
            throw new JsonGenerationException(e);
        }
        return this;
    }
}
