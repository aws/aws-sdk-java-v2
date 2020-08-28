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

package software.amazon.awssdk.http.crt.internal;


import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.io.EventLoopGroup;
import software.amazon.awssdk.crt.io.HostResolver;
import software.amazon.awssdk.http.crt.SdkEventLoopGroup;
import software.amazon.awssdk.utils.Logger;

/**
 * Provides access and manages shared {@link SharedCrtResources}s. Uses reference counting to keep track of how many HTTP
 * clients are using the shared resources and will automatically close it when that count reaches zero. The resources
 * are lazily initialized for the first time.
 */
@SdkInternalApi
public final class SharedResourcesManager {
    private static final Logger log = Logger.loggerFor(SharedResourcesManager.class);

    /**
     * Reference count of clients using the shared event loop group and host resolver
     */
    private static int referenceCount = 0;

    /**
     * Lazily initialized shared crt resources
     */
    private static SharedCrtResources sharedCrtResource;

    private SharedResourcesManager() {
    }

    public static synchronized SharedCrtResources sharedCrtResource() {
        if (sharedCrtResource == null) {
            EventLoopGroup sharedSdkEventLoopGroup = SdkEventLoopGroup.builder().build().eventLoopGroup();
            HostResolver sharedHostResolver = new HostResolver(sharedSdkEventLoopGroup);
            ReferenceCountingCrtResource referenceCountingCrtResource = new ReferenceCountingCrtResource();

            sharedCrtResource = new SharedCrtResources(sharedHostResolver, sharedSdkEventLoopGroup, referenceCountingCrtResource);
        }
        referenceCount++;

        return sharedCrtResource;
    }

    /**
     * Crt resource that prevents shutdown and decrements the reference count when a client is closed
     */
    private static final class ReferenceCountingCrtResource extends CrtResource {

        @Override
        public void close() {
            decrementReference();
        }

        /**
         * Decrement the reference count and close the Crt resrouces if necessary.
         */
        private static synchronized void decrementReference() {
            referenceCount--;
            if (referenceCount == 0) {
                log.debug(() -> "Reference count is 0, closing the shared eventLoopGroup and hostResolver");
                sharedCrtResource.eventLoopGroup.close();
                sharedCrtResource.hostResolver.close();
                sharedCrtResource = null;
            }
        }

        @Override
        protected void releaseNativeHandle() {
            // no op
        }

        @Override
        protected boolean canReleaseReferencesImmediately() {
            return false;
        }
    }

    /**
     * A wrapper class for the shared crt resources
     */
    public static final class SharedCrtResources {
        private final HostResolver hostResolver;
        private final EventLoopGroup eventLoopGroup;
        private final ReferenceCountingCrtResource referenceCountingCrtResource;

        public SharedCrtResources(HostResolver hostResolver, EventLoopGroup eventLoopGroup,
                                  ReferenceCountingCrtResource referenceCountingCrtResource) {
            this.hostResolver = hostResolver;
            this.eventLoopGroup = eventLoopGroup;
            this.referenceCountingCrtResource = referenceCountingCrtResource;
        }


        public HostResolver hostResolver() {
            return hostResolver;
        }

        public EventLoopGroup eventLoopGroup() {
            return eventLoopGroup;
        }

        public ReferenceCountingCrtResource referenceCountingCrtResource() {
            return referenceCountingCrtResource;
        }
    }

}
