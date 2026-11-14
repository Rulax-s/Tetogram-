#!/usr/bin/env python3
"""Publish actual JUnit counts/names, without captured stdout/stderr or test data."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path('TMessagesProj/build/test-results/testDebugUnitTest')
files = sorted(root.glob('TEST-*.xml'))
if not files:
    print('JUnit results unavailable: inspect the compilation/test task log.')
    raise SystemExit(0)
suites = []
for path in files:
    suite = ET.parse(path).getroot()
    suites.append({
        'name': suite.get('name'),
        **{key: int(suite.get(key, 0)) for key in ('tests', 'failures', 'errors', 'skipped')},
        'cases': [{'name': case.get('name'),
                   'class': case.get('classname'),
                   'skipped': case.find('skipped') is not None,
                   'failed': case.find('failure') is not None or case.find('error') is not None}
                  for case in suite.findall('testcase')],
    })
summary = {key: sum(suite[key] for suite in suites)
           for key in ('tests', 'failures', 'errors', 'skipped')}
output = Path('build-logs/jvm-test-results.json')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps({'summary': summary, 'suites': suites}, indent=2) + '\n')
print('Actual JUnit results:', json.dumps(summary, sort_keys=True))
if not summary['tests'] or summary['failures'] or summary['errors']:
    raise SystemExit(1)
