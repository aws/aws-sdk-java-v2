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
module software.amazon.awssdk.codegen {

    requires software.amazon.awssdk.awscore;

    //This is fine since we are not exporting any classes
    requires slf4j.api;
    requires freemarker;

    requires com.fasterxml.jackson.annotation;
    requires org.reactivestreams;
    requires org.eclipse.jdt.core;
    requires org.eclipse.text;
    requires java.xml;
    requires com.squareup.javapoet;
    requires java.xml.ws.annotation;
    requires java.compiler;
}