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

import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.http.async.SdkAsyncHttpService;

module software.amazon.awssdk.core {

    //aws-sdk-java-v2 dependencies
    requires transitive software.amazon.awssdk.annotation;
    requires transitive software.amazon.awssdk.utils;
    requires software.amazon.awssdk.http;

    requires java.xml;
    requires slf4j.api;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires ion.java;
    requires com.fasterxml.jackson.dataformat.cbor;
    requires java.xml.ws.annotation;

    exports software.amazon.awssdk.core;
    exports software.amazon.awssdk.core.interceptor;
    exports software.amazon.awssdk.core.runtime.auth;
    exports software.amazon.awssdk.core.exception;
    exports software.amazon.awssdk.core.client;
    exports software.amazon.awssdk.core.http;
    exports software.amazon.awssdk.core.protocol.json;
    exports software.amazon.awssdk.core.runtime.transform;
    exports software.amazon.awssdk.core.util.json;
    exports software.amazon.awssdk.core.util;
    exports software.amazon.awssdk.core.internal.collections;
    exports software.amazon.awssdk.core.runtime.io;
    exports software.amazon.awssdk.core.config;
    exports software.amazon.awssdk.core.config.defaults;
    exports software.amazon.awssdk.core.retry.conditions;
    exports software.amazon.awssdk.core.retry;
    exports software.amazon.awssdk.core.client.builder;
    exports software.amazon.awssdk.core.runtime.http.response;
    exports software.amazon.awssdk.core.protocol;
    exports software.amazon.awssdk.core.async;
    exports software.amazon.awssdk.core.sync;
    exports software.amazon.awssdk.core.http.pipeline.stages;
    exports software.amazon.awssdk.core.pagination.async;
    exports software.amazon.awssdk.core.runtime;
    exports software.amazon.awssdk.core.runtime.adapters.types;
    exports software.amazon.awssdk.core.protocol.json.internal;
    exports software.amazon.awssdk.core.pagination;

    uses SdkHttpService;
    uses SdkAsyncHttpService;
}