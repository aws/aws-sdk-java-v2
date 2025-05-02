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

package software.amazon.awssdk.services.s3.regression;

import java.util.concurrent.Callable;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class TestCallable<ResponseT> {
    private SdkAutoCloseable client;
    private Callable<ResponseT> runnable;

    TestCallable(SdkAutoCloseable client, Callable<ResponseT> runnable) {
        this.client = client;
        this.runnable = runnable;
    }

    public SdkAutoCloseable client() {
        return client;
    }

    public Callable<ResponseT> runnable() {
        return runnable;
    }
}
