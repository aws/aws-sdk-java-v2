#!/bin/bash

#
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
#

s3_dl='s3_download'
s3_ul='s3_upload'
tm_dl='tm_download'
tm_ul='tm_upload'

key='1'
path="/files/upload/50TiB-1x/$key"

test=$s3_dl

mvn test -pl :s3-transfer-manager \
    -Dtest=software.amazon.awssdk.transfer.s3.ExpressTest#$test \
    -Dtestpath=$path \
    -Dtestkey=$key \
    -Xmx4g