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

package software.amazon.awssdk.protocol.asserts.marshalling;

/**
 * Asserts on the request body of the marshalled request. Contains sub assertions for payloads that
 * are known to be JSON or XML which require more sophisticated comparison.
 */
public class RequestBodyAssertion extends CompositeMarshallingAssertion {

    public void setJsonEquals(String jsonEquals) {
        addAssertion(new JsonBodyAssertion(jsonEquals));
    }

    public void setXmlEquals(String xmlEquals) {
        addAssertion(new XmlBodyAssertion(xmlEquals));
    }

    public void setEquals(String equals) {
        addAssertion(new RawBodyAssertion(equals));
    }

    public void setIonEquals(String ionEquals) {
        addAssertion(new IonBodyAssertion(ionEquals));
    }
}
