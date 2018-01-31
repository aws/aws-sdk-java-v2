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

package software.amazon.awssdk.core.protocol.json.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.runtime.transform.PathMarshallers;

@SdkInternalApi
public final class SimpleTypePathMarshallers {

    public static final JsonMarshaller<String> STRING =
            new SimplePathMarshaller<>(ValueToStringConverters.FROM_STRING, PathMarshallers.NON_GREEDY);

    public static final JsonMarshaller<Integer> INTEGER =
            new SimplePathMarshaller<>(ValueToStringConverters.FROM_INTEGER, PathMarshallers.NON_GREEDY);

    public static final JsonMarshaller<Long> LONG =
            new SimplePathMarshaller<>(ValueToStringConverters.FROM_LONG, PathMarshallers.NON_GREEDY);

    /**
     * Marshallers for Strings bound to a greedy path param. No URL encoding is done on the string
     * so that it preserves the path structure.
     */
    public static final JsonMarshaller<String> GREEDY_STRING =
            new SimplePathMarshaller<>(ValueToStringConverters.FROM_STRING, PathMarshallers.GREEDY);

    public static final JsonMarshaller<Void> NULL = (val, context, paramName) -> {
        throw new IllegalArgumentException(String.format("Parameter '%s' must not be null", paramName));
    };

    private SimpleTypePathMarshallers() {
    }

    private static class SimplePathMarshaller<T> implements JsonMarshaller<T> {

        private final ValueToStringConverters.ValueToString<T> converter;
        private final PathMarshallers.PathMarshaller pathMarshaller;

        private SimplePathMarshaller(ValueToStringConverters.ValueToString<T> converter,
                                     PathMarshallers.PathMarshaller pathMarshaller) {
            this.converter = converter;
            this.pathMarshaller = pathMarshaller;
        }

        @Override
        public void marshall(T val, JsonMarshallerContext context, String paramName) {
            context.request().setResourcePath(
                    pathMarshaller.marshall(context.request().getResourcePath(), paramName, converter.apply(val)));
        }

    }
}
