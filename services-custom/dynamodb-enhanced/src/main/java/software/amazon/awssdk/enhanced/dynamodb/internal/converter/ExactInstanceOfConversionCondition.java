/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.converter.ConversionCondition;
import software.amazon.awssdk.enhanced.dynamodb.converter.DefaultConverterChain;
import software.amazon.awssdk.utils.Validate;

/**
 * This is created by {@link ConversionCondition#isExactInstanceOf(Class)}. The parent is just a marker interface, so
 * {@link DefaultConverterChain} casts this to a concrete type to invoke it.
 *
 * <p>
 * {@link ExactInstanceOfConverter} simplifies the process of implementing converters of this type.
 */
@SdkInternalApi
@ThreadSafe
public class ExactInstanceOfConversionCondition implements ConversionCondition {
    private final Class<?> clazz;

    public ExactInstanceOfConversionCondition(Class<?> clazz) {
        Validate.paramNotNull(clazz, "clazz");
        this.clazz = clazz;
    }

    public Class<?> convertedClass() {
        return clazz;
    }
}
