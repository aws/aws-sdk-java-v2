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

package software.amazon.awssdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import software.amazon.awssdk.retry.PredefinedRetryPolicies;
import software.amazon.awssdk.retry.RetryPolicy;
import software.amazon.awssdk.util.ImmutableMapParameter;

public class LegacyClientConfigurationTest {

    private static final Random RANDOM = new Random();

    private static final LegacyClientConfiguration DEFAULT_CLIENT_CONFIG = new LegacyClientConfiguration();

    private static final RetryPolicy CUSTOM_RETRY_POLICY = new RetryPolicy(
            PredefinedRetryPolicies.SdkDefaultRetryCondition.NO_RETRY_CONDITION,
            RetryPolicy.BackoffStrategy.NO_DELAY, 1000, false);

    @Test
    public void testHeadersDeepCopyInConstructor() {
        String key1 = "key1";
        String value1 = "value1";
        String key2 = "key2";
        String value2 = "value2";

        LegacyClientConfiguration source = new LegacyClientConfiguration().withHeader(key1, value1).withHeader(key2, value2);
        LegacyClientConfiguration target = new LegacyClientConfiguration(source);

        assertEquals(2, target.getHeaders().size());
        assertEquals(value1, target.getHeaders().get(key1));
        assertEquals(value2, target.getHeaders().get(key2));

        source.withHeader(key1, "value3");
        source.withHeader("new key", "new value");

        assertEquals(2, target.getHeaders().size());
        assertEquals(value1, target.getHeaders().get(key1));
        assertEquals(value2, target.getHeaders().get(key2));
    }

    @Test
    public void clientConfigurationCopyConstructor_CopiesAllValues() throws Exception {
        LegacyClientConfiguration customConfig = new LegacyClientConfiguration();

        for (Field field : LegacyClientConfiguration.class.getDeclaredFields()) {
            if (isStaticField(field)) {
                continue;
            }
            field.setAccessible(true);
            final Class<?> clzz = field.getType();

            if (clzz.isAssignableFrom(int.class) || clzz.isAssignableFrom(long.class)) {
                field.set(customConfig, RANDOM.nextInt(Integer.MAX_VALUE));
            } else if (clzz.isAssignableFrom(boolean.class)) {
                // Invert the default value to ensure it's different
                field.set(customConfig, !(Boolean) field.get(customConfig));
            } else if (clzz.isAssignableFrom(String.class)) {
                field.set(customConfig, RandomStringUtils.random(10));
            } else if (clzz.isAssignableFrom(RetryPolicy.class)) {
                field.set(customConfig, CUSTOM_RETRY_POLICY);
            } else if (clzz.isAssignableFrom(InetAddress.class)) {
                field.set(customConfig, InetAddress.getLocalHost());
            } else if (clzz.isAssignableFrom(Protocol.class)) {
                // Default is HTTPS so switch to HTTP
                field.set(customConfig, Protocol.HTTP);
            } else if (clzz.isAssignableFrom(SecureRandom.class)) {
                field.set(customConfig, new SecureRandom());
            } else if (field.getName().equals("headers")) {
                field.set(customConfig, ImmutableMapParameter.of("foo", "bar"));
            } else {
                throw new RuntimeException(
                        String.format("Field %s of type %s is not supported",
                                      field.getName(),
                                      field.getType()));
            }
            // Extra check to make sure the value differs from the default and we haven't missed something
            assertNotEquals(
                    String.format("Field %s does not differ from default value", field.getName()),
                    field.get(DEFAULT_CLIENT_CONFIG), field.get(customConfig));
        }

        // Do a deep comparison of the config after sending it through the copy constructor
        assertReflectionEquals(customConfig, new LegacyClientConfiguration(customConfig));
    }

    private boolean isStaticField(Field field) {
        return (field.getModifiers() & Modifier.STATIC) == Modifier.STATIC;
    }


}
