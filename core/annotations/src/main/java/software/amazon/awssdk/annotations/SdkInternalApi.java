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
import java.lang.annotation.Target;

/**
 * Marks APIs that should not be used by SDK users and are internal to the AWS SDK for Java v2, subject to change without notice.
 * 
 * <p><b>WARNING:</b> Elements annotated with {@code @SdkInternalApi} are not part of the public API.
 * They may be modified or removed in any release without warning, including minor and patch releases.
 * 
 * <p><b>Intended for:</b> Internal SDK implementation only. This annotation indicates that the
 * marked element should not be used outside its defining module.
 * 
 * @see SdkProtectedApi
 * @see SdkPublicApi
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@SdkProtectedApi
public @interface SdkInternalApi {
}
