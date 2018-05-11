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

package software.amazon.awssdk.regions.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;

public class AwsRegionProviderChainTest {

    @Test
    public void firstProviderInChainGivesRegionInformation_DoesNotConsultOtherProviders() {
        AwsRegionProvider providerOne = mock(AwsRegionProvider.class);
        AwsRegionProvider providerTwo = mock(AwsRegionProvider.class);
        AwsRegionProvider providerThree = mock(AwsRegionProvider.class);
        AwsRegionProviderChain chain = new AwsRegionProviderChain(providerOne, providerTwo,
                                                                  providerThree);
        final Region expectedRegion = Region.of("some-region-string");
        when(providerOne.getRegion()).thenReturn(expectedRegion);
        assertEquals(expectedRegion, chain.getRegion());

        verify(providerTwo, never()).getRegion();
        verify(providerThree, never()).getRegion();
    }

    @Test
    public void lastProviderInChainGivesRegionInformation() {
        final Region expectedRegion = Region.of("some-region-string");
        AwsRegionProviderChain chain = new AwsRegionProviderChain(new NeverAwsRegionProvider(),
                                                                  new NeverAwsRegionProvider(),
                                                                  new StaticAwsRegionProvider(
                                                                          expectedRegion));
        assertEquals(expectedRegion, chain.getRegion());
    }

    @Test
    public void providerThrowsException_ContinuesToNextInChain() {
        final Region expectedRegion = Region.of("some-region-string");
        AwsRegionProviderChain chain = new AwsRegionProviderChain(new NeverAwsRegionProvider(),
                                                                  new FaultyAwsRegionProvider(),
                                                                  new StaticAwsRegionProvider(
                                                                          expectedRegion));
        assertEquals(expectedRegion, chain.getRegion());
    }

    /**
     * Only Exceptions should be caught and continued, Errors should propagate to caller and short
     * circuit the chain.
     */
    @Test(expected = Error.class)
    public void providerThrowsError_DoesNotContinueChain() {
        final Region expectedRegion = Region.of("some-region-string");
        AwsRegionProviderChain chain = new AwsRegionProviderChain(new NeverAwsRegionProvider(),
                                                                  new FatalAwsRegionProvider(),
                                                                  new StaticAwsRegionProvider(
                                                                          expectedRegion));
        assertEquals(expectedRegion, chain.getRegion());
    }

    @Test
    public void noProviderGivesRegion_ReturnsNull() {
        AwsRegionProviderChain chain = new AwsRegionProviderChain(new NeverAwsRegionProvider(),
                                                                  new NeverAwsRegionProvider(),
                                                                  new NeverAwsRegionProvider());
        assertNull(chain.getRegion());
    }

    private static class NeverAwsRegionProvider implements AwsRegionProvider {
        @Override
        public Region getRegion() throws SdkClientException {
            return null;
        }
    }

    private static class StaticAwsRegionProvider implements AwsRegionProvider {
        private final Region region;

        public StaticAwsRegionProvider(Region region) {
            this.region = region;
        }

        @Override
        public Region getRegion() {
            return region;
        }
    }

    private static class FaultyAwsRegionProvider implements AwsRegionProvider {
        @Override
        public Region getRegion() throws SdkClientException {
            throw new SdkClientException("Unable to fetch region info");
        }
    }

    private static class FatalAwsRegionProvider implements AwsRegionProvider {
        @Override
        public Region getRegion() throws SdkClientException {
            throw new Error("Something really bad happened");
        }
    }
}
