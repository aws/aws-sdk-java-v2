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
 * Marks an API that is error-prone to use: implementing, overriding, or calling it incorrectly, or
 * configuring it with an unsafe value, compiles cleanly but can fail or misbehave at runtime rather
 * than reporting a clear error.
 *
 * <p>The annotation records the risk information on the API in a structured form: {@link #cautionWhen()}
 * classifies which kind of use is error-prone, {@link #guidance()} explains the contract that must
 * be upheld, {@link #saferAlternative()} points to a lower-risk approach, and {@link #link()} points
 * to further documentation.
 *
 * <p>This is an advisory marker: it does not gate compilation and imposes no runtime behavior.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
@SdkProtectedApi
public @interface SdkAdvancedApi {

    /**
     * Which kind of use of this API is error-prone: IMPLEMENTED if the risk is in implementing
     * or extending the annotated type, OVERRIDDEN if it is in overriding the annotated method,
     * CONFIGURED if it is in setting the annotated field or option, and CALLED if it is in
     * calling the annotated method (for example a factory that accepts an object you supply and does
     * not shield you from that object's contract). Required.
     */
    Usage cautionWhen();

    /**
     * Explains why this API is error-prone and what you must uphold to use it safely: the parts of
     * the contract that are easy to get wrong and the failure that results if they are not met.
     */
    String guidance();

    /**
     * An optional pointer to the recommended safer approach that satisfies the same need
     * without the risk (for example, a factory method that implements the contract
     * correctly). Empty when there is no direct alternative.
     */
    String saferAlternative() default "";

    /**
     * An optional link to documentation explaining the risk and correct usage in more
     * depth. Empty when there is no dedicated page.
     */
    String link() default "";

    enum Usage {
        IMPLEMENTED,
        OVERRIDDEN,
        CONFIGURED,
        CALLED
    }
}
