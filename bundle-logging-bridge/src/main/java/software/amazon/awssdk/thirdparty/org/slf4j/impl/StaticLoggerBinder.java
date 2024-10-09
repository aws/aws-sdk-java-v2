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

package software.amazon.awssdk.thirdparty.org.slf4j.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.thirdparty.org.slf4j.ILoggerFactory;
import software.amazon.awssdk.thirdparty.org.slf4j.impl.internal.ErrorUtil;
import software.amazon.awssdk.thirdparty.org.slf4j.impl.internal.ILoggerFactoryAdapter;

/**
 * Acts as a bridge to real SLF4J implementations of {@code StaticLoggerBinder}.
 */
@SdkInternalApi
public class StaticLoggerBinder {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final StaticLoggerBinder INSTANCE = new StaticLoggerBinder();
    private static final String LOGGER_BINDER_NAME = "org.slf4j.impl.StaticLoggerBinder";
    private static final Class<?> BINDER_CLASS;
    private static final Object IMPL;
    private static final State STATE;

    private StaticLoggerBinder() {
    }

    private enum State {
        INIT_FAILURE,
        INIT_SUCCESS
    }

    static {
        Class<?> binderClass;
        Object impl = null;
        State initState = State.INIT_FAILURE;
        try {
            binderClass = Class.forName(LOGGER_BINDER_NAME);
            try {
                MethodType mt = MethodType.methodType(binderClass);
                MethodHandle mh = LOOKUP.findStatic(binderClass, "getSingleton", mt);
                impl = mh.invoke();
                initState = State.INIT_SUCCESS;
            } catch (Throwable t) {
                ErrorUtil.report(String.format("%s#getSingleton threw an exception: %s. Logging will not be initialized.",
                                               binderClass.getCanonicalName(), t.getMessage()));
            }
        } catch (ClassNotFoundException e) {
            binderClass = null;
        }
        BINDER_CLASS = binderClass;
        IMPL = impl;
        STATE = initState;
    }

    // SLF4J API
    public static final StaticLoggerBinder getSingleton() {
        if (STATE != State.INIT_SUCCESS) {
            throw new NoClassDefFoundError(StaticLoggerBinder.class.getCanonicalName());
        }

        return INSTANCE;
    }

    // SLF4J API
    public ILoggerFactory getLoggerFactory() {
        MethodType mt = MethodType.methodType(org.slf4j.ILoggerFactory.class);
        try {
            MethodHandle mh = LOOKUP.findVirtual(BINDER_CLASS, "getLoggerFactory", mt);
            return new ILoggerFactoryAdapter((org.slf4j.ILoggerFactory) mh.invoke(IMPL));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // SLF4J API
    public String getLoggerFactoryClassStr() {
        return ILoggerFactoryAdapter.class.getCanonicalName();
    }

    @SdkTestInternalApi
    Object getActualStaticLoggerBinder() {
        return IMPL;
    }
}