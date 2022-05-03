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

package software.amazon.awssdk.transfer.s3.internal.serialization;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * Marshallers and unmarshallers for serializing objects in TM, using the SDK request {@link MarshallingType}.
 * <p>
 * Excluded marshalling types that should not appear inside a POJO like GetObjectRequest:
 * <ul>
 *     <li>MarshallingType.SDK_POJO</li>
 *     <li>MarshallingType.DOCUMENT</li>
 *     <li>MarshallingType.MAP</li>
 *     <li>MarshallingType.LIST</li>
 * </ul>
 * <p>
 * Note: unmarshalling generic List and Map structures is not supported at this time
 */
@SdkInternalApi
public final class TransferManagerMarshallingUtils {

    private static final Map<MarshallingType<?>, TransferManagerJsonMarshaller<?>> MARSHALLERS;
    private static final Map<MarshallingType<?>, TransferManagerJsonUnmarshaller<?>> UNMARSHALLERS;

    static {
        Map<MarshallingType<?>, TransferManagerJsonMarshaller<?>> marshallers = new HashMap<>();
        marshallers.put(MarshallingType.STRING, TransferManagerJsonMarshaller.STRING);
        marshallers.put(MarshallingType.SHORT, TransferManagerJsonMarshaller.SHORT);
        marshallers.put(MarshallingType.INTEGER, TransferManagerJsonMarshaller.INTEGER);
        marshallers.put(MarshallingType.LONG, TransferManagerJsonMarshaller.LONG);
        marshallers.put(MarshallingType.INSTANT, TransferManagerJsonMarshaller.INSTANT);
        marshallers.put(MarshallingType.NULL, TransferManagerJsonMarshaller.NULL);
        marshallers.put(MarshallingType.FLOAT, TransferManagerJsonMarshaller.FLOAT);
        marshallers.put(MarshallingType.DOUBLE, TransferManagerJsonMarshaller.DOUBLE);
        marshallers.put(MarshallingType.BIG_DECIMAL, TransferManagerJsonMarshaller.BIG_DECIMAL);
        marshallers.put(MarshallingType.BOOLEAN, TransferManagerJsonMarshaller.BOOLEAN);
        marshallers.put(MarshallingType.SDK_BYTES, TransferManagerJsonMarshaller.SDK_BYTES);
        marshallers.put(MarshallingType.LIST, TransferManagerJsonMarshaller.LIST);
        marshallers.put(MarshallingType.MAP, TransferManagerJsonMarshaller.MAP);
        MARSHALLERS = Collections.unmodifiableMap(marshallers);

        Map<MarshallingType<?>, TransferManagerJsonUnmarshaller<?>> unmarshallers = new HashMap<>();
        unmarshallers.put(MarshallingType.STRING, TransferManagerJsonUnmarshaller.STRING);
        unmarshallers.put(MarshallingType.SHORT, TransferManagerJsonUnmarshaller.SHORT);
        unmarshallers.put(MarshallingType.INTEGER, TransferManagerJsonUnmarshaller.INTEGER);
        unmarshallers.put(MarshallingType.LONG, TransferManagerJsonUnmarshaller.LONG);
        unmarshallers.put(MarshallingType.INSTANT, TransferManagerJsonUnmarshaller.INSTANT);
        unmarshallers.put(MarshallingType.NULL, TransferManagerJsonUnmarshaller.NULL);
        unmarshallers.put(MarshallingType.FLOAT, TransferManagerJsonUnmarshaller.FLOAT);
        unmarshallers.put(MarshallingType.DOUBLE, TransferManagerJsonUnmarshaller.DOUBLE);
        unmarshallers.put(MarshallingType.BIG_DECIMAL, TransferManagerJsonUnmarshaller.BIG_DECIMAL);
        unmarshallers.put(MarshallingType.BOOLEAN, TransferManagerJsonUnmarshaller.BOOLEAN);
        unmarshallers.put(MarshallingType.SDK_BYTES, TransferManagerJsonUnmarshaller.SDK_BYTES);

        UNMARSHALLERS = Collections.unmodifiableMap(unmarshallers);
    }

    private TransferManagerMarshallingUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> TransferManagerJsonMarshaller<T> getMarshaller(T val) {
        MarshallingType<T> tMarshallingType = toMarshallingType(val);
        return getMarshaller(tMarshallingType, val);
    }

    @SuppressWarnings("unchecked")
    private static <T> MarshallingType<T> toMarshallingType(T val) {
        MarshallingType<?> marshallingType = MarshallingType.NULL;
        if (val != null) {
            for (MarshallingType<?> type : MARSHALLERS.keySet()) {
                if (type.getTargetClass().isAssignableFrom(val.getClass())) {
                    marshallingType = type;
                }
            }
        }
        return (MarshallingType<T>) marshallingType;
    }

    @SuppressWarnings("unchecked")
    public static <T> TransferManagerJsonMarshaller<T> getMarshaller(MarshallingType<?> marshallingType, T val) {
        TransferManagerJsonMarshaller<?> marshaller = MARSHALLERS.get(val == null ? MarshallingType.NULL : marshallingType);
        if (marshaller == null) {
            throw new IllegalStateException(String.format("Cannot find a marshaller for marshalling type %s", marshallingType));
        }
        return (TransferManagerJsonMarshaller<T>) marshaller;
    }

    @SuppressWarnings("unchecked")
    public static TransferManagerJsonUnmarshaller<Object> getUnmarshaller(MarshallingType<?> marshallingType) {
        TransferManagerJsonUnmarshaller<?> unmarshaller = UNMARSHALLERS.get(marshallingType);
        if (unmarshaller == null) {
            throw new IllegalStateException(String.format("Cannot find an unmarshaller for marshalling type %s",
                                                          marshallingType));
        }
        return (TransferManagerJsonUnmarshaller<Object>) unmarshaller;
    }

    public static SdkField<?> getSdkField(String key) {
        return GetObjectRequest.builder().build()
                               .sdkFields().stream()
                               .filter(f -> f.locationName().equalsIgnoreCase(key))
                               .findFirst()
                               .orElseThrow(() -> new IllegalStateException("Could not match a field in GetObjectRequest"));
    }

}
