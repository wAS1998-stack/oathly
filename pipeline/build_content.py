#!/usr/bin/env python3
"""Build the app's US question files from the verified source banks.

Source banks (content/us/source/bank-2025.json, bank-2008.json) are the USCIS
civics questions, which are US-government works in the public domain
(17 U.S.C. 105). They were transcribed by the open-source civics-test-kit
project (MIT) and cross-checked for Oathly against a second independent
transcription and the official M-1778 (09/25) PDF.

Output: app/src/main/assets/content/us_2025.json and us_2008.json
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "content" / "us" / "source"
OUT = ROOT / "app" / "src" / "main" / "assets" / "content"
EXPLANATIONS = ROOT / "content" / "us" / "explanations.json"

# How many distinct answers the officer expects ("Name two ...", "Name three ...").
NEED_COUNT = {
    "2025": {10: 2, 48: 2, 65: 3, 67: 2, 69: 2, 81: 5, 126: 3},
    "2008": {9: 2, 36: 2, 51: 2, 55: 2, 64: 3, 100: 2},
}

# Questions whose answers change with elections, appointments or where you live.
DYNAMIC_KEY = {
    "2025": {23: "senators", 29: "representative", 30: "speaker", 38: "president",
             39: "vice_president", 57: "chief_justice", 61: "governor", 62: "capital"},
    "2008": {20: "senators", 23: "representative", 28: "president", 29: "vice_president",
             39: "justices_count", 40: "chief_justice", 43: "governor", 44: "capital",
             46: "president_party", 47: "speaker"},
}

EXPECTED = {"2025": (128, 20), "2008": (100, 20)}  # (questions, 65/20 questions)

# Extra ways of giving an official answer that the answer checker accepts but the app never
# shows as an official answer: synonyms USCIS lists in parentheses, common short forms and
# the names people actually use ("Christmas", "Fourth of July", "FDR").
EXTRA_ACCEPTED = {
    "2025": {
        8: ["All men are created equal"],
        13: ["Nobody is above the law", "Everyone must obey the law"],
        28: ["Connecticut Compromise"],
        48: ["Secretary of Defense"],
        51: ["Resolves disagreements about the law", "Resolves disputes", "Resolves disagreements"],
        59: ["Police", "Fire departments"],
        63: ["Women can vote", "Women and men can vote", "No poll tax"],
        71: ["16th Amendment"],
        76: ["War of Independence", "Revolution"],
        77: ["Quartering", "Boarding", "Tea Act", "Coercive Acts"],
        80: ["Battle of Trenton", "British surrender at Yorktown"],
        87: ["Louisiana Purchase"],
        94: ["Emancipation Proclamation", "Preserved the Union"],
        104: ["1929", "Stock market crash", "Crash of 1929"],
        105: ["FDR"],
        107: ["President during the Korean War", "Created the Interstate System"],
        115: ["Terrorist attack", "Terrorists attacked", "Attack on the World Trade Center"],
        116: ["Iraq War", "Afghanistan War"],
        118: ["Lightbulb", "Cars", "Internal combustion engine", "Moon landing"],
        121: ["13 colonies", "Original 13 colonies"],
        122: ["One for each state"],
        126: ["Christmas", "Thanksgiving", "Martin Luther King Day", "MLK Day", "Fourth of July",
              "July Fourth", "New Year"],
    },
    "2008": {
        24: ["People of the state"],
        42: ["Police", "Fire departments"],
        45: ["Democrats and Republicans"],
        48: ["Women can vote", "Women and men can vote", "No poll tax"],
        53: ["Defend the Constitution"],
        54: ["Eighteen (18)"],
        57: ["Eighteen (18)"],
        61: ["Taxation without representation", "Quartering", "Boarding"],
        75: ["Emancipation Proclamation", "Preserved the Union"],
        80: ["FDR"],
        96: ["13 colonies", "Original 13 colonies"],
        97: ["One for each state"],
        99: ["Fourth of July", "July Fourth", "July 4th"],
        100: ["Christmas Day", "Thanksgiving Day", "Martin Luther King Day", "MLK Day",
              "Fourth of July", "July Fourth", "New Year"],
    },
}


def derived_alternatives(answer: str) -> list[str]:
    """Shorter forms that every answer of a given shape may also be given in."""
    out = []
    # "(Battle of) Antietam/Sharpsburg": either name on its own
    m = re.search(r"(\w+)/(\w+)", answer)
    if m:
        out += [answer[:m.start()] + m.group(1) + answer[m.end():],
                answer[:m.start()] + m.group(2) + answer[m.end():]]
    # "because there were 13 original colonies" -> "13 original colonies"
    m = re.match(r"(?i)\(?because( of| there (is|are|were)| the stripes)?\)?\s+(.+)", answer)
    if m:
        out.append(m.group(3))
    # "It says all people are created equal." -> "all people are created equal"
    m = re.match(r"(?i)(it says|it identifies|they) (.+)", answer)
    if m and len(m.group(2).split()) >= 2:
        out.append(m.group(2))
    # "First president of the United States" -> "First president"
    m = re.match(r"(?i)(.+?) of the United States\.?$", answer)
    if m and "(" not in m.group(1) and len(m.group(1).split()) >= 2:
        out.append(m.group(1))
    # "Executive branch" -> "Executive", "Six (6) years" -> "Six (6)", "eighteen (18) and older" -> "eighteen (18)"
    m = re.match(r"(?i)(.+?) (branch|years|and older)$", answer)
    if m:
        out.append(m.group(1))
    return [x.strip() for x in out if x.strip() and x.strip().lower() != answer.lower()]


def short_category(cat: str) -> str:
    return {"American Government": "government", "American History": "history",
            "Symbols and Holidays": "symbols", "Integrated Civics": "civics"}[cat]


def build(version: str, explanations: dict) -> dict:
    bank = json.loads((SRC / f"bank-{version}.json").read_text(encoding="utf-8"))
    qs = bank["questions"]
    n_expected, senior_expected = EXPECTED[version]
    if len(qs) != n_expected:
        sys.exit(f"{version}: expected {n_expected} questions, got {len(qs)}")
    if sum(1 for q in qs if q["senior"]) != senior_expected:
        sys.exit(f"{version}: wrong number of 65/20 questions")

    out_q = []
    for q in qs:
        num = q["number"]
        dyn_key = DYNAMIC_KEY[version].get(num)
        if bool(dyn_key) != bool(q["dynamic"]):
            sys.exit(f"{version} Q{num}: dynamic flag mismatch")
        sub = q["subcategory"]
        m = re.match(r"^([A-Z]): (.+)$", sub)
        item = {
            "id": f"us{version}-{num:03d}",
            "n": num,
            "cat": short_category(q["category"]),
            "catTitle": q["category"],
            "sub": m.group(2) if m else sub,
            "q": q["question"],
            "a": [] if dyn_key else q["acceptableAnswers"],
            "need": NEED_COUNT[version].get(num, 1),
            "senior": bool(q["senior"]),
        }
        if not dyn_key:
            alt = list(EXTRA_ACCEPTED[version].get(num, []))
            for answer in q["acceptableAnswers"]:
                alt += derived_alternatives(answer)
            official = {a.lower() for a in q["acceptableAnswers"]}
            seen: set[str] = set()
            alt = [a for a in alt if a.lower() not in official and not (a.lower() in seen or seen.add(a.lower()))]
            if alt:
                item["alt"] = alt
        if dyn_key:
            item["dyn"] = dyn_key
            item["dynNote"] = q["dynamicNote"]
        exp = explanations.get(item["id"])
        if exp:
            item["exp"] = exp
        out_q.append(item)

    mech = bank["testMechanics"]
    return {
        "schema": 1,
        "country": "US",
        "version": version,
        "title": f"{version} civics test",
        "source": {
            "name": "U.S. Citizenship and Immigration Services (USCIS)",
            "url": bank["officialSource"],
            "note": "Questions and answers are works of the U.S. Government (public domain). "
                    "Oathly is not affiliated with USCIS or any government agency.",
            "updatesUrl": "https://www.uscis.gov/citizenship/testupdates",
        },
        "rules": {
            "asked": mech["questionsAsked"],
            "passAt": mech["passAt"],
            "failAt": mech["failAt"],
            "seniorAsked": mech["senior6520"]["questionsAsked"],
            "seniorPassAt": mech["senior6520"]["passAt"],
        },
        "questions": out_q,
    }


AU_DIR = ROOT / "content" / "au"


def build_au() -> dict | None:
    """Australian practice bank: original questions written from 'Our Common Bond' (CC BY)."""
    qfile, rfile = AU_DIR / "questions.json", AU_DIR / "test_rules.json"
    if not qfile.exists():
        return None
    src = json.loads(qfile.read_text(encoding="utf-8"))
    rules = json.loads(rfile.read_text(encoding="utf-8")) if rfile.exists() else {}
    qs = src["questions"]
    ids = [q["id"] for q in qs]
    if len(ids) != len(set(ids)):
        sys.exit("au: duplicate ids")
    for q in qs:
        if len(q["options"]) != 3 or not 0 <= q["answerIndex"] <= 2:
            sys.exit(f"au {q['id']}: needs exactly 3 options and a valid answerIndex")
        if len(set(o.strip().lower() for o in q["options"])) != 3:
            sys.exit(f"au {q['id']}: duplicate options")
        if bool(q.get("values")) != (q["part"] == 4):
            sys.exit(f"au {q['id']}: values flag must match part 4")
    per_part = {p: sum(1 for q in qs if q["part"] == p) for p in (1, 2, 3, 4)}
    if min(per_part.values()) < 20:
        sys.exit(f"au: too few questions in a part: {per_part}")
    return {
        "schema": 1,
        "country": "AU",
        "source": src["source"],
        "rules": {
            "questions": rules.get("questions", 20),
            "valuesQuestions": rules.get("valuesQuestions", 5),
            "passMarkPercent": rules.get("passMarkPercent", 75),
            "valuesRule": rules.get("valuesRule", "You must answer all five Australian values questions correctly."),
            **({"timeLimitMinutes": rules["timeLimitMinutes"]} if rules.get("timeLimitMinutes") else {}),
        },
        "questions": [
            {k: q[k] for k in ("id", "part", "partTitle", "question", "options", "answerIndex", "explanation", "values")}
            for q in qs
        ],
    }


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    explanations = {}
    if EXPLANATIONS.exists():
        explanations = json.loads(EXPLANATIONS.read_text(encoding="utf-8"))
    for version, extra in EXTRA_ACCEPTED.items():
        bad = set(extra) - set(range(1, EXPECTED[version][0] + 1))
        if bad:
            sys.exit(f"{version}: EXTRA_ACCEPTED has unknown questions {sorted(bad)}")
    for version in ("2025", "2008"):
        data = build(version, explanations)
        path = OUT / f"us_{version}.json"
        path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        with_exp = sum(1 for q in data["questions"] if "exp" in q)
        print(f"wrote {path.relative_to(ROOT)}: {len(data['questions'])} questions, {with_exp} explained")
    au = build_au()
    if au:
        path = OUT / "au.json"
        path.write_text(json.dumps(au, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
        print(f"wrote {path.relative_to(ROOT)}: {len(au['questions'])} questions")
    else:
        print("au: content/au/questions.json not found, skipped")


if __name__ == "__main__":
    main()
