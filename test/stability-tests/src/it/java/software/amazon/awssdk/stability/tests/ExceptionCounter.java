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

package software.amazon.awssdk.stability.tests;

import java.util.concurrent.atomic.AtomicInteger;


public class ExceptionCounter {

    private final AtomicInteger serviceExceptionCount = new AtomicInteger(0);
    private final AtomicInteger ioExceptionCount = new AtomicInteger(0);
    private final AtomicInteger clientExceptionCount = new AtomicInteger(0);
    private final AtomicInteger unknownExceptionCount = new AtomicInteger(0);

    public void addServiceException() {
        serviceExceptionCount.getAndAdd(1);
    }

    public void addClientException() {
        clientExceptionCount.getAndAdd(1);
    }

    public void addUnknownException() {
        unknownExceptionCount.getAndAdd(1);
    }

    public void addIoException() {
        ioExceptionCount.getAndAdd(1);
    }

    public int serviceExceptionCount() {
        return serviceExceptionCount.get();
    }

    public int ioExceptionCount() {
        return ioExceptionCount.get();
    }

    public int clientExceptionCount() {
        return clientExceptionCount.get();
    }

    public int unknownExceptionCount() {
        return unknownExceptionCount.get();
    }
}
