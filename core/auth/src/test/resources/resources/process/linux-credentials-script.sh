#!/usr/bin/env bash
echo '{'
echo '"Version": 1,'
echo "\"AccessKeyId\": \"${1:-$ACCESS_KEY_ID}\","
echo "\"SecretAccessKey\": \"${2:-$SECRET_ACCESS_KEY}\""
if [[ $# -ge 3 || -n $SESSION_TOKEN ]]; then
    echo ','
    if [[ -n $SESSION_TOKEN && "$SESSION_TOKEN" = "RANDOM_TOKEN" ]]; then
        echo "\"SessionToken\": \"$RANDOM\""
    else
        echo "\"SessionToken\": \"${3:-$SESSION_TOKEN}\""
    fi
fi
if [[ $# -ge 4 || -n $EXPIRATION ]]; then
    echo ','
    echo "\"Expiration\": \"${4:-$EXPIRATION}\""
fi
echo '}'