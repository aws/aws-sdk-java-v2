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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.IMarkerFactory;

public class IMarkerFactoryAdapterTest {
    private IMarkerFactory mockFactory;
    private IMarkerFactoryAdapter factoryAdapter;

    @BeforeEach
    public void setup() {
        mockFactory = mock(IMarkerFactory.class);
        factoryAdapter = new IMarkerFactoryAdapter(mockFactory);
    }

    @Test
    public void getMarker_delegatesCall() {
        factoryAdapter.getMarker("MyMarker");
        verify(mockFactory).getMarker("MyMarker");
    }

    @Test
    public void exists_delegatesCall() {
        factoryAdapter.exists("MyMarker");
        verify(mockFactory).exists("MyMarker");
    }

    @Test
    public void detachMarker_delegatesCall() {
        factoryAdapter.detachMarker("MyMarker");
        verify(mockFactory).detachMarker("MyMarker");
    }

    @Test
    public void getDetachedMarker_delegatesCall() {
        factoryAdapter.getDetachedMarker("MyMarker");
        verify(mockFactory).getDetachedMarker("MyMarker");
    }

}
