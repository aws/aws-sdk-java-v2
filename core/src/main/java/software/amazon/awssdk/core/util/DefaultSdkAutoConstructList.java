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

package software.amazon.awssdk.core.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Default implementation of {@link SdkAutoConstructList}.
 * <p />
 * This is an empty, unmodifiable list.
 *
 * @param <T> The element type.
 */
@SdkInternalApi
public final class DefaultSdkAutoConstructList<T> implements SdkAutoConstructList<T> {
    private static final DefaultSdkAutoConstructList INSTANCE = new DefaultSdkAutoConstructList();

    private final List impl = Collections.unmodifiableList(Collections.emptyList());

    private DefaultSdkAutoConstructList() {
    }

    @SuppressWarnings("unchecked")
    public static <T> DefaultSdkAutoConstructList<T> getInstance() {
        return (DefaultSdkAutoConstructList<T>) INSTANCE;
    }

    @Override
    public int size() {
        return impl.size();
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return impl.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return impl.iterator();
    }

    @Override
    public Object[] toArray() {
        return impl.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return (T1[]) impl.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return impl.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return impl.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return impl.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return impl.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        return impl.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return impl.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return impl.retainAll(c);
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public T get(int index) {
        return (T) impl.get(index);
    }

    @Override
    public T set(int index, T element) {
        return (T) impl.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        impl.add(index, element);
    }

    @Override
    public T remove(int index) {
        return (T) impl.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return impl.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return impl.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return impl.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return impl.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return impl.subList(fromIndex, toIndex);
    }

}
