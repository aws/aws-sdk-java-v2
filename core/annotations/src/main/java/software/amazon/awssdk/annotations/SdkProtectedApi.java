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
 * Marks APIs that should not be used by SDK users and are intended for SDK internal classes shared across different modules.
 *
 * <p><b>IMPORTANT:</b> Elements annotated with {@code @SdkProtectedApi} must maintain backward
 * compatibility. Breaking changes will break older versions of generated clients, even if they don't directly impact SDK users.
 *
 * <p><b>Intended for:</b> Generated service clients and internal SDK modules that support them.
 * These APIs form the contract between the SDK core and generated code.
 *
 * <p><b>Stability guarantee:</b> Protected APIs should not introduce breaking changes, as this
 * would require regenerating and redeploying all service clients.
 *
 * @see SdkInternalApi
 * @see SdkPublicApi
 */
@Documented
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD})
@SdkProtectedApi
public @interface SdkProtectedApi {
}
