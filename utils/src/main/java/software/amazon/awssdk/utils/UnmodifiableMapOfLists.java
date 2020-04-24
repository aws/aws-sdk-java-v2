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

package software.amazon.awssdk.utils;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * An unmodifiable view of a {@code Map<T, List<U>>}. Created using {@link CollectionUtils#unmodifiableMapOfLists(Map)}.
 */
@SdkInternalApi
class UnmodifiableMapOfLists<T, U> implements Map<T, List<U>> {
    private final Map<T, List<U>> delegate;

    UnmodifiableMapOfLists(Map<T, List<U>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public List<U> get(Object key) {
        return delegate.get(key);
    }

    @Override
    public List<U> getOrDefault(Object key, List<U> defaultValue) {
        return unmodifiableList(delegate.getOrDefault(key, defaultValue));
    }

    @Override
    public List<U> put(T key, List<U> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<U> remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends T, ? extends List<U>> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<T> keySet() {
        return Collections.unmodifiableSet(delegate.keySet());
    }

    @Override
    public Collection<List<U>> values() {
        return new UnmodifiableCollection<>(delegate.values());
    }

    @Override
    public Set<Entry<T, List<U>>> entrySet() {
        Set<? extends Entry<T, ? extends List<U>>> entries = delegate.entrySet();
        return new UnmodifiableEntrySet<>(entries);
    }

    @Override
    public void forEach(BiConsumer<? super T, ? super List<U>> action) {
        delegate.forEach((k, v) -> action.accept(k, unmodifiableList(v)));
    }

    @Override
    public void replaceAll(BiFunction<? super T, ? super List<U>, ? extends List<U>> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<U> putIfAbsent(T key, List<U> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(T key, List<U> oldValue, List<U> newValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<U> replace(T key, List<U> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<U> computeIfAbsent(T key, Function<? super T, ? extends List<U>> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<U> computeIfPresent(T key, BiFunction<? super T, ? super List<U>, ? extends List<U>> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<U> compute(T key, BiFunction<? super T, ? super List<U>, ? extends List<U>> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<U> merge(T key, List<U> value,
                         BiFunction<? super List<U>, ? super List<U>, ? extends List<U>> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    private static class UnmodifiableEntrySet<T, U> implements Set<Entry<T, List<U>>> {
        private final Set<? extends Entry<T, ? extends List<U>>> delegate;

        private UnmodifiableEntrySet(Set<? extends Entry<T, ? extends List<U>>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<Entry<T, List<U>>> iterator() {
            return new UnmodifiableEntryIterator<>(delegate.iterator());
        }

        @Override
        public void forEach(Consumer<? super Entry<T, List<U>>> action) {
            delegate.forEach(e -> action.accept(new SimpleImmutableEntry<>(e.getKey(), unmodifiableList(e.getValue()))));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object[] toArray() {
            Object[] result = delegate.toArray();
            for (int i = 0; i < result.length; i++) {
                Entry<T, List<U>> e = (Entry<T, List<U>>) result[i];
                result[i] = new SimpleImmutableEntry<>(e.getKey(), unmodifiableList(e.getValue()));
            }
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <A> A[] toArray(A[] a) {
            // Technically this could give the caller access very brief access to the modifiable entries from a different thread,
            // but that's on them. They had to have done it purposefully with a different thread, and it wouldn't be very
            // reliable.
            Object[] result = delegate.toArray(a);
            for (int i = 0; i < result.length; i++) {
                Entry<T, List<U>> e = (Entry<T, List<U>>) result[i];
                result[i] = new SimpleImmutableEntry<>(e.getKey(), unmodifiableList(e.getValue()));
            }
            return (A[]) result;
        }

        @Override
        public boolean add(Entry<T, List<U>> tListEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends Entry<T, List<U>>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private static class UnmodifiableEntryIterator<T, U> implements Iterator<Entry<T, List<U>>> {
        private final Iterator<? extends Entry<T, ? extends List<U>>> delegate;

        private UnmodifiableEntryIterator(Iterator<? extends Entry<T, ? extends List<U>>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public Entry<T, List<U>> next() {
            Entry<T, ? extends List<U>> next = delegate.next();
            return new SimpleImmutableEntry<>(next.getKey(), unmodifiableList(next.getValue()));
        }
    }

    private static class UnmodifiableCollection<U> implements Collection<List<U>> {
        private final Collection<? extends List<U>> delegate;

        private UnmodifiableCollection(Collection<? extends List<U>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Iterator<List<U>> iterator() {
            return new UnmodifiableListIterator<>(delegate.iterator());
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object[] toArray() {
            Object[] result = delegate.toArray();
            for (int i = 0; i < result.length; i++) {
                result[i] = unmodifiableList((List<U>) result[i]);
            }
            return result;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <A> A[] toArray(A[] a) {
            // Technically this could give the caller access very brief access to the modifiable entries from a different thread,
            // but that's on them. They had to have done it purposefully with a different thread, and it wouldn't be very
            // reliable.
            Object[] result = delegate.toArray(a);
            for (int i = 0; i < result.length; i++) {
                result[i] = unmodifiableList((List<U>) result[i]);
            }
            return (A[]) result;
        }

        @Override
        public boolean add(List<U> us) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends List<U>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private static class UnmodifiableListIterator<U> implements Iterator<List<U>> {
        private final Iterator<? extends List<U>> delegate;

        private UnmodifiableListIterator(Iterator<? extends List<U>> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return delegate.hasNext();
        }

        @Override
        public List<U> next() {
            return unmodifiableList(delegate.next());
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return delegate.equals(obj);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private static <T> List<T> unmodifiableList(List<T> list) {
        if (list == null) {
            return null;
        }

        return Collections.unmodifiableList(list);
    }
}
