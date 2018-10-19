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

package software.amazon.awssdk.core.internal.protocol.query.unmarshall;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.internal.protocol.StringToValueConverter;
import software.amazon.awssdk.core.protocol.SdkField;

/**
 * Unmarshaller implementation for simple, scalar values.
 *
 * @param <T> Type being unmarshalled.
 */
@SdkInternalApi
final class SimpleTypeQueryUnmarshaller<T> implements QueryUnmarshaller<T> {

    private final StringToValueConverter.StringToValue<T> stringToValue;

    SimpleTypeQueryUnmarshaller(StringToValueConverter.StringToValue<T> stringToValue) {
        this.stringToValue = stringToValue;
    }

    @Override
    public T unmarshall(QueryUnmarshallerContext context, List<XmlElement> content, SdkField<T> field) {
        if (content == null) {
            return null;
        }
        return stringToValue.convert(content.get(0).textContent(), field);
    }
}
