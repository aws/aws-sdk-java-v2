<#macro content memberModel >
    private static class ${memberModel.name}MapEntryUnmarshaller
           implements Unmarshaller<Map.Entry<${memberModel.mapModel.keyModel.variable.variableType}, ${memberModel.mapModel.valueModel.variable.variableType}>, StaxUnmarshallerContext> {

        @Override
        public Entry<${memberModel.mapModel.keyModel.variable.variableType}, ${memberModel.mapModel.valueModel.variable.variableType}> unmarshall(StaxUnmarshallerContext context) throws Exception {
            int originalDepth = context.getCurrentDepth();
            int targetDepth = originalDepth + 1;

            MapEntry<${memberModel.mapModel.keyModel.variable.variableType}, ${memberModel.mapModel.valueModel.variable.variableType}> entry
                = new MapEntry<${memberModel.mapModel.keyModel.variable.variableType}, ${memberModel.mapModel.valueModel.variable.variableType}>();

            while (true) {
                XMLEvent xmlEvent = context.nextEvent();
                if (xmlEvent.isEndDocument()) return entry;

                if (xmlEvent.isAttribute() || xmlEvent.isStartElement()) {
                    if (context.testExpression("${memberModel.mapModel.keyLocationName}", targetDepth)) {
                        entry.setKey(${memberModel.mapModel.keyModel.variable.variableType}Unmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                    if (context.testExpression("${memberModel.mapModel.valueLocationName}", targetDepth)) {
                        entry.setValue(${memberModel.mapModel.valueModel.variable.simpleType}Unmarshaller.getInstance().unmarshall(context));
                        continue;
                    }
                } else if (xmlEvent.isEndElement()) {
                    if (context.getCurrentDepth() < originalDepth) return entry;
                }
            }
        }

        private static final ${memberModel.name}MapEntryUnmarshaller INSTANCE = new ${memberModel.name}MapEntryUnmarshaller();
        public static ${memberModel.name}MapEntryUnmarshaller getInstance() {
            return INSTANCE;
        }

    }
</#macro>
