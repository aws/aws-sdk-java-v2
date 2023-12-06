#!/usr/bin/env bash

parseAdditionalParams () {
    local prefix=`echo $1 | cut -d= -f1`;
    local param=`echo $1 | cut -d= -f2`;

    case "$prefix" in
    ("token"*)
        if [[ "$param" = "RANDOM_TOKEN" ]]; then
            echo "\"SessionToken\": \"$RANDOM\""
        else
            echo "\"SessionToken\": \"$param\""
        fi;
        ;;
    ("exp"*)
        echo "\"Expiration\": \"$param\"";
        ;;
    ("credscope"*)
        echo "\"CredentialScope\": \"$param\"";
        ;;
    (*)
        echo "\"$prefix\": \"$param\"";
        ;;
    esac
}

echo '{';
echo '"Version": 1,';
echo "\"AccessKeyId\": \"$1\",";
echo "\"SecretAccessKey\": \"$2\"";
for args in "${@:3}"
do
    echo ',';
        parseAdditionalParams $args;
    done;
echo '}';