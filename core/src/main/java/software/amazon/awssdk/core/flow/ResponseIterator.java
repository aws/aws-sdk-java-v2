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

package software.amazon.awssdk.core.flow;

import java.util.Iterator;

/**
 * Iterator that provides access to a Response object.
 *
 * @param <ResponseT> Type of Response object.
 * @param <ItemT> Type of item being iterated.
 */
public final class ResponseIterator<ResponseT, ItemT> implements Iterator<ItemT> {

    private final Iterator<ItemT> iterator;
    private final ResponseT response;

    public ResponseIterator(Iterator<ItemT> iterator, ResponseT response) {
        this.iterator = iterator;
        this.response = response;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public ItemT next() {
        return iterator.next();
    }

    public ResponseT response() {
        return response;
    }
}
