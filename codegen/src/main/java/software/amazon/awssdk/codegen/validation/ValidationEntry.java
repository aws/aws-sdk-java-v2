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

package software.amazon.awssdk.codegen.validation;

import software.amazon.awssdk.utils.ToString;

public final class ValidationEntry {
    private ValidationErrorId errorId;
    private ValidationErrorSeverity severity;
    private String detailMessage;

    public ValidationErrorId getErrorId() {
        return errorId;
    }

    public void setErrorId(ValidationErrorId errorId) {
        this.errorId = errorId;
    }

    public ValidationEntry withErrorId(ValidationErrorId errorId) {
        setErrorId(errorId);
        return this;
    }

    public ValidationErrorSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(ValidationErrorSeverity severity) {
        this.severity = severity;
    }

    public ValidationEntry withSeverity(ValidationErrorSeverity severity) {
        setSeverity(severity);
        return this;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public ValidationEntry withDetailMessage(String detailMessage) {
        setDetailMessage(detailMessage);
        return this;
    }

    @Override
    public String toString() {
        return ToString.builder("ValidationEntry")
            .add("errorId", errorId)
            .add("severity", severity)
            .add("detailMessage", detailMessage)
            .build();
    }
}
