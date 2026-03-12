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

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Utilities for loading of classes and objects which have optional dependencies, and therefore need to be safely checked at
 * runtime in order to use.
 */
@SdkProtectedApi
public final class DependencyValidate {
    private static final Logger LOG = Logger.loggerFor(DependencyValidate.class);

    private DependencyValidate() {
    }

    /**
     * A helpful method that checks that some class is available on the class-path. If it fails to load, it will throw an
     * exception based on why it failed to load. This should be used in cases where certain dependencies are optional, but the
     * dependency is used at compile-time for strong typing
     */
    public static void requireClass(String classPath, String module, String feature) {
        try {
            ClassLoaderHelper.loadClass(classPath, false);
        } catch (ClassNotFoundException e) {
            LOG.debug(() -> "Cannot find the " + classPath + " class: ", e);
            String msg = String.format("Could not load class. You must add a dependency on the '%s' module to enable the %s "
                                       + "feature: ", module, feature);
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not load class (%s): ", classPath), e);
        }
    }
}
