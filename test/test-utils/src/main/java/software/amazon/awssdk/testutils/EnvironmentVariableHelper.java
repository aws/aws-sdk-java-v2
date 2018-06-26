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

package software.amazon.awssdk.testutils;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.rules.ExternalResource;
import software.amazon.awssdk.utils.SystemSetting;

/**
 * A utility that can temporarily forcibly set environment variables and
 * then allows resetting them to the original values.
 */
public class EnvironmentVariableHelper extends ExternalResource {

    private final Map<String, String> originalEnvironmentVariables;
    private final Map<String, String> modifiableMap;
    private volatile boolean mutated = false;

    @SuppressWarnings("unchecked")
    public EnvironmentVariableHelper() {
        try {
            // CHECKSTYLE:OFF - This is a specific utility around system environment variables
            originalEnvironmentVariables = new HashMap<>(System.getenv());
            Field f = System.getenv().getClass().getDeclaredField("m");
            AccessController.doPrivileged(setAccessible(f));

            modifiableMap = (Map<String, String>) f.get(System.getenv());
            // CHECKSTYLE:ON
        } catch (ReflectiveOperationException | PrivilegedActionException e) {
            throw new RuntimeException(e);
        }
    }

    public void remove(SystemSetting setting) {
        remove(setting.environmentVariable());
    }

    public void remove(String key) {
        mutated = true;
        modifiableMap.remove(key);
    }

    public void set(SystemSetting setting, String value) {
        set(setting.environmentVariable(), value);
    }

    public void set(String key, String value) {
        mutated = true;
        modifiableMap.put(key, value);
    }

    public void reset() {
        if (mutated) {
            synchronized (this) {
                if (mutated) {
                    modifiableMap.clear();
                    modifiableMap.putAll(originalEnvironmentVariables);
                    mutated = false;
                }
            }
        }
    }

    @Override
    protected void after() {
        reset();
    }

    private PrivilegedExceptionAction<Void> setAccessible(Field f) {
        return () -> {
            f.setAccessible(true);
            return null;
        };
    }

    /**
     * Static run method that allows for "single-use" environment variable modification.
     *
     * Example use:
     * <pre>
     * {@code
     * EnvironmentVariableHelper.run(helper -> {
     *    helper.set("variable", "value");
     *    //run some test that uses "variable"
     * });
     * }
     * </pre>
     *
     * Will call {@link #reset} at the end of the block (even if the block exits exceptionally).
     *
     * @param helperConsumer a code block to run that gets an {@link EnvironmentVariableHelper} as an argument
     */
    public static void run(Consumer<EnvironmentVariableHelper> helperConsumer) {
        EnvironmentVariableHelper helper = new EnvironmentVariableHelper();
        try {
            helperConsumer.accept(helper);
        } finally {
            helper.reset();
        }
    }
}
