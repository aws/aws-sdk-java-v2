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

package software.amazon.awssdk.services.s3.handlers;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.io.BufferedInputStream;
import java.util.Arrays;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.interceptor.Context;
import software.amazon.awssdk.interceptor.ExecutionAttributes;
import software.amazon.awssdk.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

public class ErrorOn200Interceptor implements ExecutionInterceptor {

    private static final String ERROR_TAG = "Error";

    private static final List<Class> OPERATIONS_WITH_200_ERROR = Arrays.asList(CopyObjectRequest.class);

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    public SdkHttpFullResponse modifyHttpResponse(Context.ModifyHttpResponse context,
                                                  ExecutionAttributes executionAttributes) {

        SdkHttpFullResponse response = context.httpResponse();

        if (OPERATIONS_WITH_200_ERROR.contains(context.request().getClass())) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(response.getContent());
            bufferedInputStream.mark(10_000);

            XMLEventReader eventReader;

            synchronized (XML_INPUT_FACTORY) {
                eventReader = invokeSafely(() -> XML_INPUT_FACTORY.createXMLEventReader(bufferedInputStream));
            }

            XMLEvent event = invokeSafely(() -> eventReader.nextTag());

            if (event instanceof StartElement) {
                StartElement element = (StartElement) event;

                if (element.getName().getLocalPart().equals(ERROR_TAG)) {
                    invokeSafely(() -> bufferedInputStream.reset());
                    return response.toBuilder().content(bufferedInputStream).statusCode(500).build();
                }
            }

            invokeSafely(() -> bufferedInputStream.reset());
            response = response.toBuilder().content(bufferedInputStream).build();
        }

        return response;
    }
}
