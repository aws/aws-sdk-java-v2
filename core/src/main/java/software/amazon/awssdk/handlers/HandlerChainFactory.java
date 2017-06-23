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

package software.amazon.awssdk.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.AmazonClientException;
import software.amazon.awssdk.util.ClassLoaderHelper;
import software.amazon.awssdk.util.StringUtils;

/**
 * Factory for creating request/response handler chains.
 */
public final class HandlerChainFactory {

    private static final String GLOBAL_HANDLER_PATH = "software/amazon/awssdk/global/handlers/request.handler2s";

    /**
     * Constructs a new request handler chain by analyzing the specified classpath resource.
     *
     * @param resource The resource to load from the classpath containing the list of request handlers to instantiate.
     * @return A list of request handlers based on the handlers referenced in the specified resource.
     */
    public List<RequestHandler> newRequestHandlerChain(String resource) {
        return createRequestHandlerChain(resource, RequestHandler.class);
    }

    public List<RequestHandler> getGlobalHandlers() {
        List<RequestHandler> handlers = new ArrayList<RequestHandler>();
        BufferedReader fileReader = null;

        try {
            List<URL> globalHandlerListLocations = Collections
                    .list(HandlerChainFactory.class.getClassLoader().getResources(GLOBAL_HANDLER_PATH));

            for (URL url : globalHandlerListLocations) {

                fileReader = new BufferedReader(new InputStreamReader(url.openStream(), StringUtils.UTF8));
                while (true) {
                    String requestHandlerClassName = fileReader.readLine();
                    if (requestHandlerClassName == null) {
                        break;
                    }
                    RequestHandler requestHandler = createRequestHandler(requestHandlerClassName, RequestHandler.class);
                    if (requestHandler == null) {
                        continue;
                    }
                    handlers.add(requestHandler);
                }
            }

        } catch (IOException | IllegalAccessException | InstantiationException | ClassNotFoundException | RuntimeException e) {
            throw new AmazonClientException("Unable to instantiate request handler chain for client: "
                                            + e.getMessage(), e);
        } finally {
            try {
                if (fileReader != null) {
                    fileReader.close();
                }
            } catch (IOException e) {
                // Ignored or expected.
            }
        }
        return handlers;
    }

    private RequestHandler createRequestHandler(String handlerClassName, Class<?> handlerApiClass)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        handlerClassName = handlerClassName.trim();
        if (handlerClassName.equals("")) {
            return null;
        }
        Class<?> requestHandlerClass = ClassLoaderHelper.loadClass(
                handlerClassName,
                handlerApiClass, getClass());
        Object requestHandlerObject = requestHandlerClass.newInstance();
        if (handlerApiClass.isInstance(requestHandlerObject)) {
            if (handlerApiClass == RequestHandler.class) {
                return (RequestHandler) requestHandlerObject;
            } else {
                throw new IllegalStateException();
            }
        } else {
            throw new AmazonClientException(
                    "Unable to instantiate request handler chain for client.  "
                    + "Listed request handler ('"
                    + handlerClassName + "') "
                    + "does not implement the "
                    + handlerApiClass + " API.");
        }
    }

    private List<RequestHandler> createRequestHandlerChain(String resource, Class<?> handlerApiClass) {
        List<RequestHandler> handlers = new ArrayList<RequestHandler>();
        BufferedReader reader = null;

        try {
            InputStream input = getClass().getResourceAsStream(resource);
            if (input == null) {
                return handlers;
            }

            reader = new BufferedReader(new InputStreamReader(input, StringUtils.UTF8));
            while (true) {
                String requestHandlerClassName = reader.readLine();
                if (requestHandlerClassName == null) {
                    break;
                }
                RequestHandler requestHandler = createRequestHandler(requestHandlerClassName, handlerApiClass);
                if (requestHandler == null) {
                    continue;
                }
                handlers.add(requestHandler);
            }
        } catch (IOException | IllegalAccessException | InstantiationException | ClassNotFoundException | RuntimeException e) {
            throw new AmazonClientException("Unable to instantiate request handler chain for client: " + e.getMessage(), e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignored or expected.
            }
        }
        return handlers;
    }
}
