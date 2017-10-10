/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.pagination;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PaginatedItemsIterableTest {

    @Mock
    private SdkIterable pagesIterable;

    @Mock
    private Iterator pagesIterator;

    @Mock
    private Function getItemIteratorFunction;

    @Mock
    private Iterator singlePageItemsIterator;

    private PaginatedItemsIterable itemsIterable;

    @Before
    public void setup() {
        when(pagesIterable.iterator()).thenReturn(pagesIterator);

        when(getItemIteratorFunction.apply(any())).thenReturn(singlePageItemsIterator);

        itemsIterable = new PaginatedItemsIterable(pagesIterable, getItemIteratorFunction);
    }

    @Test
    public void hasNext_ReturnsFalse_WhenItemsAndPagesIteratorHasNoNextElement() {
        when(singlePageItemsIterator.hasNext()).thenReturn(false);
        when(pagesIterator.hasNext()).thenReturn(false);

        assertFalse(itemsIterable.iterator().hasNext());
    }

    @Test
    public void hasNext_ReturnsTrue_WhenItemsIteratorHasNextElement() {
        when(singlePageItemsIterator.hasNext()).thenReturn(true);
        when(pagesIterator.hasNext()).thenReturn(false);

        assertTrue(itemsIterable.iterator().hasNext());
    }

    @Test
    public void hasNextMethodDoesNotRetrieveNextPage_WhenItemsIteratorHasAnElement() {
        when(singlePageItemsIterator.hasNext()).thenReturn(true);

        Iterator itemsIterator = itemsIterable.iterator();
        itemsIterator.hasNext();
        itemsIterator.hasNext();

        // pagesIterator.next() is called only once in ItemsIterator constructor
        // Not called again in ItemsIterator.hasNext() method
        verify(pagesIterator, times(1)).next();
    }

    @Test
    public void hasNextMethodGetsNextPage_WhenCurrentItemsIteratorHasNoElements() {
        when(pagesIterator.hasNext()).thenReturn(true);

        when(singlePageItemsIterator.hasNext()).thenReturn(false)
                                               .thenReturn(true);


        itemsIterable.iterator().hasNext();

        // pagesIterator.next() is called only once in ItemsIterator constructor
        // Called second time in hasNext() method
        verify(pagesIterator, times(2)).next();
    }

    @Test
    public void hasNextMethodGetsNextPage_WhenCurrentItemsIteratorIsNull() {
        when(pagesIterator.hasNext()).thenReturn(true);

        when(getItemIteratorFunction.apply(any())).thenReturn(null, singlePageItemsIterator);

        when(singlePageItemsIterator.hasNext()).thenReturn(true);

        itemsIterable.iterator().hasNext();

        // pagesIterator.next() is called only once in ItemsIterator constructor
        // Called second time in hasNext() method
        verify(pagesIterator, times(2)).next();
    }

    @Test
    public void testNextMethod_WhenIntermediatePages_HasEmptyCollectionOfItems() {
        when(pagesIterator.hasNext()).thenReturn(true);

        // first page has empty items iterator
        Iterator itemsIterator1 = Mockito.mock(Iterator.class);
        when(itemsIterator1.hasNext()).thenReturn(false);

        // second page has empty items iterator
        Iterator itemsIterator2 = Mockito.mock(Iterator.class);
        when(itemsIterator2.hasNext()).thenReturn(false);

        // third page has empty items iterator
        Iterator itemsIterator3 = Mockito.mock(Iterator.class);
        when(itemsIterator3.hasNext()).thenReturn(false);

        // fourth page is non empty
        Iterator itemsIterator4 = Mockito.mock(Iterator.class);
        when(itemsIterator4.hasNext()).thenReturn(true);

        when(getItemIteratorFunction.apply(any())).thenReturn(itemsIterator1, itemsIterator2, itemsIterator3, itemsIterator4);

        itemsIterable.iterator().next();

        verify(itemsIterator1, times(0)).next();
        verify(itemsIterator2, times(0)).next();
        verify(itemsIterator3, times(0)).next();
        verify(itemsIterator4, times(1)).next();

        verify(getItemIteratorFunction, times(4)).apply(any());
    }
}
