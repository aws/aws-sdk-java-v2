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

package software.amazon.awssdk.enhanced.dynamodb.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.FloatAttributeConverter;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Utilities for marshalling search vectors to the low-level DynamoDB API.
 */
@SdkInternalApi
public final class SearchVectorUtils {
    private static final FloatAttributeConverter FLOAT_CONVERTER = FloatAttributeConverter.create();

    private SearchVectorUtils() {
    }

    /**
     * Converts a {@code float[]} embedding into the {@code List<AttributeValue>} shape expected by
     * {@code SearchVectorsRequest.searchVector()}.
     */
    public static List<AttributeValue> toSearchVector(float[] searchVector) {
        if (searchVector == null) {
            return null;
        }

        List<AttributeValue> result = new ArrayList<>(searchVector.length);
        for (float value : searchVector) {
            result.add(FLOAT_CONVERTER.transformFrom(value));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Converts a {@code List<Float>} embedding into the {@code List<AttributeValue>} shape expected by
     * {@code SearchVectorsRequest.searchVector()}.
     */
    public static List<AttributeValue> toSearchVector(List<Float> searchVector) {
        if (searchVector == null) {
            return null;
        }

        List<AttributeValue> result = new ArrayList<>(searchVector.size());
        for (Float value : searchVector) {
            result.add(FLOAT_CONVERTER.transformFrom(value));
        }
        return Collections.unmodifiableList(result);
    }
}
