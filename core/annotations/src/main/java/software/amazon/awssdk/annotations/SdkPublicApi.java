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
 * Marks APIs as public and stable for use by SDK users building applications.
 * 
 * <p><b>Stability guarantee:</b> Elements annotated with {@code @SdkPublicApi} are backward
 * compatible.
 * 
 * <p><b>Safe to use for:</b>
 * <ul>
 *   <li>Application code that depends on the AWS SDK</li>
 *   <li>Libraries that build on top of the SDK</li>
 *   <li>Any code requiring stable, long-term API contracts</li>
 * </ul>
 * 
 * <p><b>Intended for:</b> SDK users and external developers. These APIs form the official public
 * interface of the AWS SDK for Java v2.
 * 
 * @see SdkProtectedApi
 * @see SdkInternalApi
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@SdkProtectedApi
public @interface SdkPublicApi {
}
