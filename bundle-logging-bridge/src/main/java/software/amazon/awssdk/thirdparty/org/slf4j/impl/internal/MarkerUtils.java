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

/**
 * Utility for converting {@code Marker}s back and forth between the shaded and unshaded interfaces.
 */
@SdkInternalApi
public final class MarkerUtils {
    private MarkerUtils() {
    }

    public static org.slf4j.Marker asUnshaded(software.amazon.awssdk.thirdparty.org.slf4j.Marker shaded) {
        if (shaded instanceof ShadedMarkerAdapter) {
            return ((ShadedMarkerAdapter) shaded).getUnshaded();
        }
        return new UnshadedMarkerAdapter(shaded);
    }

    public static software.amazon.awssdk.thirdparty.org.slf4j.Marker asShaded(org.slf4j.Marker unshaded) {
        if (unshaded instanceof UnshadedMarkerAdapter) {
            return ((UnshadedMarkerAdapter) unshaded).getShaded();
        }
        return new ShadedMarkerAdapter(unshaded);
    }
}
