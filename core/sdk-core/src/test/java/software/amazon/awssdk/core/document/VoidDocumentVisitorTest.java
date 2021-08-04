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

package software.amazon.awssdk.core.document;

import org.testng.annotations.Test;
import software.amazon.awssdk.core.SdkNumber;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


public class VoidDocumentVisitorTest {
    @Test
    public void voidDocumentVisitor() {
        VoidDocumentVisitCount voidDocumentVisitor = new VoidDocumentVisitCount();
        // Constructing a Document for CustomClass
        final Document build = getCustomDocument();
        build.accept(voidDocumentVisitor);
        assertThat(voidDocumentVisitor.booleanVisitsVisits).isEqualTo(2);
        assertThat(voidDocumentVisitor.mapVisits).isEqualTo(2);
        assertThat(voidDocumentVisitor.nullVisits).isEqualTo(1);
        assertThat(voidDocumentVisitor.listVisits).isEqualTo(1);
        assertThat(voidDocumentVisitor.numberVisits).isEqualTo(4);
        assertThat(voidDocumentVisitor.stringVisits).isEqualTo(2);
        // Expected  CustomClass
    }

    public Document getCustomDocument() {
        final Document build = Document.mapBuilder()
                .putDocument("customClassFromMap",
                        Document.mapBuilder().putString("innerStringField", "innerValue")
                                .putNumber("innerIntField", SdkNumber.fromLong(1)).build())
                .putString("outerStringField", "outerValue")
                .putNull("nullKey")
                .putBoolean("boolOne", true)
                .putBoolean("boolTwo", false)
                .putList("listKey", listBuilder -> listBuilder.addNumber(SdkNumber.fromLong(2)).addNumber(SdkNumber.fromLong(3)))
                .putNumber("outerLongField", SdkNumber.fromDouble(4)).build();
        return build;
    }


    private static class VoidDocumentVisitCount implements VoidDocumentVisitor {

        private int nullVisits = 0;
        private int mapVisits = 0;
        private int listVisits = 0;
        private int stringVisits = 0;
        private int numberVisits = 0;
        private int booleanVisitsVisits = 0;

        public int getNullVisits() {
            return nullVisits;
        }

        public int getMapVisits() {
            return mapVisits;
        }

        public int getListVisits() {
            return listVisits;
        }

        public int getStringVisits() {
            return stringVisits;
        }

        public int getNumberVisits() {
            return numberVisits;
        }

        public int getBooleanVisitsVisits() {
            return booleanVisitsVisits;
        }

        @Override
        public void visitNull() {
            nullVisits++;

        }

        @Override
        public void visitBoolean(Boolean document) {
            booleanVisitsVisits++;
        }

        @Override
        public void visitString(String document) {
            stringVisits++;
        }

        @Override
        public void visitNumber(SdkNumber document) {
            numberVisits++;
        }

        @Override
        public void visitMap(Map<String, Document> documentMap) {
            mapVisits++;
            documentMap.values().stream().forEach(val -> val.accept(this));
        }

        @Override
        public void visitList(List<Document> documentList) {
            listVisits++;
            documentList.forEach(item -> item.accept(this));
        }

    }


    @Test
    public void defaultVoidDocumentVisitor() {
        VoidDocumentVisitor voidDocumentVisitor = new VoidDocumentVisitor() {
        };
        Document.fromNull().accept(voidDocumentVisitor);
        Document.fromNumber(2).accept(voidDocumentVisitor);
        Document.fromString("testString").accept(voidDocumentVisitor);
        Document.fromBoolean(true).accept(voidDocumentVisitor);
        Document.listBuilder().addNumber(4).build().accept(voidDocumentVisitor);
        Document.mapBuilder().putNumber("key", 4).build().accept(voidDocumentVisitor);
    }


}
