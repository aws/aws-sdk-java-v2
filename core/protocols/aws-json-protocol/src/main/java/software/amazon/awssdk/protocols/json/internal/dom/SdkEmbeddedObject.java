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

package software.amazon.awssdk.protocols.json.internal.dom;

import com.fasterxml.jackson.core.JsonParser;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Represents an embedded object returned by a {@link JsonParser}. This is used for the ION
 * format which embeds {@link Date} and {@link ByteBuffer} objects.
 */
@SdkInternalApi
public final class SdkEmbeddedObject implements SdkJsonNode {

    private final Object embeddedObject;

    private SdkEmbeddedObject(Object embeddedObject) {
        this.embeddedObject = embeddedObject;
    }

    /**
     * @return The embedded object that was returned by the {@link JsonParser}.
     */
    @Override
    public Object embeddedObject() {
        return embeddedObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SdkEmbeddedObject that = (SdkEmbeddedObject) o;
        return Objects.equals(embeddedObject, that.embeddedObject);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(embeddedObject);
    }

    static SdkEmbeddedObject create(Object embeddedObject) {
        return new SdkEmbeddedObject(embeddedObject);
    }
}
