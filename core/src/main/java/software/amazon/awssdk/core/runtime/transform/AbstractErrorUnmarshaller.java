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

package software.amazon.awssdk.core.runtime.transform;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkServiceException;

@SdkProtectedApi
public abstract class AbstractErrorUnmarshaller<T> implements Unmarshaller<SdkServiceException, T> {

    /**
     * The type of SdkServiceException that will be instantiated. Subclasses
     * specialized for a specific type of exception can control this through the
     * protected constructor.
     */
    protected final Class<? extends SdkServiceException> exceptionClass;

    /**
     * Constructs a new error unmarshaller that will unmarshall error responses
     * into SdkServiceException objects.
     */
    public AbstractErrorUnmarshaller() {
        this(SdkServiceException.class);
    }

    /**
     * Constructs a new error unmarshaller that will unmarshall error responses
     * into objects of the specified class, extending SdkServiceException.
     *
     * @param exceptionClass
     *            The subclass of SdkServiceException which will be
     *            instantiated and populated by this class.
     */
    public AbstractErrorUnmarshaller(Class<? extends SdkServiceException> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    /**
     * Constructs a new exception object of the type specified in this class's
     * constructor and sets the specified error message.
     *
     * @param message
     *            The error message to set in the new exception object.
     *
     * @return A new exception object of the type specified in this class's
     *         constructor and sets the specified error message.
     *
     * @throws Exception
     *             If there are any problems using reflection to invoke the
     *             exception class's constructor.
     */
    protected SdkServiceException newException(String message) throws Exception {
        Method builderMethod = null;

        try {
            builderMethod = exceptionClass.getDeclaredMethod("builder");
            makeAccessible(builderMethod);
        } catch (NoSuchMethodException e) {
            // ignored
        }

        if (builderMethod != null) {
            Object exceptionBuilder = builderMethod.invoke(null);
            Method buildMethod = exceptionBuilder.getClass().getDeclaredMethod("build");
            Method messageSetter = exceptionBuilder.getClass().getDeclaredMethod("message", String.class);
            makeAccessible(messageSetter);
            makeAccessible(buildMethod);

            messageSetter.invoke(exceptionBuilder, message);

            return (SdkServiceException) buildMethod.invoke(exceptionBuilder);
        } else {
            Constructor<? extends SdkServiceException> constructor = exceptionClass.getConstructor(String.class);
            return constructor.newInstance(message);
        }
    }

    protected static void makeAccessible(AccessibleObject object) {
        if (!object.isAccessible()) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                object.setAccessible(true);
                return null;
            });
        }
    }

}
