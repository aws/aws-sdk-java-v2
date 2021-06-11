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

module software.amazon.awssdk.modulepath.tests {
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.http.urlconnection;
    requires software.amazon.awssdk.http.apache;
    requires software.amazon.awssdk.http.nio.netty;
    requires software.amazon.awssdk.http;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.awscore;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.services.s3;
    requires software.amazon.awssdk.protocol.tests;
    requires org.reactivestreams;
    requires software.amazon.awssdk.utils;
    requires software.amazon.awssdk.testutils.service;

    requires org.slf4j;
    requires slf4j.simple;
}
