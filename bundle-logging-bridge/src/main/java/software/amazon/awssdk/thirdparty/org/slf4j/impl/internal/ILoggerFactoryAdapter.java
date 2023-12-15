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

package software.amazon.awssdk.thirdparty.org.slf4j.impl.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.thirdparty.org.slf4j.ILoggerFactory;
import software.amazon.awssdk.thirdparty.org.slf4j.Logger;

/**
 * Adapts a normal, unshaded {@link org.slf4j.ILoggerFactory} to the shaded {@link ILoggerFactory}.
 */
@SdkInternalApi
public class ILoggerFactoryAdapter implements ILoggerFactory {
    private final org.slf4j.ILoggerFactory impl;

    public ILoggerFactoryAdapter(org.slf4j.ILoggerFactory impl) {
        this.impl = impl;
    }

    @Override
    public Logger getLogger(String s) {
        return new LoggerAdapter(impl.getLogger(s));
    }
}
