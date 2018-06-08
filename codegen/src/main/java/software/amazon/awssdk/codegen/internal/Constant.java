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

package software.amazon.awssdk.codegen.internal;

public final class Constant {

    public static final String CODEGEN_CONFIG_FILE = "codegen.config";

    public static final String CUSTOMIZATION_CONFIG_FILE = "customization.config";

    public static final String ASYNC_CLIENT_INTERFACE_NAME_PATTERN = "%sAsyncClient";
    public static final String ASYNC_CLIENT_CLASS_NAME_PATTERN = "Default%sAsyncClient";
    public static final String ASYNC_BUILDER_INTERFACE_NAME_PATTERN = "%sAsyncClientBuilder";
    public static final String ASYNC_BUILDER_CLASS_NAME_PATTERN = "Default%sAsyncClientBuilder";

    public static final String SYNC_CLIENT_INTERFACE_NAME_PATTERN = "%sClient";
    public static final String SYNC_CLIENT_CLASS_NAME_PATTERN = "Default%sClient";
    public static final String SYNC_BUILDER_INTERFACE_NAME_PATTERN = "%sClientBuilder";
    public static final String SYNC_BUILDER_CLASS_NAME_PATTERN = "Default%sClientBuilder";

    public static final String BASE_BUILDER_INTERFACE_NAME_PATTERN = "%sBaseClientBuilder";
    public static final String BASE_BUILDER_CLASS_NAME_PATTERN = "Default%sBaseClientBuilder";

    public static final String BASE_EXCEPTION_NAME_PATTERN = "%sException";

    public static final String BASE_REQUEST_NAME_PATTERN = "%sRequest";

    public static final String BASE_RESPONSE_NAME_PATTERN = "%sResponse";

    public static final String PROTOCOL_CONFIG_LOCATION = "/protocol-config/%s.json";

    public static final String JAVA_FILE_NAME_SUFFIX = ".java";

    public static final String PROPERTIES_FILE_NAME_SUFFIX = ".properties";

    public static final String PACKAGE_NAME_CLIENT_PATTERN = "%s";

    public static final String PACKAGE_NAME_MODEL_PATTERN = "%s.model";

    public static final String PACKAGE_NAME_TRANSFORM_PATTERN = "%s.transform";

    public static final String PACKAGE_NAME_PAGINATORS_PATTERN = "%s.paginators";

    public static final String PACKAGE_NAME_SMOKE_TEST_PATTERN = "%s.smoketests";

    public static final String PACKAGE_NAME_CUSTOM_AUTH_PATTERN = "%s.auth";

    public static final String AUTH_POLICY_ENUM_CLASS_DIR = "software/amazon/awssdk/auth/policy/actions";

    public static final String REQUEST_CLASS_SUFFIX = "Request";

    public static final String RESPONSE_CLASS_SUFFIX = "Response";

    public static final String EXCEPTION_CLASS_SUFFIX = "Exception";

    public static final String FAULT_CLASS_SUFFIX = "Fault";

    public static final String VARIABLE_NAME_SUFFIX = "Value";

    public static final String AUTHORIZER_NAME_PREFIX = "I";

    public static final String LF = System.lineSeparator();

    public static final String AWS_DOCS_HOST = "docs.aws.amazon.com";

    public static final String APPROVED_SIMPLE_METHOD_VERBS = "(get|list|describe|lookup|batchGet).*";

    private Constant() {
    }
}
