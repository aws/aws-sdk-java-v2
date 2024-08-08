#  Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
#  Licensed under the Apache License, Version 2.0 (the "License").
#  You may not use this file except in compliance with the License.
#  A copy of the License is located at
#
#   http://aws.amazon.com/apache2.0
#
#  or in the "license" file accompanying this file. This file is distributed
#  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
#  express or implied. See the License for the specific language governing
#  permissions and limitations under the License.

import csv
import os
import re
import json

PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(os.path.join(__file__, "../../../../"))))
RESOURCES_ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(os.path.join(__file__, "../resources"))))
RECIPE_ROOT_DIR = os.path.join(RESOURCES_ROOT_DIR, 'META-INF/rewrite')


def find_sdk_version():
    pom = open(os.path.join(PROJECT_DIR, "pom.xml"), 'r')
    reg = re.compile("<version>.+")
    version = re.search(reg, pom.read())
    versionStr = version.group(0)
    return versionStr[9:-10]


def load_module_mappings(filename):
    mappings = {}
    with open(filename, mode='r') as file:
        reader = csv.reader(file)
        for row in reader:
            if row:
                mappings[row[0]] = row[2]
    return mappings


def load_model_file(service):
    if service == 'dynamodb':
        service_model_path = os.path.join(PROJECT_DIR, 'services/dynamodb/src/main/resources/codegen-resources/dynamodb/service-2.json')
    else:
        service_model_path = os.path.join(PROJECT_DIR, 'services', service, 'src/main/resources/codegen-resources/service-2.json')

    with open(service_model_path, 'r') as f:
        model_data = json.load(f)
    return model_data


def write_copy_right_header(f):
    f.write('''#
# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#  http://aws.amazon.com/apache2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
''')
