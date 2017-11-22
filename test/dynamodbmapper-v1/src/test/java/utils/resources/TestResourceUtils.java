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

package utils.resources;

import software.amazon.awssdk.testutils.Waiter;
import software.amazon.awssdk.utils.Logger;
import utils.resources.RequiredResources.ResourceCreationPolicy;
import utils.resources.TestResource.ResourceStatus;


public class TestResourceUtils {
    private static final Logger log = Logger.loggerFor(TestResourceUtils.class);

    public static void createResource(TestResource resource, ResourceCreationPolicy policy) throws InterruptedException {
        TestResource.ResourceStatus finalizedStatus = waitForFinalizedStatus(resource);
        if (policy == ResourceCreationPolicy.ALWAYS_RECREATE) {
            if (finalizedStatus != ResourceStatus.NOT_EXIST) {
                resource.delete(true);
            }
            resource.create(true);
        } else if (policy == ResourceCreationPolicy.REUSE_EXISTING) {
            switch (finalizedStatus) {
                case AVAILABLE:
                    log.info(() -> "Found existing resource " + resource + " that could be reused...");
                    return;
                case EXIST_INCOMPATIBLE_RESOURCE:
                    resource.delete(true);
                    resource.create(true);
                    // fallthru
                case NOT_EXIST:
                    resource.create(true);
                    break;
                default:
                    break;
            }
        }
    }

    public static void deleteResource(TestResource resource) throws InterruptedException {
        ResourceStatus finalizedStatus = waitForFinalizedStatus(resource);
        if (finalizedStatus != ResourceStatus.NOT_EXIST) {
            resource.delete(false);
        }
    }

    public static ResourceStatus waitForFinalizedStatus(TestResource resource) throws InterruptedException {
        return Waiter.run(resource::getResourceStatus)
                     .until(status -> status != ResourceStatus.TRANSIENT)
                     .orFail();
    }
}
