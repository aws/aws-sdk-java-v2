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

package software.amazon.awssdk.enhanced.dynamodb.mapper.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Used to explicitly designate a field or getter or setter to participate as an attribute in the mapped database
 * object with a custom name. A string value must be specified to specify a different name for the attribute than the
 * mapper would automatically infer using a naming strategy.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbAttribute {
    /**
     * The attribute name that this property should map to in the DynamoDb record. The value is case sensitive.
     */
    String value();
}
