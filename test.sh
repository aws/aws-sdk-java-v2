#!/usr/bin/env bash
if git diff --name-only master HEAD | grep -q "core/"
then
echo "changes in core"
else
echo "no changes in core"
fi
