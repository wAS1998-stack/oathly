#!/usr/bin/env python3
"""Build officials.json: the answers that change with elections and appointments.

Runs daily in GitHub Actions. Sources (all free and open):
  * Members of Congress, President, Vice President, Speaker:
      unitedstates/congress-legislators (public domain / CC0)
  * Governors and the Chief Justice: Wikidata (CC0)
  * State capitals: static table below

Safety rules, so a bad upstream day can never publish wrong answers:
  * every section is validated; anything that fails keeps the last good value
  * if the core federal data is missing the script exits non-zero, the
    workflow fails, and GitHub emails the owner
Writes public/v1/officials.json (served by Cloudflare Pages) and the bundled
copy in app/src/main/assets/officials.json.
"""
from __future__ import annotations

import datetime as dt
import json
import sys
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ROOT / "public" / "v1" / "officials.json"
ASSET = ROOT / "app" / "src" / "main" / "assets" / "officials.json"

UA = "OathlyDataBot/1.0 (study app data refresh; https://oathly.pages.dev)"
CL_BASE = "https://raw.githubusercontent.com/unitedstates/congress-legislators/gh-pages/"
WIKIDATA = "https://query.wikidata.org/sparql"

# code, name, type, capital, extra accepted spellings for the capital
PLACES = [
    ("AL", "Alabama", "state", "Montgomery", []), ("AK", "Alaska", "state", "Juneau", []),
    ("AZ", "Arizona", "state", "Phoenix", []), ("AR", "Arkansas", "state", "Little Rock", []),
    ("CA", "California", "state", "Sacramento", []), ("CO", "Colorado", "state", "Denver", []),
    ("CT", "Connecticut", "state", "Hartford", []), ("DE", "Delaware", "state", "Dover", []),
    ("FL", "Florida", "state", "Tallahassee", []), ("GA", "Georgia", "state", "Atlanta", []),
    ("HI", "Hawaii", "state", "Honolulu", []), ("ID", "Idaho", "state", "Boise", []),
    ("IL", "Illinois", "state", "Springfield", []), ("IN", "Indiana", "state", "Indianapolis", []),
    ("IA", "Iowa", "state", "Des Moines", []), ("KS", "Kansas", "state", "Topeka", []),
    ("KY", "Kentucky", "state", "Frankfort", []), ("LA", "Louisiana", "state", "Baton Rouge", []),
    ("ME", "Maine", "state", "Augusta", []), ("MD", "Maryland", "state", "Annapolis", []),
    ("MA", "Massachusetts", "state", "Boston", []), ("MI", "Michigan", "state", "Lansing", []),
    ("MN", "Minnesota", "state", "Saint Paul", ["St. Paul"]),
    ("MS", "Mississippi", "state", "Jackson", []), ("MO", "Missouri", "state", "Jefferson City", []),
    ("MT", "Montana", "state", "Helena", []), ("NE", "Nebraska", "state", "Lincoln", []),
    ("NV", "Nevada", "state", "Carson City", []), ("NH", "New Hampshire", "state", "Concord", []),
    ("NJ", "New Jersey", "state", "Trenton", []), ("NM", "New Mexico", "state", "Santa Fe", []),
    ("NY", "New York", "state", "Albany", []), ("NC", "North Carolina", "state", "Raleigh", []),
    ("ND", "North Dakota", "state", "Bismarck", []), ("OH", "Ohio", "state", "Columbus", []),
    ("OK", "Oklahoma", "state", "Oklahoma City", []), ("OR", "Oregon", "state", "Salem", []),
    ("PA", "Pennsylvania", "state", "Harrisburg", []), ("RI", "Rhode Island", "state", "Providence", []),
    ("SC", "South Carolina", "state", "Columbia", []), ("SD", "South Dakota", "state", "Pierre", []),
    ("TN", "Tennessee", "state", "Nashville", []), ("TX", "Texas", "state", "Austin", []),
    ("UT", "Utah", "state", "Salt Lake City", []), ("VT", "Vermont", "state", "Montpelier", []),
    ("VA", "Virginia", "state", "Richmond", []), ("WA", "Washington", "state", "Olympia", []),
    ("WV", "West Virginia", "state", "Charleston", []), ("WI", "Wisconsin", "state", "Madison", []),
    ("WY", "Wyoming", "state", "Cheyenne", []),
    ("DC", "District of Columbia", "district", None, []),
    ("PR", "Puerto Rico", "territory", "San Juan", []),
    ("GU", "Guam", "territory", "Hagåtña", ["Hagatna", "Agana"]),
    ("VI", "U.S. Virgin Islands", "territory", "Charlotte Amalie", []),
    ("AS", "American Samoa", "territory", "Pago Pago", []),
    ("MP", "Northern Mariana Islands", "territory", "Saipan", ["Capitol Hill"]),
]
TERRITORY_QIDS = "wd:Q1183 wd:Q16635 wd:Q11703 wd:Q16641 wd:Q16644"

GOV_QUERY = f"""
SELECT ?iso ?govLabel ?start ?family WHERE {{
  {{ ?place wdt:P31 wd:Q35657 . }} UNION {{ VALUES ?place {{ {TERRITORY_QIDS} }} }}
  ?place wdt:P300 ?iso .
  ?place p:P6 ?st . ?st ps:P6 ?gov .
  FILTER NOT EXISTS {{ ?st pq:P582 ?end . }}
  FILTER NOT EXISTS {{ ?st wikibase:rank wikibase:DeprecatedRank . }}
  OPTIONAL {{ ?st pq:P580 ?start . }}
  OPTIONAL {{ ?gov wdt:P734 ?fam . ?fam rdfs:label ?family . FILTER(LANG(?family) = "en") }}
  SERVICE wikibase:label {{ bd:serviceParam wikibase:language "en". }}
}}
"""
CJ_QUERY = """
SELECT ?pLabel ?start WHERE {
  ?p p:P39 ?st . ?st ps:P39 wd:Q11201 .
  FILTER NOT EXISTS { ?st pq:P582 ?end . }
  OPTIONAL { ?st pq:P580 ?start . }
  SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
}
"""


def http_json(url: str, timeout: int = 60):
    req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.load(resp)


def sparql(query: str):
    url = WIKIDATA + "?" + urllib.parse.urlencode({"query": query, "format": "json"})
    return http_json(url, timeout=90)["results"]["bindings"]


def person(name: str, last: str | None = None, **extra) -> dict:
    name = " ".join(name.split())
    short = last or name.replace(", Jr.", "").replace(" Jr.", "").split()[-1]
    out = {"name": name, "short": short}
    out.update({k: v for k, v in extra.items() if v is not None})
    return out


def display_name(n: dict) -> str:
    if n.get("official_full"):
        return n["official_full"]
    first = n.get("nickname") or n.get("first", "")
    return f"{first} {n.get('last', '')}".strip()


def congress() -> tuple[dict, dict]:
    """Returns (federal partial, {code: {senators, representatives, delegate}})."""
    legislators = http_json(CL_BASE + "legislators-current.json")
    executive = http_json(CL_BASE + "executive.json")
    today = dt.date.today().isoformat()

    federal: dict = {}
    for p in executive:
        t = p["terms"][-1]
        if t["start"] <= today <= t["end"]:
            key = "president" if t["type"] == "prez" else "vicePresident"
            extra = {"party": t.get("party")} if key == "president" else {}
            federal[key] = person(display_name(p["name"]), p["name"].get("last"), **extra)

    by_place: dict = {code: {"senators": [], "representatives": [], "delegate": None}
                      for code, *_ in PLACES}
    for m in legislators:
        t = m["terms"][-1]
        code = t["state"]
        if code not in by_place:
            continue
        entry = person(display_name(m["name"]), m["name"].get("last"), party=t.get("party"))
        for r in m.get("leadership_roles", []):
            if r.get("title") == "Speaker of the House" and not r.get("end"):
                federal["speaker"] = person(display_name(m["name"]), m["name"].get("last"))
        if t["type"] == "sen":
            by_place[code]["senators"].append(entry)
        else:
            kind = next(k for c, _, k, *_ in PLACES if c == code)
            if kind == "state":
                entry["district"] = int(t.get("district", 0))
                by_place[code]["representatives"].append(entry)
            else:
                title = "Resident Commissioner" if code == "PR" else "Delegate"
                by_place[code]["delegate"] = dict(entry, title=title)
    for v in by_place.values():
        v["senators"].sort(key=lambda x: x["short"])
        v["representatives"].sort(key=lambda x: (x["district"], x["short"]))
    return federal, by_place


def governors() -> dict:
    rows = sparql(GOV_QUERY)
    today = dt.date.today().isoformat()
    best: dict = {}
    for r in rows:
        code = r["iso"]["value"].replace("US-", "")
        start = r.get("start", {}).get("value", "")[:10]
        if start and start > today:  # governor-elect, not in office yet
            continue
        name = r["govLabel"]["value"]
        if name.startswith("Q") and name[1:].isdigit():  # no English label
            continue
        cand = (start, person(name, r.get("family", {}).get("value")))
        if code not in best or cand[0] > best[code][0]:
            best[code] = cand
    return {code: v[1] for code, v in best.items()}


def chief_justice() -> dict | None:
    rows = sparql(CJ_QUERY)
    rows = [r for r in rows if not r["pLabel"]["value"].startswith("Q")]
    if len(rows) != 1:
        return None
    return person(rows[0]["pLabel"]["value"])


def load_previous() -> dict:
    for path in (PUBLIC, ASSET):
        if path.exists():
            try:
                return json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                pass
    return {}


def main() -> int:
    prev = load_previous()
    prev_fed = prev.get("federal", {})
    prev_places = {p["code"]: p for p in prev.get("places", [])}
    warnings: list[str] = []

    federal, by_place = congress()
    senators = sum(len(v["senators"]) for v in by_place.values())
    reps = sum(len(v["representatives"]) for v in by_place.values())
    if "president" not in federal or "vicePresident" not in federal or senators < 98 or reps < 420:
        print(f"FATAL: congress data incomplete (senators={senators}, reps={reps}, keys={list(federal)})")
        return 1
    if "speaker" not in federal:
        if prev_fed.get("speaker"):
            federal["speaker"] = prev_fed["speaker"]
            warnings.append("Speaker not found upstream; kept previous value")
        else:
            federal["speaker"] = None
            warnings.append("Speaker unknown")

    try:
        govs = governors()
    except Exception as exc:  # network or query failure: keep previous values
        govs = {}
        warnings.append(f"Wikidata governors query failed: {exc}")
    try:
        cj = chief_justice()
    except Exception as exc:
        cj = None
        warnings.append(f"Wikidata chief justice query failed: {exc}")
    federal["chiefJustice"] = cj or prev_fed.get("chiefJustice") or person("John G. Roberts, Jr.", "Roberts")
    federal["justices"] = 9

    places = []
    stale_govs = []
    for code, name, kind, capital, capital_alt in PLACES:
        gov = govs.get(code)
        if gov is None and kind != "district":
            gov = prev_places.get(code, {}).get("governor")
            stale_govs.append(code)
        places.append({
            "code": code, "name": name, "type": kind,
            "capital": capital, "capitalAlt": capital_alt,
            "governor": gov if kind != "district" else None,
            **by_place[code],
        })

    if stale_govs:
        warnings.append(f"governors not refreshed (kept last known value): {', '.join(stale_govs)}")

    data = {
        "schema": 1,
        "generatedAt": dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat(),
        "federal": federal,
        "places": places,
        "sources": [
            {"name": "unitedstates/congress-legislators", "url": "https://github.com/unitedstates/congress-legislators"},
            {"name": "Wikidata", "url": "https://www.wikidata.org"},
            {"name": "USCIS test updates", "url": "https://www.uscis.gov/citizenship/testupdates"},
        ],
        "warnings": warnings,
    }

    # Only rewrite files when the answers changed, so the app's update check stays quiet.
    def answers(d: dict) -> str:
        return json.dumps({"federal": d.get("federal"), "places": d.get("places")}, sort_keys=True)

    changed = not prev or answers(prev) != answers(data)
    for w in warnings:
        print("WARNING:", w)
    if changed:
        for path in (PUBLIC, ASSET):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        print("officials.json updated")
        for line in diff(prev, data):
            print("CHANGE:", line)
    else:
        print("no changes")
    return 0


def diff(old: dict, new: dict) -> list[str]:
    out = []
    of, nf = old.get("federal", {}) or {}, new["federal"]
    for k in ("president", "vicePresident", "speaker", "chiefJustice"):
        a, b = (of.get(k) or {}).get("name"), (nf.get(k) or {}).get("name")
        if a != b:
            out.append(f"{k}: {a} -> {b}")
    op = {p["code"]: p for p in old.get("places", [])}
    for p in new["places"]:
        q = op.get(p["code"], {})
        for k in ("governor",):
            a, b = (q.get(k) or {}).get("name"), (p.get(k) or {}).get("name")
            if a != b:
                out.append(f"{p['code']} {k}: {a} -> {b}")
        for k in ("senators", "representatives"):
            a = sorted(x["name"] for x in q.get(k, []))
            b = sorted(x["name"] for x in p.get(k, []))
            if a != b:
                out.append(f"{p['code']} {k}: {len(set(a) ^ set(b))} names changed")
    return out


if __name__ == "__main__":
    sys.exit(main())
