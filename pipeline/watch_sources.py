#!/usr/bin/env python3
"""Watch the official sources and raise a GitHub issue when something needs a human look.

Runs daily in GitHub Actions after build_officials.py.
  1. Cross-check: every current officeholder in officials.json must appear on the
     USCIS "test updates" page. A mismatch opens an issue.
  2. Change watch: the official test pages (USCIS, Home Affairs) are hashed; when
     the text changes, an issue asks the owner to review whether the test changed.
GitHub emails the repository owner about new issues, so nothing needs checking by hand.
Pages that can't be fetched are only reported after 7 failed days in a row.
"""
from __future__ import annotations

import hashlib
import html
import json
import os
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / "pipeline" / "state" / "watch.json"
OFFICIALS = ROOT / "public" / "v1" / "officials.json"
UA = ("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
      "Chrome/130.0 Safari/537.36 OathlyDataBot/1.0")

TEST_UPDATES = "https://www.uscis.gov/citizenship/testupdates"
WATCHED = {
    "uscis-2025-test": "https://www.uscis.gov/citizenship-resource-center/naturalization-test-and-study-resources/2025-civics-test",
    "uscis-study": "https://www.uscis.gov/citizenship/find-study-materials-and-resources/study-for-the-test",
    "uscis-test-updates": TEST_UPDATES,
    "au-test": "https://immi.homeaffairs.gov.au/citizenship/test-and-interview/learn-about-citizenship-interview-and-test/learn-about-citizenship-test",
    "au-booklet": "https://immi.homeaffairs.gov.au/citizenship/test-and-interview/our-common-bond",
    "au-booklet-pdf": "https://immi.homeaffairs.gov.au/citizenship-subsite/files/our-common-bond-testable.pdf",
}
FAIL_DAYS_BEFORE_ALERT = 7


def fetch_text(url: str) -> str:
    req = urllib.request.Request(url, headers={
        "User-Agent": UA,
        "Accept": "text/html,application/xhtml+xml,application/pdf;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-AU,en;q=0.9",
    })
    with urllib.request.urlopen(req, timeout=60) as resp:
        body = resp.read()
        if url.endswith(".pdf") or "pdf" in resp.headers.get("Content-Type", ""):
            # Hash the PDF itself: any edit to the booklet changes it.
            return hashlib.sha256(body).hexdigest()
        raw = body.decode("utf-8", errors="replace")
    raw = re.sub(r"(?is)<(script|style|noscript)[^>]*>.*?</\1>", " ", raw)
    main = re.search(r"(?is)<main[^>]*>(.*?)</main>", raw)
    text = html.unescape(re.sub(r"(?s)<[^>]+>", "\n", main.group(1) if main else raw))
    lines = [" ".join(l.split()) for l in text.splitlines()]
    noise = re.compile(r"(?i)(last (reviewed|updated)|page last|©|copyright)")
    return "\n".join(l for l in lines if l and not noise.search(l))


def open_issue(title: str, body: str) -> None:
    token, repo = os.environ.get("GITHUB_TOKEN"), os.environ.get("GITHUB_REPOSITORY")
    print(f"ISSUE: {title}\n{body}\n")
    if not token or not repo:
        return
    api = f"https://api.github.com/repos/{repo}/issues"
    headers = {"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json",
               "User-Agent": UA}
    req = urllib.request.Request(api + "?state=open&labels=source-watch&per_page=100", headers=headers)
    with urllib.request.urlopen(req, timeout=30) as resp:
        if any(i["title"] == title for i in json.load(resp)):
            return  # already open
    payload = json.dumps({"title": title, "body": body, "labels": ["source-watch"]}).encode()
    req = urllib.request.Request(api, data=payload, headers=headers, method="POST")
    urllib.request.urlopen(req, timeout=30).close()


def main() -> int:
    state = json.loads(STATE.read_text()) if STATE.exists() else {"hashes": {}, "failures": {}}
    officials = json.loads(OFFICIALS.read_text(encoding="utf-8"))

    for key, url in WATCHED.items():
        try:
            text = fetch_text(url)
        except Exception as exc:
            n = state["failures"].get(key, 0) + 1
            state["failures"][key] = n
            print(f"::warning title=Source watch::could not fetch {key} ({exc}); failure {n} in a row")
            if n == FAIL_DAYS_BEFORE_ALERT:
                open_issue(f"Source unreachable for {n} days: {key}",
                           f"{url}\n\nThe daily check could not read this page. Please open it and confirm "
                           "nothing changed in the test. Close this issue when done.")
            continue
        state["failures"][key] = 0
        digest = hashlib.sha256(text.encode()).hexdigest()
        old = state["hashes"].get(key)
        state["hashes"][key] = digest
        if old and old != digest:
            open_issue(f"Official page changed: {key}",
                       f"{url}\n\nThe text of this official page changed. Please check whether the test "
                       "questions, answers or rules changed. If they did, update the content files and "
                       "release an app update. Close this issue when reviewed.")

        if key == "uscis-test-updates":
            fed = officials["federal"]
            missing = [f"{role}: {p['name']}" for role, p in fed.items()
                       if isinstance(p, dict) and p.get("short") and p["short"].lower() not in text.lower()]
            if missing:
                open_issue("Officeholder mismatch with USCIS test updates",
                           "These names from officials.json were not found on " + url + ":\n\n- "
                           + "\n- ".join(missing)
                           + "\n\nIf USCIS lists someone else, the upstream data may be late. Check and fix.")

    STATE.parent.mkdir(parents=True, exist_ok=True)
    STATE.write_text(json.dumps(state, indent=1, sort_keys=True) + "\n")
    return 0


if __name__ == "__main__":
    sys.exit(main())
