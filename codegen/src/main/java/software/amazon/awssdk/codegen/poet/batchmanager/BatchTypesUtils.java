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

package software.amazon.awssdk.codegen.poet.batchmanager;

import static software.amazon.awssdk.utils.internal.CodegenNamingUtils.uppercaseFirstChar;

import com.squareup.javapoet.ClassName;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.BatchManagerMethods;

/**
 * Conenvience methods to access the batch types and methods listed in the customization.config
 * TODO: would it make more sense to instantiate this as a class like PoetExtensions instead of making this a static utils class?
 */
public final class BatchTypesUtils {

    private BatchTypesUtils() {
    }

    public static ClassName getRequestType(Map.Entry<String, BatchManagerMethods> batchFunctions, String modelPackage) {
        String requestMethodName = uppercaseFirstChar(batchFunctions.getKey()) + "Request";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getResponseType(Map.Entry<String, BatchManagerMethods> batchFunctions, String modelPackage) {
        String requestMethodName = uppercaseFirstChar(batchFunctions.getKey()) + "Response";
        return getType(requestMethodName, modelPackage);
    }

    public static String getBatchRequestMethod(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        return batchFunctions.getValue().getBatchMethod();
    }

    public static ClassName getBatchRequestType(Map.Entry<String, BatchManagerMethods> batchFunctions, String modelPackage) {
        String requestMethodName = uppercaseFirstChar(getBatchRequestMethod(batchFunctions)) + "Request";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getBatchRequestEntryType(Map.Entry<String, BatchManagerMethods> batchFunctions, String modelPackage) {
        String requestMethodName = batchFunctions.getValue().getBatchRequestEntry();
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getBatchResponseType(Map.Entry<String, BatchManagerMethods> batchFunctions, String modelPackage) {
        String requestMethodName = uppercaseFirstChar(batchFunctions.getValue().getBatchMethod()) + "Response";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getSuccessBatchEntry(Map.Entry<String, BatchManagerMethods> batchFunctions, String modelPackage) {
        return getType(batchFunctions.getValue().getSuccessBatchEntry(), modelPackage);
    }

    public static ClassName getErrorBatchEntry(Map.Entry<String, BatchManagerMethods> batchFunctions, String modelPackage) {
        String errorBatchEntry = batchFunctions.getValue().getErrorBatchEntry();
        if (errorBatchEntry == null) {
            return getSuccessBatchEntry(batchFunctions, modelPackage);
        }
        return getType(errorBatchEntry, modelPackage);
    }

    public static String getErrorEntriesMethod(Map.Entry<String, BatchManagerMethods> batchFunctions) {
        String errorEntriesMethod = batchFunctions.getValue().getErrorEntriesMethod();
        if (errorEntriesMethod == null) {
            return batchFunctions.getValue().getSuccessEntriesMethod();
        }
        return errorEntriesMethod;
    }

    public static ClassName getType(String type, String modelPackage) {
        return ClassName.get(modelPackage, type);
    }

}
