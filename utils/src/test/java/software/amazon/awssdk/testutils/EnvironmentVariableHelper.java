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

package software.amazon.awssdk.testutils;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    public EnvironmentVariableHelper() {
        // CHECKSTYLE:OFF - This is a specific utility around system environment variables
        originalEnvironmentVariables = new HashMap<>(System.getenv());
        modifiableMap = Optional.ofNullable(processEnv()).orElse(envMap());
        // CHECKSTYLE:ON
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

    private Map<String, String> envMap() {
        // CHECKSTYLE:OFF - This is a specific utility around system environment variables
        return getField(System.getenv().getClass(), System.getenv(), "m");
        // CHECKSTYLE:ON
    }

    /**
     * Windows is using a different process environment.
     *
     * See http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/windows/classes/java/lang/ProcessEnvironment.java#l235
     */
    private Map<String, String> processEnv() {
        Class<?> processEnvironment;
        try {
            processEnvironment = Class.forName("java.lang.ProcessEnvironment");
            return getField(processEnvironment, null, "theCaseInsensitiveEnvironment");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getField(Class<?> processEnvironment, Object obj, String fieldName) {
        try {
            Field declaredField = processEnvironment.getDeclaredField(fieldName);
            AccessController.doPrivileged(setAccessible(declaredField));

            return (Map<String, String>) declaredField.get(obj);
        } catch (IllegalAccessException | NoSuchFieldException | PrivilegedActionException e) {
            return null;
        }
    }
}
