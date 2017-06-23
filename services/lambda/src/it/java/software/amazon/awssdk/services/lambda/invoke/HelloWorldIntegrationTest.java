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

package software.amazon.awssdk.services.lambda.invoke;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.amazon.awssdk.services.lambda.IntegrationTestBase;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.DeleteFunctionRequest;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.LogType;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.test.retry.RetryRule;

public class HelloWorldIntegrationTest extends IntegrationTestBase {

    // This is a bit ugly since it means only one person can be running
    // this test (per account) at a time, but using annotations forces us to
    // know the function name at compile time. :(
    private static final String FUNCTION_NAME = "helloWorld";
    @Rule
    public RetryRule retryRule = new RetryRule(10, 2000, TimeUnit.MILLISECONDS);
    private HelloWorldService invoker;

    @Before
    public void uploadFunction() throws Exception {
        // Upload function
        byte[] functionBits;
        InputStream functionZip = new FileInputStream(cloudFuncZip);
        try {
            functionBits = read(functionZip);
        } finally {
            functionZip.close();
        }

        lambda.createFunction(CreateFunctionRequest.builder()
                .description("My cloud function")
                .functionName(FUNCTION_NAME)
                .code(FunctionCode.builder().zipFile(ByteBuffer.wrap(functionBits)).build())
                .handler("helloworld.handler")
                .memorySize(128)
                .runtime(Runtime.Nodejs43)
                .timeout(10)
                .role(lambdaServiceRoleArn)
                .build());

        invoker = LambdaInvokerFactory.builder()
                                      .lambdaClient(lambda)
                                      .build(HelloWorldService.class);
    }

    @After
    public void deleteFunction() {
        lambda.deleteFunction(DeleteFunctionRequest.builder().functionName(FUNCTION_NAME).build());
    }

    @Test
    public void test_Async() {
        // Just make sure it doesn't throw.
        invoker.helloWorldAsync();
    }

    @Test
    public void test_DryRun() {
        invoker.helloWorldDryRun();
    }

    @Test
    public void test_NoArgs() {
        Assert.assertEquals("Hello World", invoker.helloWorld());
    }

    @Test
    public void test_String() {
        Assert.assertEquals("Hello World", invoker.helloWorld("Testing 123"));
    }

    @Test
    public void test_Complex() {
        ComplexInput input = new ComplexInput();
        input.setString("Testing");
        input.setInteger(123);

        Assert.assertEquals("Hello World", invoker.helloWorld(input));
    }

    @Test(expected = LambdaSerializationException.class)
    public void test_Bogus() {
        invoker.bogus();
    }

    @Test
    public void test_Failure() {
        try {
            invoker.helloWorld("BOOM");
            Assert.fail("Expected LambdaFunctionException");
        } catch (LambdaFunctionException expected) {
            expected.printStackTrace();
        }
    }

    public static interface HelloWorldService {
        @LambdaFunction(functionName = "helloWorld", invocationType = InvocationType.Event)
        void helloWorldAsync();

        @LambdaFunction(functionName = "helloWorld", invocationType = InvocationType.DryRun)
        void helloWorldDryRun();

        @LambdaFunction
        String helloWorld();

        @LambdaFunction
        String helloWorld(String input);

        @LambdaFunction(logType = LogType.Tail)
        String helloWorld(ComplexInput input);

        @LambdaFunction(functionName = "helloWorld")
        ComplexInput bogus();
    }

    public static class ComplexInput {

        private String string;
        private Integer integer;

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }
    }
}
