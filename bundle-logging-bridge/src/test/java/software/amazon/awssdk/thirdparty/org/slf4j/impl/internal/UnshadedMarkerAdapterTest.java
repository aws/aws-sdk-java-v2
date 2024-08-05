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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.thirdparty.org.slf4j.Marker;

public class UnshadedMarkerAdapterTest {
    private Marker mockMarker;
    private UnshadedMarkerAdapter markerAdapter;

    @BeforeEach
    public void setup() {
        mockMarker = mock(Marker.class);
        markerAdapter = new UnshadedMarkerAdapter(mockMarker);
    }

    @Test
    public void getName_delegatesCall() {
        markerAdapter.getName();
        verify(mockMarker).getName();
    }

    @Test
    public void add_delegatesCall() {
        Marker reference = mock(Marker.class);
        UnshadedMarkerAdapter referenceAdapter = new UnshadedMarkerAdapter(reference);
        markerAdapter.add(referenceAdapter);
        verify(mockMarker).add(reference);
    }

    @Test
    public void remove_delegatesCall() {
        Marker reference = mock(Marker.class);
        UnshadedMarkerAdapter referenceAdapter = new UnshadedMarkerAdapter(reference);
        markerAdapter.remove(referenceAdapter);
        verify(mockMarker).remove(reference);
    }

    @Test
    public void hasChildren_delegatesCall() {
        markerAdapter.hasChildren();
        verify(mockMarker).hasChildren();
    }

    @Test
    public void hasReferences_delegatesCall() {
        markerAdapter.hasReferences();
        verify(mockMarker).hasReferences();
    }

    @Test
    public void contains_delegatesCall() {
        markerAdapter.contains("OtherMarker");
        verify(mockMarker).contains("OtherMarker");

        Marker reference = mock(Marker.class);
        UnshadedMarkerAdapter referenceAdapter = new UnshadedMarkerAdapter(reference);
        markerAdapter.contains(referenceAdapter);
        verify(mockMarker).contains(reference);
    }

    @Test
    public void iterator_delegatesCall() {
        markerAdapter.iterator();
        verify(mockMarker).iterator();
    }

    @Test
    public void iterator_adaptsReferences() {
        Marker ref1 = mock(Marker.class);
        when(ref1.getName()).thenReturn("ref1");

        Marker ref2 = mock(Marker.class);
        when(ref2.getName()).thenReturn("ref2");

        List<Marker> mockReferences = Arrays.asList(ref1, ref2);

        when(mockMarker.iterator()).thenAnswer(i -> mockReferences.iterator());

        Iterator<org.slf4j.Marker> iterator = markerAdapter.iterator();

        List<String> referenceNames = new ArrayList<>();
        iterator.forEachRemaining(s -> referenceNames.add(s.getName()));

        assertThat(referenceNames).containsExactly("ref1", "ref2");
    }

    @Test
    public void iterator_delegatesHasNext() {
        Iterator<Marker> mockIter = mock(Iterator.class);
        when(mockMarker.iterator()).thenReturn(mockIter);
        markerAdapter.iterator().hasNext();
        verify(mockIter).hasNext();
    }

    @Test
    public void iterator_delegatesNext() {
        Iterator<Marker> mockIter = mock(Iterator.class);
        when(mockMarker.iterator()).thenReturn(mockIter);
        markerAdapter.iterator().next();
        verify(mockIter).next();
    }

    @Test
    public void iterator_delegatesRemove() {
        Iterator<Marker> mockIter = mock(Iterator.class);
        when(mockMarker.iterator()).thenReturn(mockIter);
        markerAdapter.iterator().remove();
        verify(mockIter).remove();
    }

    @Test
    public void iterator_delegatesForEachRemaining() {
        Iterator<Marker> mockIter = mock(Iterator.class);
        when(mockMarker.iterator()).thenReturn(mockIter);
        markerAdapter.iterator().forEachRemaining(m -> {});
        verify(mockIter).forEachRemaining(any(Consumer.class));
    }

    @Test
    public void equals_comparesNames() {
        when(mockMarker.getName()).thenReturn("MyMarker");

        Marker m2 = mock(Marker.class);
        when(m2.getName()).thenReturn("MyMarker");
        UnshadedMarkerAdapter m2Adapter = new UnshadedMarkerAdapter(m2);

        assertThat(markerAdapter).isEqualTo(m2Adapter);
    }

    @Test
    public void hashCode_delegatesCall() {
        UnshadedMarkerAdapter adapter = new UnshadedMarkerAdapter(new TestMarker());
        assertThat(adapter.hashCode()).isEqualTo(42);
    }


    private static class TestMarker implements Marker {

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void add(Marker reference) {

        }

        @Override
        public boolean remove(Marker reference) {
            return false;
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public boolean hasReferences() {
            return false;
        }

        @Override
        public Iterator<Marker> iterator() {
            return null;
        }

        @Override
        public boolean contains(Marker other) {
            return false;
        }

        @Override
        public boolean contains(String name) {
            return false;
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }
}
