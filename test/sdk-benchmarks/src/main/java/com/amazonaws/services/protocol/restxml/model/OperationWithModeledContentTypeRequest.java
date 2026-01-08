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
package com.amazonaws.services.protocol.restxml.model;

import java.io.Serializable;


import com.amazonaws.AmazonWebServiceRequest;

/**
 * 
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/OperationWithModeledContentType"
 *      target="_top">AWS API Documentation</a>
 */

public class OperationWithModeledContentTypeRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private String contentType;

    /**
     * @param contentType
     */

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return
     */

    public String getContentType() {
        return this.contentType;
    }

    /**
     * @param contentType
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public OperationWithModeledContentTypeRequest withContentType(String contentType) {
        setContentType(contentType);
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
        if (getContentType() != null)
            sb.append("ContentType: ").append(getContentType());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof OperationWithModeledContentTypeRequest == false)
            return false;
        OperationWithModeledContentTypeRequest other = (OperationWithModeledContentTypeRequest) obj;
        if (other.getContentType() == null ^ this.getContentType() == null)
            return false;
        if (other.getContentType() != null && other.getContentType().equals(this.getContentType()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getContentType() == null) ? 0 : getContentType().hashCode());
        return hashCode;
    }

    @Override
    public OperationWithModeledContentTypeRequest clone() {
        return (OperationWithModeledContentTypeRequest) super.clone();
    }

}
