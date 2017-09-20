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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for resources required for the test case. It could be applied to
 * either a type (test class) or a method (test method).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiredResources {

    /**
     * An array of RequiredResource annotations
     */
    RequiredResource[] value() default {};

    enum ResourceCreationPolicy {
        /**
         * Existing resource will be reused if it matches the required resource
         * definition (i.e. TestResource.getResourceStatus() returns AVAILABLE).
         */
        REUSE_EXISTING,
        /**
         * Always destroy existing resources (if any) and then recreate new ones for test.
         */
        ALWAYS_RECREATE;
    }

    enum ResourceRetentionPolicy {
        /**
         * Do not delete the created resource after test.
         */
        KEEP,
        /**
         * When used for @RequiredAnnota
         */
        DESTROY_IMMEDIATELY,
        DESTROY_AFTER_ALL_TESTS;
    }

    @interface RequiredResource {

        /**
         * The Class object of the TestResource class
         */
        Class<? extends TestResource> resource();

        /**
         * How the resource should be created before the test starts.
         */
        ResourceCreationPolicy creationPolicy();

        /**
         * Retention policy after the test is done.
         */
        ResourceRetentionPolicy retentionPolicy();
    }

}
