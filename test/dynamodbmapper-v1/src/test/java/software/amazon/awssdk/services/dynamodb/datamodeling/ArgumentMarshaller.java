/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Interface to make it possible to cache the expensive type determination
 * behavior.
 */
public interface ArgumentMarshaller {

    /**
     * Marshalls the object given into an AttributeValue.
     */
    AttributeValue marshall(Object obj);

    interface BooleanAttributeMarshaller extends ArgumentMarshaller {
    }

    interface StringAttributeMarshaller extends ArgumentMarshaller {
    }

    interface NumberAttributeMarshaller extends ArgumentMarshaller {
    }

    interface BinaryAttributeMarshaller extends ArgumentMarshaller {
    }

    interface StringSetAttributeMarshaller extends ArgumentMarshaller {
    }

    interface NumberSetAttributeMarshaller extends ArgumentMarshaller {
    }

    interface BinarySetAttributeMarshaller extends ArgumentMarshaller {
    }

    interface ListAttributeMarshaller extends ArgumentMarshaller {
    }

    interface MapAttributeMarshaller extends ArgumentMarshaller {
    }
}
