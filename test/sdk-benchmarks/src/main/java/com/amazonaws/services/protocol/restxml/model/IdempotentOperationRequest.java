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
 * @see <a href="http://docs.aws.amazon.com/goto/WebAPI/restxml-2016-03-11/IdempotentOperation" target="_top">AWS API
 *      Documentation</a>
 */

public class IdempotentOperationRequest extends AmazonWebServiceRequest implements Serializable, Cloneable {

    private String pathIdempotentToken;

    private String queryIdempotentToken;

    private String headerIdempotentToken;

    /**
     * @param pathIdempotentToken
     */

    public void setPathIdempotentToken(String pathIdempotentToken) {
        this.pathIdempotentToken = pathIdempotentToken;
    }

    /**
     * @return
     */

    public String getPathIdempotentToken() {
        return this.pathIdempotentToken;
    }

    /**
     * @param pathIdempotentToken
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public IdempotentOperationRequest withPathIdempotentToken(String pathIdempotentToken) {
        setPathIdempotentToken(pathIdempotentToken);
        return this;
    }

    /**
     * @param queryIdempotentToken
     */

    public void setQueryIdempotentToken(String queryIdempotentToken) {
        this.queryIdempotentToken = queryIdempotentToken;
    }

    /**
     * @return
     */

    public String getQueryIdempotentToken() {
        return this.queryIdempotentToken;
    }

    /**
     * @param queryIdempotentToken
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public IdempotentOperationRequest withQueryIdempotentToken(String queryIdempotentToken) {
        setQueryIdempotentToken(queryIdempotentToken);
        return this;
    }

    /**
     * @param headerIdempotentToken
     */

    public void setHeaderIdempotentToken(String headerIdempotentToken) {
        this.headerIdempotentToken = headerIdempotentToken;
    }

    /**
     * @return
     */

    public String getHeaderIdempotentToken() {
        return this.headerIdempotentToken;
    }

    /**
     * @param headerIdempotentToken
     * @return Returns a reference to this object so that method calls can be chained together.
     */

    public IdempotentOperationRequest withHeaderIdempotentToken(String headerIdempotentToken) {
        setHeaderIdempotentToken(headerIdempotentToken);
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
        if (getPathIdempotentToken() != null)
            sb.append("PathIdempotentToken: ").append(getPathIdempotentToken()).append(",");
        if (getQueryIdempotentToken() != null)
            sb.append("QueryIdempotentToken: ").append(getQueryIdempotentToken()).append(",");
        if (getHeaderIdempotentToken() != null)
            sb.append("HeaderIdempotentToken: ").append(getHeaderIdempotentToken());
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;

        if (obj instanceof IdempotentOperationRequest == false)
            return false;
        IdempotentOperationRequest other = (IdempotentOperationRequest) obj;
        if (other.getPathIdempotentToken() == null ^ this.getPathIdempotentToken() == null)
            return false;
        if (other.getPathIdempotentToken() != null && other.getPathIdempotentToken().equals(this.getPathIdempotentToken()) == false)
            return false;
        if (other.getQueryIdempotentToken() == null ^ this.getQueryIdempotentToken() == null)
            return false;
        if (other.getQueryIdempotentToken() != null && other.getQueryIdempotentToken().equals(this.getQueryIdempotentToken()) == false)
            return false;
        if (other.getHeaderIdempotentToken() == null ^ this.getHeaderIdempotentToken() == null)
            return false;
        if (other.getHeaderIdempotentToken() != null && other.getHeaderIdempotentToken().equals(this.getHeaderIdempotentToken()) == false)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + ((getPathIdempotentToken() == null) ? 0 : getPathIdempotentToken().hashCode());
        hashCode = prime * hashCode + ((getQueryIdempotentToken() == null) ? 0 : getQueryIdempotentToken().hashCode());
        hashCode = prime * hashCode + ((getHeaderIdempotentToken() == null) ? 0 : getHeaderIdempotentToken().hashCode());
        return hashCode;
    }

    @Override
    public IdempotentOperationRequest clone() {
        return (IdempotentOperationRequest) super.clone();
    }

}
