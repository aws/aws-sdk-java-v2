#!/usr/bin/env python
import jmespath
import json
import sys

expression = sys.argv[1]
parsed = jmespath.compile(expression)
print(json.dumps(parsed.parsed, indent=2))
