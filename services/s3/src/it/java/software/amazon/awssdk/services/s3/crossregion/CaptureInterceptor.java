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

package software.amazon.awssdk.services.s3.crossregion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpMethod;

public final class CaptureInterceptor implements ExecutionInterceptor {
    private final List<SdkHttpMethod> httpMethods = new ArrayList<>();
    private List<String> hosts = new ArrayList<>();
    private AtomicInteger serviceCalls = new AtomicInteger(0);

    @Override
    public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
        hosts.add(context.httpRequest().host());
        httpMethods.add(context.httpRequest().method());
        serviceCalls.incrementAndGet();
    }

    public int getServiceCalls() {
        return serviceCalls.get();
    }

    public List<String> hosts() {
        return Collections.unmodifiableList(this.hosts);
    }

    public List<SdkHttpMethod> httpMethods() {
        return Collections.unmodifiableList(this.httpMethods);
    }

    public void reset() {
        this.hosts = new ArrayList<>();
    }
}
