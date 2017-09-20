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

package software.amazon.awssdk.services.dynamodb.datamodeling;

/**
 * Generic exception for problems occuring when mapping DynamoDB items to Java
 * objects or vice versa. Excludes service exceptions.
 */
public class DynamoDbMappingException extends RuntimeException {

    private static final long serialVersionUID = -4883173289978517967L;

    public DynamoDbMappingException() {
        super();
    }

    public DynamoDbMappingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DynamoDbMappingException(String message) {
        super(message);
    }

    public DynamoDbMappingException(Throwable cause) {
        super(cause);
    }

}
