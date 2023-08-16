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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated element must not be null. Accepts any type.
 * <p>
 * This is useful to tell linting and testing tools that a particular value will never be null. It's not meant to be used on
 * public interfaces as something that customers should rely on.
 */
@Documented
@Target({ElementType.METHOD,
         ElementType.FIELD,
         ElementType.ANNOTATION_TYPE,
         ElementType.CONSTRUCTOR,
         ElementType.PARAMETER})
@Retention(RetentionPolicy.CLASS)
@SdkProtectedApi
public @interface NotNull {
}
