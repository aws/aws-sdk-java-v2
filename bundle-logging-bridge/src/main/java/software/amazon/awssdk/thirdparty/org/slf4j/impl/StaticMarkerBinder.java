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
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicReference;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.thirdparty.org.slf4j.IMarkerFactory;
import software.amazon.awssdk.thirdparty.org.slf4j.impl.internal.ErrorUtil;
import software.amazon.awssdk.thirdparty.org.slf4j.impl.internal.IMarkerFactoryAdapter;
import software.amazon.awssdk.thirdparty.org.slf4j.impl.internal.InvocationHelper;

@SdkInternalApi
public class StaticMarkerBinder {
    private static final String LOGGER_BINDER_NAME = "org.slf4j.impl.StaticMarkerBinder";
    private static final AtomicReference<MethodHandle> GET_SINGLETON = new AtomicReference<>();
    private static final AtomicReference<MethodHandle> GET_MARKER_FACTORY = new AtomicReference<>();
    private static final StaticMarkerBinder INSTANCE = new StaticMarkerBinder();
    private static final Class<?> BINDER_CLASS;
    private static final Object IMPL;
    private static final State STATE;

    private StaticMarkerBinder() {
    }

    private enum State {
        INIT_FAILURE,
        INIT_SUCCESS
    }

    static {
        Class<?> binderClass = null;
        State state = State.INIT_FAILURE;
        Object impl = null;
        try {
            binderClass = Class.forName(LOGGER_BINDER_NAME);
            Class<?> binderFinal = binderClass;
            MethodHandle getSingleton = InvocationHelper.cachedGetHandle(GET_SINGLETON, () -> {
                return InvocationHelper.staticHandle(binderFinal, "getSingleton", MethodType.methodType(binderFinal))
                                       .orElse(null);
            });
            if (getSingleton == null) {
                ErrorUtil.report(String.format("%s does not have a getSingleton method", LOGGER_BINDER_NAME));
            } else {
                try {
                    impl = getSingleton.invoke();
                    state = State.INIT_SUCCESS;
                } catch (Throwable t) {
                    ErrorUtil.report(String.format("%s#getSingleton() threw exception: %s", LOGGER_BINDER_NAME, t.getMessage()));
                }
            }
        } catch (ClassNotFoundException e) {
            ErrorUtil.report(String.format("%s not found", LOGGER_BINDER_NAME));
        }

        BINDER_CLASS = binderClass;
        IMPL = impl;
        STATE = state;
    }

    // SLF4J API
    public static StaticMarkerBinder getSingleton() {
        if (STATE != State.INIT_SUCCESS) {
            throw new NoClassDefFoundError(StaticMarkerBinder.class.getCanonicalName());
        }
        return INSTANCE;
    }

    // SLF4J API
    public IMarkerFactory getMarkerFactory() {
        MethodHandle mh = InvocationHelper.cachedGetHandle(GET_MARKER_FACTORY, () -> {
            return InvocationHelper.virtualHandle(BINDER_CLASS, "getMarkerFactory",
                                           MethodType.methodType(org.slf4j.IMarkerFactory.class))
                .orElse(null);
        });

        if (mh == null) {
            throw new RuntimeException(new NoSuchMethodException("getMarkerFactory"));
        }

        try {
            org.slf4j.IMarkerFactory realFactory = (org.slf4j.IMarkerFactory) mh.invoke(IMPL);
            return new IMarkerFactoryAdapter(realFactory);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    // SLF4J API
    public String getMarkerFactoryClassStr() {
        return IMarkerFactoryAdapter.class.getCanonicalName();
    }

    @SdkTestInternalApi
    Object getActualStaticMarkerBinder() {
        return IMPL;
    }
}
