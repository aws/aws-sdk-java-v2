/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.services.protocol.restjson.model;

import java.io.Serializable;


/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restjson-2016-03-11/JsonValuesOperation" target="_top">AWS API
 *      Documentation</a>
 */

public class JsonValuesOperationResult extends com.amazonaws.AmazonWebServiceResult<com.amazonaws.ResponseMetadata> implements Serializable, Cloneable {

    private String jsonValueHeaderMember;

    private String jsonValueMember;

    /**
     * <p>
     * This field's value must be valid JSON according to RFC 7159, including the opening and closing braces. For
     * example: '{"key": "value"}'.
     * </p>
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * 
     * @param jsonValueHeaderMember
     */

    public void setJsonValueHeaderMember(String jsonValueHeaderMember) {
        this.jsonValueHeaderMember = jsonValueHeaderMember;
    }

    /**
     * <p>
     * This field's value will be valid JSON according to RFC 7159, including the opening and closing braces. For
     * example: '{"key": "value"}'.
     * </p>
     * 
     * @return
     */

    public String getJsonValueHeaderMember() {
        return this.jsonValueHeaderMember;
    }

    /**
     * <p>
     * This field's value must be valid JSON according to RFC 7159, including the opening and closing braces. For
     * example: '{"key": "value"}'.
     * </p>
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * 
     * @param jsonValueHeaderMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public JsonValuesOperationResult withJsonValueHeaderMember(String jsonValueHeaderMember) {
        setJsonValueHeaderMember(jsonValueHeaderMember);
        return this;
    }

    /**
     * <p>
     * This field's value must be valid JSON according to RFC 7159, including the opening and closing braces. For
     * example: '{"key": "value"}'.
     * </p>
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * 
     * @param jsonValueMember
     */

    public void setJsonValueMember(String jsonValueMember) {
        this.jsonValueMember = jsonValueMember;
    }

    /**
     * <p>
     * This field's value will be valid JSON according to RFC 7159, including the opening and closing braces. For
     * example: '{"key": "value"}'.
     * </p>
     * 
     * @return
     */

    public String getJsonValueMember() {
        return this.jsonValueMember;
    }

    /**
     * <p>
     * This field's value must be valid JSON according to RFC 7159, including the opening and closing braces. For
     * example: '{"key": "value"}'.
     * </p>
     * <p>
     * The AWS SDK for Java performs a Base64 encoding on this field before sending this request to the AWS service.
     * Users of the SDK should not perform Base64 encoding on this field.
     * </p>
     * 
     * @param jsonValueMember
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public JsonValuesOperationResult withJsonValueMember(String jsonValueMember) {
        setJsonValueMember(jsonValueMember);
        return this;
    }

    /**
     * Returns a string representation of this object. This is useful for testing and debugging. Sensitive data will be
     * redacted from this string using a placeholder value.
     *
     * @return A string representation of this object.
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        if (getJsonValueHeaderMember() != null)
            sb.append("JsonValueHeaderMember: ").append(getJsonValueHeaderMember()).append(",");
        if (getJsonValueMember() != null)
            sb.append("JsonValueMember: ").append(getJsonValueMember());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof JsonValuesOperationResult == false)
            return false;
        JsonValuesOperationResult other = (JsonValuesOperationResult) obj;
        if (other.getJsonValueHeaderMember() == null ^ this.getJsonValueHeaderMember() == null)
            return false;
        if (other.getJsonValueHeaderMember() != null && other.getJsonValueHeaderMember().equals(this.getJsonValueHeaderMember()) == false)
            return false;
        if (other.getJsonValueMember() == null ^ this.getJsonValueMember() == null)
            return false;
        if (other.getJsonValueMember() != null && other.getJsonValueMember().equals(this.getJsonValueMember()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getJsonValueHeaderMember() == null) ? 0 : getJsonValueHeaderMember().hashCode());
        hashCode = prime * hashCode + ((getJsonValueMember() == null) ? 0 : getJsonValueMember().hashCode());
        return hashCode;
    }

    @Override
    public JsonValuesOperationResult clone() {
        try {
            return (JsonValuesOperationResult) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Got a CloneNotSupportedException from Object.clone() " + "even though we're Cloneable!", e);
        }
    }

}
