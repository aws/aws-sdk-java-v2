#!/usr/bin/env bash
echo '{';
echo '"Version": 1,';
echo "\"AccessKeyId\": \"$1\",";
echo "\"SecretAccessKey\": \"$2\"";
if [[ $# -ge 3 ]]; then
    echo ',';
    if [[ "$3" = "RANDOM_TOKEN" ]]; then
        echo "\"SessionToken\": \"$RANDOM\""
    else
        echo "\"SessionToken\": \"$3\""
    fi;
fi;
if [[ $# -ge 4 ]]; then
    echo ','
    echo "\"Expiration\": \"$4\"";
fi;
echo '}';