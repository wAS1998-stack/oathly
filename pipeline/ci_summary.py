#!/usr/bin/env python3
"""Turn unit-test and lint results into GitHub annotations, so they show on the run page."""
from __future__ import annotations

import glob
import xml.etree.ElementTree as ET

tests = failures = 0
for path in glob.glob("app/build/test-results/**/*.xml", recursive=True):
    root = ET.parse(path).getroot()
    tests += int(root.get("tests", 0))
    failures += int(root.get("failures", 0)) + int(root.get("errors", 0))
print(f"::notice title=Unit tests::{tests} tests run, {failures} failed")

issues = []
for path in glob.glob("app/build/reports/lint-results-*.xml"):
    for issue in ET.parse(path).getroot().iter("issue"):
        loc = issue.find("location")
        file = loc.get("file", "") if loc is not None else ""
        line = loc.get("line", "1") if loc is not None else "1"
        rel = file.split("/oathly/")[-1] if "/oathly/" in file else file.split("/work/")[-1]
        issues.append((issue.get("severity", ""), issue.get("id", ""), rel, line, issue.get("message", "")))

by_sev = {}
for sev, *_ in issues:
    by_sev[sev] = by_sev.get(sev, 0) + 1
print(f"::notice title=Lint::{len(issues)} issues {by_sev}")
for sev, iid, rel, line, msg in issues[:45]:
    level = "error" if sev in ("Error", "Fatal") else "warning"
    msg = msg.replace("\n", " ")[:300]
    print(f"::{level} file={rel},line={line},title=Lint {iid}::{msg}")
