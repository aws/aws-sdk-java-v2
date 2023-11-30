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

import java.util.Iterator;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.thirdparty.org.slf4j.Marker;

/**
 * Adapts an unshaded {@link org.slf4j.Marker} to the shaded {@link Marker}.
 */
@SdkInternalApi
public class ShadedMarkerAdapter implements Marker {
    private final org.slf4j.Marker unshaded;

    public ShadedMarkerAdapter(org.slf4j.Marker unshaded) {
        this.unshaded = unshaded;
    }

    public org.slf4j.Marker getUnshaded() {
        return unshaded;
    }

    @Override
    public String getName() {
        return unshaded.getName();
    }

    @Override
    public void add(Marker marker) {
        unshaded.add(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public boolean remove(Marker marker) {
        return unshaded.remove(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public boolean hasChildren() {
        return unshaded.hasChildren();
    }

    @Override
    public boolean hasReferences() {
        return unshaded.hasReferences();
    }

    @Override
    public Iterator<Marker> iterator() {
        Iterator<org.slf4j.Marker> iterator = unshaded.iterator();
        return new Iterator<Marker>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Marker next() {
                return MarkerUtils.asShaded(iterator.next());
            }
        };
    }

    @Override
    public boolean contains(Marker marker) {
        return unshaded.contains(MarkerUtils.asUnshaded(marker));
    }

    @Override
    public boolean contains(String s) {
        return unshaded.contains(s);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Marker)) {
            return false;
        }
        Marker that = (Marker) o;
        return unshaded.getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return unshaded.hashCode();
    }
}
