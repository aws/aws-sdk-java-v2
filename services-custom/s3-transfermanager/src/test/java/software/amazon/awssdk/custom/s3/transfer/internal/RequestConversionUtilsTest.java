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

package software.amazon.awssdk.custom.s3.transfer.internal;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

/**
 * Tests for {@link RequestConversionUtils}.
 */
public class RequestConversionUtilsTest {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RNG = new Random();

    @Test
    public void toHeadObjectCopiesAllProperties() {
        GetObjectRequest randomGetObject = randomGetObjectRequest();
        HeadObjectRequest convertedToHeadObject = RequestConversionUtils.toHeadObjectRequest(randomGetObject);

        Map<String, SdkField<?>> headObjectFields = sdkFieldMap(HeadObjectRequest.builder().sdkFields());
        Map<String, SdkField<?>> getObjectFields = sdkFieldMap(GetObjectRequest.builder().sdkFields());

        for (Map.Entry<String, SdkField<?>> headObjectEntry : headObjectFields.entrySet()) {
            SdkField<?> headObjField = headObjectEntry.getValue();
            SdkField<?> getObjectField = getObjectFields.get(headObjectEntry.getKey());

            Object headObjectVal = headObjField.getValueOrDefault(convertedToHeadObject);
            Object getObjectVal = getObjectField.getValueOrDefault(randomGetObject);

            assertThat(headObjectVal).isEqualTo(getObjectVal);
        }
    }

    private GetObjectRequest randomGetObjectRequest() {
        GetObjectRequest.Builder builder = GetObjectRequest.builder();
        setFieldsToRandomValues(builder.sdkFields(), builder);
        return builder.build();
    }

    private void setFieldsToRandomValues(Collection<SdkField<?>> fields, Object builder) {
        for (SdkField<?> f : fields) {
            setFieldToRandomValue(f, builder);
        }
    }

    private static void setFieldToRandomValue(SdkField<?> sdkField, Object obj) {
        Class<?> targetClass = sdkField.marshallingType().getTargetClass();
        if (targetClass.equals(String.class)) {
            sdkField.set(obj, randomString(8));
        } else if (targetClass.equals(Integer.class)) {
            sdkField.set(obj, randomInteger());
        } else if (targetClass.equals(Instant.class)) {
            sdkField.set(obj, randomInstant());
        } else {
            throw new IllegalArgumentException("Unknown SdkField type: " + targetClass);
        }
    }

    private static Map<String, SdkField<?>> sdkFieldMap(Collection<? extends SdkField<?>> sdkFields) {
        Map<String, SdkField<?>> map = new HashMap<>(sdkFields.size());
        for (SdkField<?> f : sdkFields) {
            String locName = f.locationName();
            if (map.put(locName, f) != null) {
                throw new IllegalArgumentException("Multiple SdkFields map to same location name");
            }
        }
        return map;
    }

    private static String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        while (len-- > 0) {
            sb.append(ALPHA.charAt(RNG.nextInt(ALPHA.length())));
        }
        return sb.toString();
    }

    private static Instant randomInstant() {
        return Instant.ofEpochMilli(RNG.nextLong());
    }

    private static Integer randomInteger() {
        return RNG.nextInt();
    }
}
