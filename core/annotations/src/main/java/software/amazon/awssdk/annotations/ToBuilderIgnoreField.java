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

package software.amazon.awssdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to suppress certain fields from being considered in the spot-bugs rule for toBuilder(). This annotation must be
 * attached to the toBuilder() method to function.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@SdkProtectedApi
public @interface ToBuilderIgnoreField {
    /**
     * Specify which fields to ignore in the to-builder spotbugs rule.
     */
    String[] value();
}
