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

package software.amazon.awssdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * An annotation applied during SDK 2.0 developer preview. This makes note of something we know will change before GA or are
 * unsure about. By applying this annotation and making sure all instances of it are removed before GA, we will make sure not to
 * miss anything we intended to review.
 */
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@SdkProtectedApi
public @interface ReviewBeforeRelease {
    /**
     * An explanation of why we should review this before general availability. Will it definitely change? Are we just testing
     * something?
     */
    String value();
}
