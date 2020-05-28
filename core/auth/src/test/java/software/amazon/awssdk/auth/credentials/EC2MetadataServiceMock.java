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

package software.amazon.awssdk.auth.credentials;

import static software.amazon.awssdk.core.SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.auth.signer.internal.SignerTestUtils;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Mock server for imitating the Amazon EC2 Instance Metadata Service. Tests can
 * use this class to start up a server on a localhost port, and control what
 * response the server will send when a connection is made.
 */

//TODO: this should really be replaced by WireMock
public class EC2MetadataServiceMock {

    private static final String OUTPUT_HEADERS = "HTTP/1.1 200 OK\r\n" +
                                                 "Content-Type: text/html\r\n" +
                                                 "Content-Length: ";
    private static final String OUTPUT_END_OF_HEADERS = "\r\n\r\n";
    private final String securityCredentialsResource;
    private EC2MockMetadataServiceListenerThread hosmMockServerThread;

    public EC2MetadataServiceMock(String securityCredentialsResource) {
        this.securityCredentialsResource = securityCredentialsResource;
    }

    /**
     * Sets the name of the file that should be sent back as the response from
     * this mock server. The files are loaded from the software/amazon/awssdk/auth
     * directory of the tst folder, and no file extension should be specified.
     *
     * @param responseFileName The name of the file that should be sent back as the response
     * from this mock server.
     */
    public void setResponseFileName(String responseFileName) {
        hosmMockServerThread.setResponseFileName(responseFileName);
    }

    /**
     * Accepts a newline delimited list of security credential names that the
     * mock metadata service should advertise as available.
     *
     * @param securityCredentialNames A newline delimited list of security credentials that the
     * metadata service will advertise as available.
     */
    public void setAvailableSecurityCredentials(String securityCredentialNames) {
        hosmMockServerThread.setAvailableSecurityCredentials(securityCredentialNames);
    }

    public void start() throws IOException {
        hosmMockServerThread = new EC2MockMetadataServiceListenerThread(startServerSocket(), securityCredentialsResource);
        hosmMockServerThread.start();
    }

    public void stop() {
        hosmMockServerThread.stopServer();
    }

    private ServerSocket startServerSocket() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);

            System.setProperty(AWS_EC2_METADATA_SERVICE_ENDPOINT.property(),
                               "http://localhost:" + serverSocket.getLocalPort());
            System.out.println("Started mock metadata service at: " +
                               System.getProperty(AWS_EC2_METADATA_SERVICE_ENDPOINT.property()));

            return serverSocket;
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to start mock EC2 metadata server", ioe);
        }
    }

    /**
     * Thread subclass that listens for connections on an opened server socket
     * and response with a predefined response file.
     */
    private static class EC2MockMetadataServiceListenerThread extends Thread {
        private static final String TOKEN_RESOURCE_PATH = "/latest/api/token";
        private ServerSocket serverSocket;
        private final String credentialsResource;
        private String responseFileName;
        private String securityCredentialNames;

        public EC2MockMetadataServiceListenerThread(ServerSocket serverSocket, String credentialsResource) {
            this.serverSocket = serverSocket;
            this.credentialsResource = credentialsResource;
        }

        public void setResponseFileName(String responseFileName) {
            this.responseFileName = responseFileName;
        }

        public void setAvailableSecurityCredentials(String securityCredentialNames) {
            this.securityCredentialNames = securityCredentialNames;
        }

        @Override
        public void run() {
            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    // Just exit if the socket gets shut down while we're waiting
                    return;
                }

                try (OutputStream outputStream = socket.getOutputStream()) {
                    InputStream inputStream = socket.getInputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String requestLine = reader.readLine();

                    String[] strings = requestLine.split(" ");
                    String resourcePath = strings[1];


                    String httpResponse = null;

                    if (resourcePath.equals(credentialsResource)) {
                        httpResponse = formHttpResponse(securityCredentialNames);
                        outputStream.write(httpResponse.getBytes());

                    } else if (resourcePath.startsWith(credentialsResource)) {
                        String responseFilePath = "/software/amazon/awssdk/core/auth/" + responseFileName + ".json";
                        System.out.println("Serving: " + responseFilePath);

                        List<String> dataFromFile;
                        StringBuilder credentialsString;
                        try (InputStream responseFileInputStream = this.getClass().getResourceAsStream(responseFilePath)) {
                            dataFromFile = IOUtils.readLines(responseFileInputStream);
                        }

                        credentialsString = new StringBuilder();

                        for (String line : dataFromFile) {
                            credentialsString.append(line);
                        }

                        httpResponse = formHttpResponse(credentialsString
                                                            .toString());
                        outputStream.write(httpResponse.getBytes());

                    } else if (TOKEN_RESOURCE_PATH.equals(resourcePath)) {
                        httpResponse = "HTTP/1.1 404 Not Found\r\n" +
                                       "Content-Length: 0\r\n" +
                                       "\r\n";
                        outputStream.write(httpResponse.getBytes());
                    }
                    else {
                        throw new RuntimeException("Unknown resource requested: " + resourcePath);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to respond to request", e);
                } finally {
                    try {
                        socket.close();
                    } catch (Exception e) {
                        // Ignored or expected.
                    }
                }
            }
        }

        private String formHttpResponse(String content) {
            return OUTPUT_HEADERS + content.length()
                   + OUTPUT_END_OF_HEADERS
                   + content;

        }

        public void stopServer() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to stop server", e);
                }
            }
        }
    }
}
