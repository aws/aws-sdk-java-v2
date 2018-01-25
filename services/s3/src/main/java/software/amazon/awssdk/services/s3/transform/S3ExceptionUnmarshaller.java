/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.transform;

import static software.amazon.awssdk.core.util.XpathUtils.asString;
import static software.amazon.awssdk.core.util.XpathUtils.xpath;

import javax.xml.xpath.XPath;
import org.w3c.dom.Node;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.core.runtime.transform.AbstractErrorUnmarshaller;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Base exception unmarshaller for S3.
 */
public abstract class S3ExceptionUnmarshaller extends AbstractErrorUnmarshaller<Node> {

    private final String errorCode;

    public S3ExceptionUnmarshaller(Class<? extends SdkServiceException> exceptionClass, String errorCode) {
        super(exceptionClass);
        this.errorCode = errorCode;
    }

    @Override
    public SdkServiceException unmarshall(Node in) throws Exception {

        XPath xpath = xpath();

        String error = asString("Error/Code", in, xpath);
        String requestId = asString("Error/RequestId", in, xpath);
        String message = asString("Error/Message", in, xpath);

        if (errorCode != null && !StringUtils.equals(error, this.errorCode)) {
            return null;
        }

        SdkServiceException exception = newException(message);
        exception.errorCode(errorCode);
        exception.requestId(requestId);

        return exception;
    }
}
