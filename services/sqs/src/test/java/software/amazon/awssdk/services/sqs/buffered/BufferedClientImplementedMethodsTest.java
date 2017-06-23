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

package software.amazon.awssdk.services.sqs.buffered;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.SQSAsyncClient;

public class BufferedClientImplementedMethodsTest {

    @Test
    public void allMethodsInAsyncClientInterfaceOverridenInBufferedClient() {
        Stream.of(SQSAsyncClient.class.getDeclaredMethods())
              .filter(method -> !Modifier.isStatic(method.getModifiers()))
              .forEach(m -> {
                  try {
                      SqsBufferedAsyncClient.class.getDeclaredMethod(m.getName(), (Class<?>[]) m.getParameterTypes());
                  } catch (NoSuchMethodException e) {
                      throw new AssertionError(m.getName() + " is not implemented by " + SqsBufferedAsyncClient.class);
                  }
              });
    }
}
