/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

public class SdkRequestConfigTest {
    private final Random rng = new SecureRandom();

    @Test
    public void customHeadersSetInBuilder_AreBuiltIntoUnmodifiableMap() {
        Map<String, String> headers = SdkRequestConfig.builder()
                .customHeader("FooHeader", "FooValue")
                .build()
                .getCustomHeaders();

        assertThat(headers.entrySet(), not(empty()));
        assertIsUnmodifiable(headers);
    }

    @Test
    public void customQueryParamsSetInBuilder_AreBuiltIntoUnmodifiableMap() {
        Map<String, List<String>> params = SdkRequestConfig.builder()
                .customQueryParam("FooParam", "FooValue")
                .build()
                .getCustomQueryParams();

        assertThat(params.entrySet(), not(empty()));
        assertIsUnmodifiable(params);
    }

    @Test
    public void copyBuilderCopiesAllProperties() {
        SdkRequestConfig config = SdkRequestConfig.builder().build();
        Field[] fields = config.getClass().getDeclaredFields();

        // Set all non static fields to random values
        Arrays.stream(fields)
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(f -> {
                    f.setAccessible(true);
                    return f;
                })
                // This needs to be modified accordingly when a member with a new type is added to SdkRequestConfig
                .forEach(f -> {
                    try {
                        Class<?> cls = f.getType();
                        if (cls.isAssignableFrom(Integer.class)) {
                            f.set(config, Math.abs(rng.nextInt()));
                        } else if (cls.isAssignableFrom(Map.class) && f.getName().equals("customHeaders")) {
                            f.set(config, randomHeaders(5));
                        } else if (cls.isAssignableFrom(Map.class) && f.getName().equals("customQueryParams")) {
                            f.set(config, randomQueryParams(5,2));
                        } else {
                            throw new RuntimeException(String.format("Unable to generate a random"
                                    + " SdkRequestConfig because we encountered a new field that"
                                    + " can't be auto generated: '%s' with class %s; please modify"
                                    + " the test.", f.getName(), cls));
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Unable to set field!", e);
                    }
                });

        SdkRequestConfig configCopy = config.copyBuilder().build();
        assertReflectionEquals(config, configCopy);
    }


    private Map<String,String> randomHeaders(int n) {
        Map<String,String> headers = new HashMap<>();

        while (headers.size() < n) {
            headers.put(RandomStringUtils.random(10), RandomStringUtils.random(10));
        }
        return headers;
    }

    private Map<String, List<String>> randomQueryParams(int n, int m) {
        Map<String, List<String>> params = new HashMap<>();

        for (int i = 0; i < n; ++i) {
            List<String> values = new ArrayList<>();
            for (int j = 0; j < m; ++j) {
                values.add(RandomStringUtils.random(10));
            }

            List<String> prev;
            do {
                prev = params.put(RandomStringUtils.random(10), values);
            } while (prev != null);
        }

        return params;
    }

    private static void assertIsUnmodifiable(Map<String, ?> map) {
        try {
            map.put("NewKey", null);
            fail("Expected map to be unmodifiable");
        } catch (UnsupportedOperationException expected) {
        }
    }
}
