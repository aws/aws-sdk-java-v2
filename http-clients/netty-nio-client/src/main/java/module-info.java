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

import software.amazon.awssdk.http.async.SdkAsyncHttpService;
import software.amazon.awssdk.http.nio.netty.NettySdkAsyncHttpService;

module software.amazon.awssdk.http.nio.netty {
    requires software.amazon.awssdk.utils;
    requires software.amazon.awssdk.annotation;
    requires software.amazon.awssdk.http;

    requires io.netty.buffer;
    requires io.netty.codec;
    requires io.netty.codec.http;
    requires io.netty.transport;
    requires io.netty.handler;
    requires io.netty.transport.epoll;
    requires io.netty.common;

    requires org.reactivestreams;
    requires netty.reactive.streams.http;

    requires slf4j.api;

    provides SdkAsyncHttpService with NettySdkAsyncHttpService;
}