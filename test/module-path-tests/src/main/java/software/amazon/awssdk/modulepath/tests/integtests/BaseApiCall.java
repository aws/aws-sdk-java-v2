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

package software.amazon.awssdk.modulepath.tests.integtests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.testutils.service.AwsTestBase;

/**
 * Base Api Call class
 */
public abstract class BaseApiCall extends AwsTestBase {

    private static final Logger logger = LoggerFactory.getLogger(BaseApiCall.class);

    private final String serviceName;

    public BaseApiCall(String serviceName) {
        this.serviceName = serviceName;
    }

    public void usingApacheClient() {
        logger.info("Starting testing {} client with Apache http client", serviceName);
        apacheClientRunnable().run();
    }

    public void usingUrlConnectionClient() {
        logger.info("Starting testing {} client with url connection http client", serviceName);
        urlHttpConnectionClientRunnable().run();
    }

    public void usingNettyClient() {
        logger.info("Starting testing {} client with netty client", serviceName);
        nettyClientRunnable().run();
    }

    public abstract Runnable apacheClientRunnable();

    public abstract Runnable urlHttpConnectionClientRunnable();

    public abstract Runnable nettyClientRunnable();
}
