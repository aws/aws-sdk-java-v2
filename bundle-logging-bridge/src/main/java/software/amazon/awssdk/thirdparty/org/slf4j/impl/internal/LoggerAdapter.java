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

package software.amazon.awssdk.thirdparty.org.slf4j.impl.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.thirdparty.org.slf4j.Logger;
import software.amazon.awssdk.thirdparty.org.slf4j.Marker;

/**
 * Adapts from the shaded {@link Logger} to the unshaded {@link org.slf4j.Logger}.
 */
@SdkInternalApi
public class LoggerAdapter implements Logger {
    private final org.slf4j.Logger impl;

    public LoggerAdapter(org.slf4j.Logger impl) {
        this.impl = impl;
    }

    @Override
    public String getName() {
        return impl.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return impl.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        impl.trace(s);
    }

    @Override
    public void trace(String s, Object o) {
        impl.trace(s, o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        impl.trace(s, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        impl.trace(s, objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        impl.trace(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return impl.isTraceEnabled(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public void trace(Marker marker, String s) {
        impl.trace(MarkerUtils.asUnshaded(marker), s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        impl.trace(MarkerUtils.asUnshaded(marker), s, o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        impl.trace(MarkerUtils.asUnshaded(marker), s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        impl.trace(MarkerUtils.asUnshaded(marker), s, objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        impl.trace(MarkerUtils.asUnshaded(marker), s, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return impl.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        impl.debug(s);
    }

    @Override
    public void debug(String s, Object o) {
        impl.debug(s, o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        impl.debug(s, o, o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        impl.debug(s, objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        impl.debug(s, throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return impl.isDebugEnabled(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public void debug(Marker marker, String s) {
        impl.debug(MarkerUtils.asUnshaded(marker), s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        impl.debug(MarkerUtils.asUnshaded(marker), s, o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        impl.debug(MarkerUtils.asUnshaded(marker), s, o, o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        impl.debug(MarkerUtils.asUnshaded(marker), s, objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        impl.debug(MarkerUtils.asUnshaded(marker), s, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return impl.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        impl.info(s);
    }

    @Override
    public void info(String s, Object o) {
        impl.info(s, o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        impl.info(s, o, o1);
    }

    @Override
    public void info(String s, Object... objects) {
        impl.info(s, objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        impl.info(s, throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return impl.isInfoEnabled(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public void info(Marker marker, String s) {
        impl.info(MarkerUtils.asUnshaded(marker), s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        impl.info(MarkerUtils.asUnshaded(marker), s, o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        impl.info(MarkerUtils.asUnshaded(marker), s, o, o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        impl.info(MarkerUtils.asUnshaded(marker), s, objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        impl.info(MarkerUtils.asUnshaded(marker), s, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return impl.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        impl.warn(s);
    }

    @Override
    public void warn(String s, Object o) {
        impl.warn(s, o);
    }

    @Override
    public void warn(String s, Object... objects) {
        impl.warn(s, objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        impl.warn(s, o, o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        impl.warn(s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return impl.isWarnEnabled(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public void warn(Marker marker, String s) {
        impl.warn(MarkerUtils.asUnshaded(marker), s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        impl.warn(MarkerUtils.asUnshaded(marker), s, o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        impl.warn(MarkerUtils.asUnshaded(marker), s, o, o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        impl.warn(MarkerUtils.asUnshaded(marker), s, objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        impl.warn(MarkerUtils.asUnshaded(marker), s, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return impl.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        impl.error(s);
    }

    @Override
    public void error(String s, Object o) {
        impl.error(s, o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        impl.error(s, o, o1);
    }

    @Override
    public void error(String s, Object... objects) {
        impl.error(s, objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        impl.error(s, throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return impl.isErrorEnabled(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public void error(Marker marker, String s) {
        impl.error(MarkerUtils.asUnshaded(marker), s);
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        impl.error(MarkerUtils.asUnshaded(marker), s, o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        impl.error(MarkerUtils.asUnshaded(marker), s, o, o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        impl.error(MarkerUtils.asUnshaded(marker), s, objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        impl.error(MarkerUtils.asUnshaded(marker), s, throwable);
    }
}
