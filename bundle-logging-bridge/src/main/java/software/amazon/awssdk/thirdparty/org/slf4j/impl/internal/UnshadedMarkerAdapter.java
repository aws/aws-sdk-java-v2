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
import java.util.function.Consumer;
import org.slf4j.Marker;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Adapts a shaded {@link software.amazon.awssdk.thirdparty.org.slf4j.Marker} to the unshaded {@link Marker}.
 */
@SdkInternalApi
public class UnshadedMarkerAdapter implements Marker {
    private static final long serialVersionUID = -2627393415659340178L;

    private final software.amazon.awssdk.thirdparty.org.slf4j.Marker shaded;

    public UnshadedMarkerAdapter(software.amazon.awssdk.thirdparty.org.slf4j.Marker shaded) {
        this.shaded = shaded;
    }

    public software.amazon.awssdk.thirdparty.org.slf4j.Marker getShaded() {
        return shaded;
    }

    @Override
    public String getName() {
        return shaded.getName();
    }

    @Override
    public void add(Marker reference) {
        shaded.add(MarkerUtils.asShaded(reference));
    }

    @Override
    public boolean remove(Marker reference) {
        return shaded.remove(MarkerUtils.asShaded(reference));
    }

    @Override
    public boolean hasChildren() {
        return shaded.hasChildren();
    }

    @Override
    public boolean hasReferences() {
        return shaded.hasReferences();
    }

    @Override
    public Iterator<Marker> iterator() {
        return new IteratorAdapter(shaded.iterator());
    }

    @Override
    public boolean contains(Marker other) {
        return shaded.contains(MarkerUtils.asShaded(other));
    }

    @Override
    public boolean contains(String name) {
        return shaded.contains(name);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Marker)) {
            return false;
        }
        Marker that = (Marker) o;
        return shaded.getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return shaded.hashCode();
    }

    private static class IteratorAdapter implements Iterator<Marker> {
        private final Iterator<software.amazon.awssdk.thirdparty.org.slf4j.Marker> shaded;

        IteratorAdapter(Iterator<software.amazon.awssdk.thirdparty.org.slf4j.Marker> shaded) {
            this.shaded = shaded;
        }

        @Override
        public boolean hasNext() {
            return shaded.hasNext();
        }

        @Override
        public Marker next() {
            return MarkerUtils.asUnshaded(shaded.next());
        }

        @Override
        public void remove() {
            shaded.remove();
        }

        @Override
        public void forEachRemaining(Consumer<? super Marker> action) {
            shaded.forEachRemaining(m -> action.accept(MarkerUtils.asUnshaded(m)));
        }
    }
}
