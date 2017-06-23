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

package software.amazon.awssdk.protocol.json.internal;

import java.util.Date;
import software.amazon.awssdk.annotation.SdkInternalApi;

@SdkInternalApi
public class HeaderMarshallers {

    public static final JsonMarshaller<String> STRING = new SimpleHeaderMarshaller<>(ValueToStringConverters.FROM_STRING);

    public static final JsonMarshaller<Integer> INTEGER = new SimpleHeaderMarshaller<>(ValueToStringConverters.FROM_INTEGER);

    public static final JsonMarshaller<Long> LONG = new SimpleHeaderMarshaller<>(ValueToStringConverters.FROM_LONG);

    public static final JsonMarshaller<Double> DOUBLE = new SimpleHeaderMarshaller<>(ValueToStringConverters.FROM_DOUBLE);

    public static final JsonMarshaller<Float> FLOAT = new SimpleHeaderMarshaller<>(ValueToStringConverters.FROM_FLOAT);

    public static final JsonMarshaller<Boolean> BOOLEAN = new SimpleHeaderMarshaller<>(ValueToStringConverters.FROM_BOOLEAN);

    public static final JsonMarshaller<Date> DATE = new SimpleHeaderMarshaller<>(ValueToStringConverters.FROM_DATE);

    private static class SimpleHeaderMarshaller<T> implements JsonMarshaller<T> {

        private final ValueToStringConverters.ValueToString<T> converter;

        private SimpleHeaderMarshaller(ValueToStringConverters.ValueToString<T> converter) {
            this.converter = converter;
        }

        @Override
        public void marshall(T val, JsonMarshallerContext context, String paramName) {
            context.request().addHeader(paramName, converter.apply(val));
        }
    }

}
