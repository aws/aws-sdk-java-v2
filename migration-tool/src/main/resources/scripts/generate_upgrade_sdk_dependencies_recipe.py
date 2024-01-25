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

import os
import re
from scripts.utils import find_sdk_version

MAPPING_FILE_NAME = 'upgrade-sdk-dependencies.yml'
RESOURCES_ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(os.path.join(__file__, "../resources"))))
RECIPE_ROOT_DIR =os.path.join(
    RESOURCES_ROOT_DIR,
    'META-INF/rewrite'
)
SERVICE_DIR = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(os.path.join(__file__, "../../../../")))),
    'services'
)

def load_all_service_modules():
    service_mapping = {}
    for s in [s for s in os.listdir(SERVICE_DIR) if os.path.isdir(os.path.join(SERVICE_DIR, s))]:
         service_mapping[s] = find_v1_equivalent(s)
    return service_mapping

def find_v1_equivalent(s):
    return "aws-java-sdk-" + s


def write_bom_recipe(f, version):
    change_bom = '''
  - org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId:
      oldGroupId: com.amazonaws
      oldArtifactId: aws-java-sdk-bom
      newGroupId: software.amazon.awssdk
      newArtifactId: bom
      newVersion: {0}'''
    f.write(change_bom.format(version))


def write_recipe_yml_file(service_mapping):
    filename = os.path.join(RECIPE_ROOT_DIR, MAPPING_FILE_NAME)
    version = find_sdk_version()
    with open(filename, 'w') as f:
        write_copy_right_header(f)
        write_recipe_metadata(f, version)
        write_bom_recipe(f, version)
        for s in service_mapping:
            write_recipe(f, s, service_mapping, version)
    return filename

def write_recipe_metadata(f, version):
    f.write('''---
type: specs.openrewrite.org/v1beta/recipe
name: software.amazon.awssdk.UpgradeSdkDependencies
displayName: Change Maven dependency groupId, artifactId and/or the version example
recipeList:
''')

def write_recipe(f, s, service_mapping, version):
    change_dependency_group_id_and_artifact_id = '''
  - org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId:
      oldGroupId: com.amazonaws
      oldArtifactId: {0}
      newGroupId: software.amazon.awssdk
      newArtifactId: {1}
      newVersion: {2}'''
    f.write(change_dependency_group_id_and_artifact_id.format(service_mapping[s], s, version))


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

def generate_upgrade_sdk_dependencies_recipe():
    service_mapping = load_all_service_modules()
    write_recipe_yml_file(service_mapping)
