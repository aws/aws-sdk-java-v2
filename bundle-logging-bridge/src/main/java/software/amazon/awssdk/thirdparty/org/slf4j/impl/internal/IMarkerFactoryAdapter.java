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
import software.amazon.awssdk.thirdparty.org.slf4j.IMarkerFactory;
import software.amazon.awssdk.thirdparty.org.slf4j.Marker;

/**
 * Adapts a normal, unshaded {@link org.slf4j.IMarkerFactory} to the shaded {@link IMarkerFactory}.
 */
@SdkInternalApi
public class IMarkerFactoryAdapter implements IMarkerFactory {
    private final org.slf4j.IMarkerFactory impl;

    public IMarkerFactoryAdapter(org.slf4j.IMarkerFactory impl) {
        this.impl = impl;
    }

    @Override
    public Marker getMarker(String s) {
        return new ShadedMarkerAdapter(impl.getMarker(s));
    }

    @Override
    public boolean exists(String s) {
        return impl.exists(s);
    }

    @Override
    public boolean detachMarker(String s) {
        return impl.detachMarker(s);
    }

    @Override
    public Marker getDetachedMarker(String s) {
        return new ShadedMarkerAdapter(impl.getDetachedMarker(s));
    }
}
