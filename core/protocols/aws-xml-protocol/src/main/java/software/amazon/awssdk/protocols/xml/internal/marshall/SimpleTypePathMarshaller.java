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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.protocols.core.PathMarshaller;
import software.amazon.awssdk.protocols.core.ValueToStringConverter;

@SdkInternalApi
public final class SimpleTypePathMarshaller {

    public static final XmlMarshaller<String> STRING =
        new SimplePathMarshaller<>(ValueToStringConverter.FROM_STRING, PathMarshaller.NON_GREEDY);

    public static final XmlMarshaller<Integer> INTEGER =
        new SimplePathMarshaller<>(ValueToStringConverter.FROM_INTEGER, PathMarshaller.NON_GREEDY);

    public static final XmlMarshaller<Long> LONG =
        new SimplePathMarshaller<>(ValueToStringConverter.FROM_LONG, PathMarshaller.NON_GREEDY);

    /**
     * Marshallers for Strings bound to a greedy path param. No URL encoding is done on the string
     * so that it preserves the path structure.
     */
    public static final XmlMarshaller<String> GREEDY_STRING =
        new SimplePathMarshaller<>(ValueToStringConverter.FROM_STRING, PathMarshaller.GREEDY_WITH_SLASHES);

    public static final XmlMarshaller<Void> NULL = (val, context, paramName, sdkField) -> {
        throw new IllegalArgumentException(String.format("Parameter '%s' must not be null", paramName));
    };

    private SimpleTypePathMarshaller() {
    }

    private static class SimplePathMarshaller<T> implements XmlMarshaller<T> {

        private final ValueToStringConverter.ValueToString<T> converter;
        private final PathMarshaller pathMarshaller;

        private SimplePathMarshaller(ValueToStringConverter.ValueToString<T> converter,
                                     PathMarshaller pathMarshaller) {
            this.converter = converter;
            this.pathMarshaller = pathMarshaller;
        }

        @Override
        public void marshall(T val, XmlMarshallerContext context, String paramName, SdkField<T> sdkField) {
            context.request().encodedPath(
                pathMarshaller.marshall(context.request().encodedPath(), paramName, converter.convert(val, sdkField)));
        }
    }
}
