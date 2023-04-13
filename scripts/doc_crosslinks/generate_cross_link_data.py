import os
import argparse
import io
import codecs
import json
import re
import pathlib
from pathlib import Path
from os import listdir
from os.path import isdir, exists, join
from re import split

sdks = {}
clientClassPrefix = {}

# Eventstreaming operations are only available on the async clients
asyncOnlyOperations = {}

def generateDocsMap(apiDefinitionsPath, apiDefinitionsRelativeFilePath):

    rootPath = pathlib.Path(r'./services')
    for serviceModelPaths in rootPath.rglob('service-2.json'):
        tokenizePath = str(Path(serviceModelPaths).parent).split("/")
        getServiceName = tokenizePath[len(tokenizePath)-1]
        if (getServiceName == "codegen-resources"):
            getServiceName = str(serviceModelPaths).split("services/")[1].split("/src/main/resources")[0]
        with codecs.open(serviceModelPaths, 'rb', 'utf-8') as apiDefinition:
            apiContent = json.loads(apiDefinition.read())
            if "uid" in apiContent["metadata"].keys():
                sdks[apiContent["metadata"]["uid"]] = getServiceName
            clientClassPrefix[apiContent["metadata"]["uid"]] = getClientClassNameFromMetadata(apiContent["metadata"])
            asyncOnlyOps = generateAsyncOnlyOps(apiContent)
            if len(asyncOnlyOps.keys()) > 0:
                asyncOnlyOperations[apiContent["metadata"]["uid"]] = asyncOnlyOps

    return sdks

def generateAsyncOnlyOps(apiContent):
    asyncOnlyOps = {}
    for op in apiContent['operations'].keys():
        eventStreamInOut = hasEventStreamInputOutput(apiContent, op)
        if len(eventStreamInOut.keys()) > 0:
            asyncOnlyOps[op] = eventStreamInOut
    return asyncOnlyOps

def hasEventStreamInputOutput(serviceModel, operationName):
    inOut = {}
    opModel = serviceModel['operations'][operationName]
    if 'input' in opModel.keys():
        inputShapeName = opModel['input']['shape']
        if hasEventStreamMember(serviceModel, inputShapeName):
            inOut['input'] = True
    if 'output' in opModel.keys():
        outputShapeName = opModel['output']['shape']
        if hasEventStreamMember(serviceModel, outputShapeName):
            inOut['output'] = True
    return inOut


def hasEventStreamMember(serviceModel, shapeName):
    shapeModel = serviceModel['shapes'][shapeName]
    if 'members' in shapeModel.keys():
        for name,memberModel in shapeModel['members'].items():
            if isEventStream(serviceModel,memberModel['shape']):
                return True
    return False

def isEventStream(serviceModel, shapeName):
    shapeModel = serviceModel['shapes'][shapeName]
    return 'eventstream' in shapeModel and shapeModel['eventstream']


def splitOnWordBoundaries(toSplit) :
    result = toSplit
    result = re.sub(r'[^A-Za-z0-9]+', " ", result)
    result = re.sub(r'([^a-z]{2,})v([0-9]+)', r'\g<1> v\g<2>' , result)
    result = re.sub(r'([^A-Z]{2,})V([0-9]+)', r'\g<1> V\g<2>', result)
    result = re.sub(r'(?<=[a-z])(?=[A-Z]([a-zA-Z]|[0-9]))', r' ', result)
    result = re.sub(r'([A-Z]+)([A-Z][a-z])', r'\g<1> \g<2>', result)
    result = re.sub(r'([0-9])([a-zA-Z])', r'\g<1> \g<2>', result)
    result = re.sub(r' +', ' ', result)
    return result.split(" ");


def capitalize(str):
    if(str is None or len(str) == 0):
        return str
    strFirstCaps = str[0].title() + str[1:]
    return strFirstCaps


def lowerCase(str):
    if(str is None or len(str) == 0):
        return str
    return str.lower()

def pascalCase(str):
    splits = splitOnWordBoundaries(str)
    modifiedStr = ""
    for i in range(0, len(splits)) :
        modifiedStr += capitalize(lowerCase(splits[i]))
    return modifiedStr

def getClientClassNameFromMetadata(metadataNode):
    toSanitize = ""
    if "serviceId" in metadataNode.keys():
        toSanitize = metadataNode["serviceId"]
    clientName = pascalCase(toSanitize)
    clientName =  removeLeading(clientName, "Amazon")
    clientName =  removeLeading(clientName, "Aws")
    clientName = removeTrailing(clientName, "Service" )
    return clientName

def removeLeading(str, toRemove) :
    if(str is None) :
        return str
    if(str.startswith(toRemove)):
        return str.replace(toRemove, "")
    return str

def removeTrailing(str, toRemove) :
    if(str is None) :
        return str
    if(str.endswith(toRemove)):
        return str.replace(toRemove, "")
    return str

def insertDocsMapToRedirect(apiDefinitionsBasePath, apiDefinitionsRelativeFilePath, templateFilePath, outputFilePath):
    generateDocsMap(apiDefinitionsBasePath, apiDefinitionsRelativeFilePath)
    output = ""
    with codecs.open(templateFilePath, 'rb', 'utf-8') as redirect_template:
        current_template = redirect_template.read();
        output = current_template.replace("${UID_SERVICE_MAPPING}", json.dumps(sdks, ensure_ascii=False))
        output = output.replace("${UID_CLIENT_CLASS_MAPPING}", json.dumps(clientClassPrefix, ensure_ascii=False))
        output = output.replace("${SERVICE_NAME_TO_ASYNC_ONLY_OPERATION_MAPPING}", json.dumps(asyncOnlyOperations, ensure_ascii=False))
    with open(outputFilePath, 'w') as redirect_output:
        redirect_output.write(output)

def Main():
    parser = argparse.ArgumentParser(description="Generates a Cross-link redirect file.")
    parser.add_argument("--apiDefinitionsBasePath", action="store")
    parser.add_argument("--apiDefinitionsRelativeFilePath", action="store")
    parser.add_argument("--templateFilePath", action="store")
    parser.add_argument("--outputFilePath", action="store")
    
    args = vars( parser.parse_args() )
    argMap = {}
    argMap[ "apiDefinitionsBasePath" ] = args[ "apiDefinitionsBasePath" ] or "./../services/"
    argMap[ "apiDefinitionsRelativeFilePath" ] = args[ "apiDefinitionsRelativeFilePath" ] or "/src/main/resources/codegen-resources/service-2.json"
    argMap[ "templateFilePath" ] = args[ "templateFilePath" ] or "./scripts/doc_crosslinks/crosslink_redirect.html"
    argMap[ "outputFilePath" ] = args[ "outputFilePath" ] or "./crosslink_redirect.html"
    
    insertDocsMapToRedirect(argMap["apiDefinitionsBasePath"], argMap["apiDefinitionsRelativeFilePath"], argMap["templateFilePath"], argMap["outputFilePath"])
    print("Generated Cross link at " + argMap["outputFilePath"])
    
Main()
