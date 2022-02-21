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

package software.amazon.awssdk.codegen.checksum;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Class to map the HttpChecksum trait of an operation.
 */
@SdkInternalApi
public class HttpChecksum {

    private boolean requestChecksumRequired;

    private String requestAlgorithmMember;

    private String requestValidationModeMember;

    private List<String> responseAlgorithms;

    public boolean isRequestChecksumRequired() {
        return requestChecksumRequired;
    }

    public void setRequestChecksumRequired(boolean requestChecksumRequired) {
        this.requestChecksumRequired = requestChecksumRequired;
    }

    public String getRequestAlgorithmMember() {
        return requestAlgorithmMember;
    }

    public void setRequestAlgorithmMember(String requestAlgorithmMember) {
        this.requestAlgorithmMember = requestAlgorithmMember;
    }

    public String getRequestValidationModeMember() {
        return requestValidationModeMember;
    }

    public void setRequestValidationModeMember(String requestValidationModeMember) {
        this.requestValidationModeMember = requestValidationModeMember;
    }

    public List<String> getResponseAlgorithms() {
        return responseAlgorithms;
    }

    public void setResponseAlgorithms(List<String> responseAlgorithms) {
        this.responseAlgorithms = responseAlgorithms;
    }
}