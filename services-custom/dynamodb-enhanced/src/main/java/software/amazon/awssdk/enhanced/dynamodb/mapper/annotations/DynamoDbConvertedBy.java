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
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;

/**
 * Associates a custom {@link AttributeConverter} with this attribute. This annotation is optional and takes
 * precedence over any converter for this type provided by the table schema {@link AttributeConverterProvider}
 * if it exists. Use custom AttributeConverterProvider when you have specific needs for type conversion
 * that the defaults do not cover.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@SdkPublicApi
public @interface DynamoDbConvertedBy {
    Class<? extends AttributeConverter> value();
}
