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

package software.amazon.awssdk.release;

import java.nio.file.Path;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public abstract class PomTransformer {
    public final void transform(Path file) throws Exception {
        DocumentBuilderFactory docFactory = newSecureDocumentBuilderFactory();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(file.toFile());

        doc.normalize();
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                                                      doc,
                                                      XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }

        updateDocument(doc);

        TransformerFactory transformerFactory = newSecureTransformerFactory();
        transformerFactory.setAttribute("indent-number", 4);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(file.toFile());
        transformer.transform(source, result);
    }

    protected abstract void updateDocument(Document doc);

    protected final Node findChild(Node parent, String childName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (childName.equals(child.getNodeName()) && child.getNodeType() == Node.ELEMENT_NODE) {
                return child;
            }
        }

        throw new IllegalArgumentException(parent + " has no child element named " + childName);
    }

    protected final void addChild(Node parent, Element childToAdd) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node existingChild = children.item(i);
            if (existingChild.isEqualNode(childToAdd)) {
                // Child already exists, skip.
                return;
            }
        }

        parent.appendChild(childToAdd);
    }

    protected final Element textElement(Document doc, String name, String value) {
        Element element = doc.createElement(name);
        element.setTextContent(value);
        return element;
    }

    protected final Element sdkDependencyElement(Document doc, String artifactId) {
        Element newDependency = doc.createElement("dependency");

        newDependency.appendChild(textElement(doc, "groupId", "software.amazon.awssdk"));
        newDependency.appendChild(textElement(doc, "artifactId", artifactId));
        newDependency.appendChild(textElement(doc, "version", "${awsjavasdk.version}"));

        return newDependency;
    }

    private DocumentBuilderFactory newSecureDocumentBuilderFactory() {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setXIncludeAware(false);
        docFactory.setExpandEntityReferences(false);
        trySetFeature(docFactory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(docFactory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(docFactory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(docFactory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetAttribute(docFactory, "http://javax.xml.XMLConstants/property/accessExternalDTD", "");
        trySetAttribute(docFactory, "http://javax.xml.XMLConstants/property/accessExternalSchema", "");
        return docFactory;
    }

    private TransformerFactory newSecureTransformerFactory() {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        trySetAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
        trySetAttribute(transformerFactory, XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return transformerFactory;
    }

    private void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void trySetAttribute(DocumentBuilderFactory factory, String feature, String value) {
        try {
            factory.setAttribute(feature, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void trySetAttribute(TransformerFactory factory, String feature, Object value) {
        try {
            factory.setAttribute(feature, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
