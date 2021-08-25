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

import com.squareup.javapoet.ClassName;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.BatchManager;

/**
 * Conenvience methods to access the batch types and methods listed in the customization.config
 * TODO: would it make more sense to instantiate this as a class like PoetExtensions instead of making this a static utils class?
 */
public final class BatchTypesUtils {

    private BatchTypesUtils() {
    }

    public static ClassName getRequestType(Map.Entry<String, BatchManager> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(batchFunctions.getKey()) + "Request";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getResponseType(Map.Entry<String, BatchManager> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(batchFunctions.getKey()) + "Response";
        return getType(requestMethodName, modelPackage);
    }

    public static String getBatchRequestMethod(Map.Entry<String, BatchManager> batchFunctions) {
        return batchFunctions.getValue().getBatchMethod();
    }

    public static ClassName getBatchRequestType(Map.Entry<String, BatchManager> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(getBatchRequestMethod(batchFunctions)) + "Request";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getBatchRequestEntryType(Map.Entry<String, BatchManager> batchFunctions, String modelPackage) {
        String requestMethodName = batchFunctions.getValue().getBatchRequestEntry();
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getBatchResponseType(Map.Entry<String, BatchManager> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(batchFunctions.getValue().getBatchMethod()) + "Response";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getSuccessBatchEntry(Map.Entry<String, BatchManager> batchFunctions, String modelPackage) {
        return getType(batchFunctions.getValue().getSuccessBatchEntry(), modelPackage);
    }

    public static ClassName getErrorBatchEntry(Map.Entry<String, BatchManager> batchFunctions, String modelPackage) {
        String errorBatchEntry = batchFunctions.getValue().getErrorBatchEntry();
        if (errorBatchEntry == null) {
            return getSuccessBatchEntry(batchFunctions, modelPackage);
        }
        return getType(errorBatchEntry, modelPackage);
    }

    public static String getBatchKeyMethod(Map.Entry<String, BatchManager> batchFunctions) {
        return batchFunctions.getValue().getBatchKey();
    }

    public static String getSuccessEntriesMethod(Map.Entry<String, BatchManager> batchFunctions) {
        return batchFunctions.getValue().getSuccessEntriesMethod();
    }

    public static String getErrorEntriesMethod(Map.Entry<String, BatchManager> batchFunctions) {
        String errorEntriesMethod = batchFunctions.getValue().getErrorEntriesMethod();
        if (errorEntriesMethod == null) {
            return getSuccessEntriesMethod(batchFunctions);
        }
        return errorEntriesMethod;
    }

    public static String getErrorCodeMethod(Map.Entry<String, BatchManager> batchFunctions) {
        return batchFunctions.getValue().getErrorCodeMethod();
    }

    public static String getRequestIdentifier(Map.Entry<String, BatchManager> batchFunctions) {
        return batchFunctions.getValue().getBatchRequestIdentifier();
    }

    public static ClassName getType(String type, String modelPackage) {
        return ClassName.get(modelPackage, type);
    }

    public static String capitalizeRequestMethodName(String methodName) {
        return methodName.substring(0, 1).toUpperCase(Locale.ROOT) + methodName.substring(1);
    }

}
