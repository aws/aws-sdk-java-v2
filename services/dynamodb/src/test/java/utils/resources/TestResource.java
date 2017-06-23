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

/**
 * An interface which represents a resource to be used in a test case.
 * <p>
 * Note that sub-classes implementing this interface must provide a no-arg
 * constructor.
 */
public interface TestResource {

    /**
     * Create/initialize the resource which this TestResource represents.
     *
     * @param waitTillFinished Whether this method should block until the resource is fully
     *                         initialized.
     */
    public void create(boolean waitTillFinished);

    /**
     * Delete the resource which this TestResource represents.
     *
     * @param waitTillFinished Whether this method should block until the resource is fully
     *                         initialized.
     */
    public void delete(boolean waitTillFinished);

    /**
     * Returns the current status of the resource which this TestResource
     * represents.
     */
    ResourceStatus getResourceStatus();

    /**
     * Enum of all the generalized resource statuses.
     */
    public static enum ResourceStatus {
        /**
         * The resource is currently available, and it is compatible with the
         * required resource.
         */
        AVAILABLE,
        /**
         * The resource does not exist and there is no existing resource that is
         * incompatible.
         */
        NOT_EXIST,
        /**
         * There is an existing resource that has to be removed before creating
         * the required resource. For example, DDB table with the same name but
         * different table schema.
         */
        EXIST_INCOMPATIBLE_RESOURCE,
        /**
         * The resource is in transient state (e.g. creating/deleting/updating)
         */
        TRANSIENT,
    }
}
