#!/usr/bin/env python3
"""Build the public website (public/) from the same content the app uses.

Every page is plain static HTML, so Cloudflare Pages serves it with no build step:
  * U.S. guide, the full 2025 and 2008 question lists, one page per question,
    the 65/20 list, a page per state/territory with its current answers, and a
    practice interview that runs in the browser
  * Australian guide, practice questions by part, and a practice test
  * sitemap.xml, robots.txt, 404 page

Inputs: app/src/main/assets/content/*.json (run build_content.py first) and
public/v1/officials.json. The output is deterministic: it only changes when the
content or the officeholders change, so the daily data job can rebuild and commit it.

Set OATHLY_SITE_URL if the site is not served from https://oathly.pages.dev.
"""
from __future__ import annotations

import html
import json
import os
import re
import shutil
import sys
import tempfile
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PUBLIC = ROOT / "public"
CONTENT = ROOT / "app" / "src" / "main" / "assets" / "content"
STATIC = Path(__file__).resolve().parent / "site"

SITE = os.environ.get("OATHLY_SITE_URL", "").strip().rstrip("/") or "https://oathly.pages.dev"
PLAY = "https://play.google.com/store/apps/details?id=com.oathly.app"
US = "/us-citizenship-test/"
AU = "/australian-citizenship-test/"
USCIS_STUDY = "https://www.uscis.gov/citizenship/find-study-materials-and-resources/study-for-the-test"
USCIS_UPDATES = "https://www.uscis.gov/citizenship/testupdates"
AU_TEST_INFO = ("https://immi.homeaffairs.gov.au/citizenship/test-and-interview/"
                "learn-about-citizenship-interview-and-test/learn-about-citizenship-test")
AU_BOOKLET = "https://immi.homeaffairs.gov.au/citizenship/test-and-interview/our-common-bond"
FIND_REP = "https://www.house.gov/representatives/find-your-representative"

# What this script owns in public/. Everything is built in a scratch folder first and
# swapped in at the end, so a failed run never leaves a half-written site behind.
GENERATED = ["us-citizenship-test", "australian-citizenship-test", "assets",
             "index.html", "404.html", "sitemap.xml", "robots.txt"]
OUT = PUBLIC  # replaced by the scratch folder in main()

MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August",
          "September", "October", "November", "December"]

e = html.escape


def slugify(text: str, words: int = 14) -> str:
    text = unicodedata.normalize("NFKD", text).encode("ascii", "ignore").decode()
    text = re.sub(r"['’.]", "", text.lower())
    parts = [p for p in re.split(r"[^a-z0-9]+", text) if p]
    return "-".join(parts[:words])


def long_date(iso: str) -> str:
    y, m, d = (int(x) for x in iso[:10].split("-"))
    return f"{MONTHS[m - 1]} {d}, {y}"


def clip(text: str, limit: int = 158) -> str:
    text = " ".join(text.split())
    if len(text) <= limit:
        return text
    return text[:limit - 1].rsplit(" ", 1)[0].rstrip(",;:") + "…"


def play_link(campaign: str) -> str:
    return f"{PLAY}&amp;referrer=utm_source%3Dwebsite%26utm_medium%3D{campaign}"


# --------------------------------------------------------------------------- data

def load() -> dict:
    def read(path: Path):
        if not path.exists():
            sys.exit(f"missing {path.relative_to(ROOT)}")
        return json.loads(path.read_text(encoding="utf-8"))

    officials = read(PUBLIC / "v1" / "officials.json")
    return {
        "us": {"2025": read(CONTENT / "us_2025.json"), "2008": read(CONTENT / "us_2008.json")},
        "au": read(CONTENT / "au.json"),
        "officials": officials,
    }


def number_word(n: int) -> str:
    return {7: "Seven", 8: "Eight", 9: "Nine", 10: "Ten", 11: "Eleven"}.get(n, str(n))


def federal_answers(o: dict) -> dict[str, list[str]]:
    """Answers that are the same for everyone. Mirrors DynamicAnswers.kt in the app."""
    f = o["federal"]
    out: dict[str, list[str]] = {}
    for key, field in (("president", "president"), ("vice_president", "vicePresident"),
                       ("speaker", "speaker"), ("chief_justice", "chiefJustice")):
        if f.get(field):
            out[key] = [f[field]["name"]]
    party = (f.get("president") or {}).get("party")
    if party:
        out["president_party"] = [party if party.endswith("Party") else f"{party} (Party)"]
    if f.get("justices"):
        out["justices_count"] = [f"{number_word(f['justices'])} ({f['justices']})"]
    return out


def district_label(n: int) -> str:
    return "At-large" if n == 0 else f"District {n}"


def place_answers(p: dict) -> dict[str, list[str]]:
    """Answers that depend on where the applicant lives. Mirrors DynamicAnswers.kt."""
    is_state, is_dc = p["type"] == "state", p["type"] == "district"
    who = "D.C." if is_dc else p["name"]
    out: dict[str, list[str]] = {}
    if not is_state:
        out["senators"] = [f"{who} has no U.S. senators."]
        d = p.get("delegate")
        rep = [f"{d['name']} ({d.get('title') or 'Delegate'})"] if d else []
        out["representative"] = rep + [f"{who} has no (voting) representatives in Congress."]
    else:
        if p["senators"]:
            out["senators"] = [s["name"] for s in p["senators"]]
        reps = p["representatives"]
        if len(reps) == 1:
            out["representative"] = [f"{reps[0]['name']} (at-large)"]
        elif reps:
            out["representative"] = [
                f"It depends on your district. {p['name']} has {len(reps)} representatives; "
                "find yours on the state page or at house.gov."]
    if is_dc:
        out["governor"] = ["D.C. does not have a governor."]
        out["capital"] = ["D.C. is not a state and does not have a capital."]
    else:
        if p.get("governor"):
            out["governor"] = [p["governor"]["name"]]
        if p.get("capital"):
            out["capital"] = [p["capital"]]
    return out


def place_slug(p: dict) -> str:
    return slugify(p["name"])


def place_kind(p: dict) -> str:
    return {"state": "state", "district": "district", "territory": "territory"}[p["type"]]


# --------------------------------------------------------------------------- page shell

class Site:
    def __init__(self) -> None:
        self.pages: list[tuple[str, str | None]] = []  # (path, lastmod)

    def write(self, path: str, title: str, desc: str, body: str, crumbs: list[tuple[str, str]],
              jsonld: list[dict] | None = None, lang: str = "en-US", lastmod: str | None = None,
              scripts: str = "", index: bool = True, footer_extra: str = "") -> None:
        url = SITE + path
        ld = list(jsonld or [])
        if crumbs:
            ld.append({
                "@context": "https://schema.org", "@type": "BreadcrumbList",
                "itemListElement": [
                    {"@type": "ListItem", "position": i + 1, "name": name, "item": SITE + href}
                    for i, (name, href) in enumerate([("Oathly", "/")] + crumbs)
                ],
            })
        ld_html = "".join(
            '<script type="application/ld+json">'
            + json.dumps(x, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/")
            + "</script>\n" for x in ld)
        crumb_html = ""
        if crumbs:
            items = ['<li><a href="/">Oathly</a></li>']
            for name, href in crumbs[:-1]:
                items.append(f'<li><a href="{href}">{e(name)}</a></li>')
            items.append(f'<li aria-current="page">{e(crumbs[-1][0])}</li>')
            crumb_html = f'<nav aria-label="Breadcrumb"><ol class="crumbs">{"".join(items)}</ol></nav>'
        robots = "" if index else '<meta name="robots" content="noindex">\n'
        doc = f"""<!doctype html>
<html lang="{lang}">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{e(title)}</title>
<meta name="description" content="{e(desc)}">
{robots}<link rel="canonical" href="{url}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Oathly">
<meta property="og:title" content="{e(title)}">
<meta property="og:description" content="{e(desc)}">
<meta property="og:url" content="{url}">
<meta property="og:image" content="{SITE}/assets/og.png">
<meta name="twitter:card" content="summary_large_image">
<meta name="theme-color" content="#1f3a5f">
<link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
<link rel="apple-touch-icon" href="/assets/icon.png">
<link rel="stylesheet" href="/assets/site.css">
{ld_html}</head>
<body>
<header class="top"><div class="wrap">
<a class="brand" href="/"><img src="/assets/favicon.svg" alt="" width="30" height="30">Oathly</a>
<nav aria-label="Main">
<a href="{US}">U.S. test</a>
<a href="{US}2025-civics-questions/">128 questions</a>
<a href="{US}practice-test/">Practice</a>
<a href="{US}states/">By state</a>
<a href="{AU}">Australia</a>
</nav>
</div></header>
<main><div class="wrap">
{crumb_html}
{body}
</div></main>
<footer><div class="wrap">
<nav aria-label="Footer">
<a href="{US}">U.S. citizenship test</a>
<a href="{US}2025-civics-questions/">2025 civics questions</a>
<a href="{US}2008-civics-questions/">2008 civics questions</a>
<a href="{US}65-20-questions/">65/20 questions</a>
<a href="{US}states/">Answers by state</a>
<a href="{AU}">Australian citizenship test</a>
<a href="/privacy">Privacy</a>
</nav>
<p>Oathly is an independent study app. It is not affiliated with, endorsed by, or connected to U.S. Citizenship and Immigration Services (USCIS), the Australian Department of Home Affairs, or any government agency, and nothing here is legal advice. Always check the official sources: <a href="{USCIS_STUDY}" rel="noopener">USCIS study materials</a>, <a href="{USCIS_UPDATES}" rel="noopener">USCIS test updates</a> and <a href="{AU_BOOKLET}" rel="noopener">Our Common Bond</a>.</p>
{footer_extra}
</div></footer>
{scripts}</body>
</html>
"""
        out = OUT / path.lstrip("/")
        if path.endswith("/"):
            out = out / "index.html"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(doc, encoding="utf-8")
        if index:
            self.pages.append((path, lastmod))


def cta(campaign: str, heading: str, text: str) -> str:
    return (f'<aside class="cta"><h2>{e(heading)}</h2><p>{e(text)}</p>'
            f'<a class="btn" href="{play_link(campaign)}" rel="noopener">Get Oathly on Google Play</a></aside>')


def faq_block(faqs: list[tuple[str, str]]) -> tuple[str, dict]:
    """faqs: (question, answer as HTML). Returns the visible block and its structured data."""
    body = "".join(f"<details><summary>{e(q)}</summary><p>{a}</p></details>" for q, a in faqs)
    ld = {
        "@context": "https://schema.org", "@type": "FAQPage",
        "mainEntity": [{"@type": "Question", "name": q,
                        "acceptedAnswer": {"@type": "Answer", "text": re.sub(r"<[^>]+>", "", a)}}
                       for q, a in faqs],
    }
    return body, ld


# --------------------------------------------------------------------------- U.S. pages

VERSION_INFO = {
    "2025": {"count": 128, "slug": "2025-civics-questions", "name": "2025 civics test",
             "who": "Form N-400 filed on or after October 20, 2025"},
    "2008": {"count": 100, "slug": "2008-civics-questions", "name": "2008 civics test",
             "who": "Form N-400 filed before October 20, 2025"},
}

STATE_KEYS = ("senators", "representative", "governor", "capital")


def q_path(version: str, q: dict) -> str:
    return f"{US}{VERSION_INFO[version]['slug']}/{q['n']}-{slugify(q['q'])}/"


def us_answers(q: dict, federal: dict) -> list[str] | None:
    """Fixed answers, or the current federal answer; None when it depends on the state."""
    if not q.get("dyn"):
        return q["a"]
    return federal.get(q["dyn"])


def answer_label(q: dict, answers: list[str]) -> str:
    if q["need"] > 1:
        return f"Answer (name {q['need']})"
    return "Answer (any one of these)" if len(answers) > 1 else "Answer"


def ul(items: list[str], cls: str = "") -> str:
    c = f' class="{cls}"' if cls else ""
    return f"<ul{c}>" + "".join(f"<li>{e(x)}</li>" for x in items) + "</ul>"


def state_dependent_note(q: dict) -> str:
    what = {"senators": "U.S. senators", "representative": "U.S. representative",
            "governor": "governor", "capital": "capital"}[q["dyn"]]
    return (f'This answer depends on where you live. <a href="{US}states/">Choose your state or territory</a> '
            f'to see its current {what}.')


def build_us(site: Site, data: dict) -> None:
    officials = data["officials"]
    federal = federal_answers(officials)
    checked = long_date(officials["generatedAt"])
    lastmod = officials["generatedAt"][:10]
    places = officials["places"]

    twin: dict[tuple[str, int], tuple[str, dict]] = {}
    norm = lambda s: re.sub(r"[^a-z0-9]+", " ", s.lower()).strip()
    by_text = {v: {norm(q["q"]): q for q in data["us"][v]["questions"]} for v in ("2025", "2008")}
    for v, other in (("2025", "2008"), ("2008", "2025")):
        for q in data["us"][v]["questions"]:
            match = by_text[other].get(norm(q["q"]))
            if match:
                twin[(v, q["n"])] = (other, match)

    for version in ("2025", "2008"):
        build_us_list(site, version, data["us"][version], federal, checked, lastmod)
        build_us_questions(site, version, data["us"][version], federal, checked, lastmod, twin, places)
    build_us_hub(site, data, federal, checked, lastmod)
    build_us_senior(site, data, federal)
    build_us_states(site, data, federal, checked, lastmod)
    build_us_practice(site, data, federal, places)


def sections(bank: dict) -> list[tuple[str, str, list[dict]]]:
    """(category title, section title, questions) in test order."""
    out: list[tuple[str, str, list[dict]]] = []
    for q in bank["questions"]:
        if not out or out[-1][0] != q["catTitle"] or out[-1][1] != q["sub"]:
            out.append((q["catTitle"], q["sub"], []))
        out[-1][2].append(q)
    return out


def qa_item(version: str, q: dict, federal: dict, explain: bool = True) -> str:
    answers = us_answers(q, federal)
    star = ' <span class="tag star">65/20</span>' if q["senior"] else ""
    head = (f'<p class="q"><a href="{q_path(version, q)}"><span class="n">{q["n"]}.</span> '
            f'{e(q["q"])}</a>{star}</p>')
    if answers is None:
        body = f"<p>{state_dependent_note(q)}</p>"
    else:
        body = ul(answers)
        if q.get("dyn"):
            body += '<p class="why">This answer can change. It is checked every day.</p>'
    why = f'<p class="why">{e(q["exp"])}</p>' if explain and q.get("exp") else ""
    return f'<li id="q{q["n"]}">{head}{body}{why}</li>'


def build_us_list(site: Site, version: str, bank: dict, federal: dict, checked: str, lastmod: str) -> None:
    info = VERSION_INFO[version]
    r = bank["rules"]
    n = info["count"]
    other = "2008" if version == "2025" else "2025"
    path = f"{US}{info['slug']}/"
    toc, blocks, last_cat = [], [], None
    for cat, sub, qs in sections(bank):
        anchor = slugify(f"{cat} {sub}")
        if cat != last_cat:
            blocks.append(f"<h2>{e(cat)}</h2>")
            last_cat = cat
        toc.append(f'<li><a href="#{anchor}">{e(sub)}</a> <span class="muted">({qs[0]["n"]}–{qs[-1]["n"]})</span></li>')
        blocks.append(f'<h3 id="{anchor}">{e(sub)}</h3><ol class="qa">'
                      + "".join(qa_item(version, q, federal) for q in qs) + "</ol>")
    title = f"{n} Civics Questions and Answers for the U.S. Citizenship Test ({version} Version)"
    desc = (f"All {n} civics questions and answers for the {version} U.S. naturalization test, with a short "
            f"explanation of each one and the current President, Speaker and Chief Justice. Free to read and print.")
    body = f"""<h1>{n} civics questions and answers ({version} civics test)</h1>
<p class="lead">This is the complete list of civics questions for the <strong>{info['name']}</strong>, which applies to {info['who']}. At the interview the officer asks up to {r['asked']} of these questions out loud, and you pass when you answer {r['passAt']} correctly.</p>
<p>Many questions have more than one acceptable answer. Unless the question asks for more, you only need to give one. Questions marked <span class="tag star">65/20</span> are the {sum(1 for q in bank['questions'] if q['senior'])} questions for applicants who are 65 or older and have been permanent residents for at least 20 years. Answers that change with elections were last updated on {checked}.</p>
<div class="btns noprint">
<a class="btn" href="{US}practice-test/">Take a practice interview</a>
<button class="btn alt" type="button" onclick="window.print()">Print this list</button>
</div>
<p class="muted noprint">Filed your N-400 {'before' if version == '2025' else 'on or after'} October 20, 2025? You need the <a href="{US}{VERSION_INFO[other]['slug']}/">{other} civics test ({VERSION_INFO[other]['count']} questions)</a> instead.</p>
<nav class="card noprint" aria-label="Sections"><strong>Sections</strong><ul>{''.join(toc)}</ul></nav>
{''.join(blocks)}
{cta('list' + version, 'Learn these faster on your phone', f'Oathly reads each question aloud, checks the answer you say or type, and brings back the questions you miss. All {n} questions are free.')}
<p class="muted">Source: the civics questions and answers are published by USCIS and are in the public domain. <a href="{e(bank['source']['url'])}" rel="noopener">See the official list (PDF)</a>.</p>"""
    ld = [{
        "@context": "https://schema.org", "@type": "ItemList", "name": title,
        "numberOfItems": n,
        "itemListElement": [{"@type": "ListItem", "position": q["n"], "name": q["q"],
                             "url": SITE + q_path(version, q)} for q in bank["questions"]],
    }]
    site.write(path, title, desc, body,
               [("U.S. citizenship test", US), (f"{version} civics questions", path)], ld, lastmod=lastmod)


def build_us_questions(site: Site, version: str, bank: dict, federal: dict, checked: str, lastmod: str,
                       twin: dict, places: list[dict]) -> None:
    info = VERSION_INFO[version]
    list_path = f"{US}{info['slug']}/"
    qs = bank["questions"]
    for i, q in enumerate(qs):
        path = q_path(version, q)
        answers = us_answers(q, federal)
        state_dep = answers is None
        tags = [f'<span class="tag">{e(info["name"])} · question {q["n"]} of {info["count"]}</span>',
                f'<span class="tag">{e(q["catTitle"])}: {e(q["sub"])}</span>']
        if q["senior"]:
            tags.append('<span class="tag star">65/20 question</span>')

        if state_dep:
            answer_html = f'<div class="answer"><p class="label">Answer</p><p>{state_dependent_note(q)}</p></div>'
            note = {
                "senators": "Each state has two U.S. senators, and either name is a correct answer. Residents of Washington, D.C. and of U.S. territories should answer that it has no U.S. senators.",
                "representative": f'Your representative depends on the congressional district where you live. Residents of territories with a nonvoting Delegate or Resident Commissioner may give that person\'s name, or say the territory has no voting representatives in Congress. You can confirm yours at <a href="{FIND_REP}" rel="noopener">house.gov</a>.',
                "governor": "Residents of Washington, D.C. should answer that D.C. does not have a governor.",
                "capital": "Residents of Washington, D.C. should answer that D.C. is not a state and does not have a capital. Residents of U.S. territories should name the capital of the territory.",
            }[q["dyn"]]
            how = f"<p>{note}</p>"
            links = "".join(f'<li><a href="{US}states/{place_slug(p)}/">{e(p["name"])}</a></li>' for p in places)
            how += f'<h2>Find the answer for your state</h2><ul class="cols">{links}</ul>'
            desc_answer = "The answer depends on your state. See the current answer for every state and territory."
        else:
            answer_html = (f'<div class="answer"><p class="label">{answer_label(q, answers)}</p>'
                           f'{ul(answers)}</div>')
            if q["need"] > 1:
                how = f"<p>The officer needs <strong>{q['need']} answers</strong> for this question. Any {q['need']} from the list are fine.</p>"
            elif len(answers) > 1:
                how = "<p>You only need to give <strong>one</strong> of these answers. Pick the one that is easiest for you to remember and say.</p>"
            else:
                how = ""
            if q.get("dyn"):
                how += (f'<p>This answer changes after elections and appointments. Oathly checks it against public '
                        f'records every day; it was last updated on {checked}. You can confirm it on the '
                        f'<a href="{USCIS_UPDATES}" rel="noopener">USCIS test updates page</a>.</p>')
            desc_answer = "Answer: " + "; ".join(answers[:3]) + "."

        why = f'<h2>Why this is the answer</h2><p>{e(q["exp"])}</p>' if q.get("exp") else ""
        senior = ""
        if q["senior"]:
            senior = (f'<p>This is one of the 20 questions for applicants who are 65 or older and have been '
                      f'permanent residents for 20 years or more. <a href="{US}65-20-questions/">See all 65/20 questions</a>.</p>')
        other = twin.get((version, q["n"]))
        twin_html = ""
        if other:
            ov, oq = other
            twin_html = (f'<p class="muted">The same question is number {oq["n"]} on the '
                         f'<a href="{q_path(ov, oq)}">{ov} civics test</a>.</p>')

        prev_q, next_q = (qs[i - 1] if i else None), (qs[i + 1] if i + 1 < len(qs) else None)
        pn = '<nav class="pn" aria-label="Previous and next question">'
        pn += (f'<a href="{q_path(version, prev_q)}"><small>← Question {prev_q["n"]}</small>{e(prev_q["q"])}</a>'
               if prev_q else "<span></span>")
        pn += (f'<a class="next" href="{q_path(version, next_q)}"><small>Question {next_q["n"]} →</small>{e(next_q["q"])}</a>'
               if next_q else "<span></span>")
        pn += "</nav>"

        siblings = [s for s in qs if s["sub"] == q["sub"] and s["catTitle"] == q["catTitle"] and s["n"] != q["n"]]
        near = sorted(siblings, key=lambda s: abs(s["n"] - q["n"]))[:6]
        near.sort(key=lambda s: s["n"])
        related = ""
        if near:
            related = (f'<h2>More questions on {e(q["sub"])}</h2><ul>'
                       + "".join(f'<li><a href="{q_path(version, s)}">{s["n"]}. {e(s["q"])}</a></li>' for s in near)
                       + f'</ul><p><a href="{list_path}">See all {info["count"]} questions and answers</a></p>')

        title = f"{clip(q['q'], 70)} ({version} Civics Test Answer)"
        desc = clip(f"{version} civics test question {q['n']}. {desc_answer} {q.get('exp', '')}", 200)
        body = f"""<h1>{e(q['q'])}</h1>
<p>{''.join(tags)}</p>
{answer_html}
{how}
{why}
{senior}
{twin_html}
{pn}
{related}
{cta('question', 'Practice this question out loud', 'At the interview you hear the question and answer by speaking. Oathly reads each question aloud, checks what you say, and repeats the ones you miss.')}"""
        ld = []
        if not state_dep:
            ld.append({
                "@context": "https://schema.org", "@type": "Quiz",
                "name": f"U.S. citizenship civics test ({version}): question {q['n']}",
                "about": {"@type": "Thing", "name": "United States naturalization civics test"},
                "educationalAlignment": [{"@type": "AlignmentObject", "alignmentType": "educationalSubject",
                                          "targetName": "Civics"}],
                "hasPart": [{
                    "@type": "Question", "eduQuestionType": "Flashcard", "text": q["q"],
                    "acceptedAnswer": {"@type": "Answer", "text": "; ".join(answers)},
                }],
            })
        site.write(path, title, desc, body,
                   [("U.S. citizenship test", US), (f"{version} civics questions", list_path),
                    (f"Question {q['n']}", path)],
                   ld, lastmod=lastmod if q.get("dyn") else None)


def build_us_hub(site: Site, data: dict, federal: dict, checked: str, lastmod: str) -> None:
    r25, r08 = data["us"]["2025"]["rules"], data["us"]["2008"]["rules"]
    f = data["officials"]["federal"]

    def dyn_row(label: str, key: str) -> str:
        q = next((x for x in data["us"]["2025"]["questions"] if x.get("dyn") == key), None)
        name = (federal.get(key) or ["See the USCIS test updates page"])[0]
        link = f'<a href="{q_path("2025", q)}">{e(label)}</a>' if q else e(label)
        return f"<tr><td>{link}</td><td><strong>{e(name)}</strong></td></tr>"

    faqs = [
        ("Which version of the civics test will I take?",
         f'It depends on when you filed Form N-400. If you filed on or after October 20, 2025, you take the <a href="{US}2025-civics-questions/">2025 civics test</a> (128 questions). If you filed before that date, you take the <a href="{US}2008-civics-questions/">2008 civics test</a> (100 questions).'),
        ("How many questions are on the U.S. citizenship test?",
         f"On the 2025 test, the officer asks up to {r25['asked']} questions from the list of 128, and you must answer {r25['passAt']} correctly. On the 2008 test, the officer asks up to {r08['asked']} questions from the list of 100, and you must answer {r08['passAt']} correctly."),
        ("Is the civics test multiple choice?",
         "No. It is an oral test. A USCIS officer asks each question out loud in English and you answer out loud. That is why it helps to practice by speaking your answers, not only reading them."),
        ("Does the officer ask every question?",
         f"No. The officer stops as soon as the result is certain. On the 2025 test that means the test ends when you have {r25['passAt']} correct answers, or when you have {r25['failAt']} incorrect answers."),
        ("What is the 65/20 rule?",
         f'If you are 65 or older and have been a lawful permanent resident for 20 years or more, you study only 20 specially marked questions. The officer asks {r25["seniorAsked"]} of them and you need {r25["seniorPassAt"]} correct, and you may take the test in the language of your choice. <a href="{US}65-20-questions/">See the 65/20 questions</a>.'),
        ("Which answers change over time?",
         f'The names of the President, Vice President, Speaker of the House, Chief Justice, and your own senators, representative and governor. You must give the answer that is correct on the day of your interview. Oathly checks these every day, and USCIS lists them on its <a href="{USCIS_UPDATES}" rel="noopener">test updates page</a>.'),
        ("What happens if I do not pass?",
         f'USCIS gives you one more chance to take the part you did not pass, usually between 60 and 90 days after your first interview. Check your own notice and the <a href="{USCIS_STUDY}" rel="noopener">official USCIS study page</a> for the rules that apply to you.'),
        ("Is there also an English test?",
         "Yes. Besides civics, the officer checks your English: speaking (during the interview itself), reading (you read one of up to three sentences aloud correctly) and writing (you write one of up to three sentences correctly). Some applicants are exempt from the English requirement because of age and time as a permanent resident."),
    ]
    faq_html, faq_ld = faq_block(faqs)
    title = "U.S. Citizenship Test: Civics Questions, Answers and Free Practice"
    desc = ("Study for the civics test at your naturalization interview: all 128 questions and answers for the 2025 "
            "test, the 100 questions of the 2008 test, the 65/20 list, current answers for your state and a free practice interview.")
    body = f"""<h1>U.S. citizenship test: civics questions, answers and free practice</h1>
<p class="lead">At your naturalization interview, a USCIS officer asks you civics questions about American government and history. You can prepare for every one of them: the full list of questions and answers is public. This guide has both versions of the test, the answers that are correct today, and a free practice interview.</p>
<ul class="grid">
<li><a href="{US}2025-civics-questions/"><strong>2025 civics test</strong><span>All 128 questions and answers. For N-400 filed on or after October 20, 2025.</span></a></li>
<li><a href="{US}2008-civics-questions/"><strong>2008 civics test</strong><span>All 100 questions and answers. For N-400 filed before October 20, 2025.</span></a></li>
<li><a href="{US}practice-test/"><strong>Practice interview</strong><span>Random questions with the real stopping rules. Free, no sign-up.</span></a></li>
<li><a href="{US}states/"><strong>Answers for your state</strong><span>Your senators, representatives, governor and capital.</span></a></li>
<li><a href="{US}65-20-questions/"><strong>65/20 questions</strong><span>The 20 questions for applicants 65 or older with 20 years of residence.</span></a></li>
</ul>

<h2>How the civics test works</h2>
<table>
<tr><th></th><th>2025 civics test</th><th>2008 civics test</th></tr>
<tr><td>Who takes it</td><td>N-400 filed on or after October 20, 2025</td><td>N-400 filed before October 20, 2025</td></tr>
<tr><td>Questions to study</td><td>128</td><td>100</td></tr>
<tr><td>Questions asked</td><td>Up to {r25['asked']}</td><td>Up to {r08['asked']}</td></tr>
<tr><td>Correct answers to pass</td><td>{r25['passAt']}</td><td>{r08['passAt']}</td></tr>
<tr><td>Test stops when you miss</td><td>{r25['failAt']}</td><td>{r08['failAt']}</td></tr>
<tr><td>Format</td><td colspan="2">Oral: the officer asks, you answer out loud in English</td></tr>
</table>
<p>The officer chooses the questions, so you cannot know which ones you will get. The safest plan is to learn the whole list, then practice answering out loud until the answers come quickly.</p>

<h2>Answers that are correct today</h2>
<p>Some answers change after elections and appointments. These were last updated on {checked}.</p>
<table>
{dyn_row('President of the United States', 'president')}
{dyn_row('Vice President', 'vice_president')}
{dyn_row('Speaker of the House', 'speaker')}
{dyn_row('Chief Justice of the United States', 'chief_justice')}
<tr><td>Political party of the President <span class="muted">(2008 test)</span></td><td><strong>{e((f.get('president') or {}).get('party', ''))}</strong></td></tr>
<tr><td>Your senators, representative, governor and capital</td><td><a href="{US}states/">Choose your state</a></td></tr>
</table>

<h2>How to study</h2>
<ol>
<li><strong>Confirm your version.</strong> Check the date you filed Form N-400, then use the 2025 or the 2008 list.</li>
<li><strong>Read the list once from start to finish.</strong> Each question page explains why the answer is what it is, which makes it far easier to remember.</li>
<li><strong>Pick one answer per question.</strong> Most questions accept several answers. Choose the one that is shortest and easiest for you to say.</li>
<li><strong>Learn your own answers.</strong> Write down your two senators, your representative, your governor and your state capital.</li>
<li><strong>Practice out loud every day.</strong> A few minutes a day works better than one long session. Repeat the questions you miss until you stop missing them.</li>
</ol>
{cta('ushub', 'Practice like the real interview', 'The Oathly app asks the questions aloud, checks your spoken or typed answer, shows the right answers for your state, and tells you your chance of passing today.')}

<h2>Common questions</h2>
{faq_html}"""
    site.write(US, title, desc, body, [("U.S. citizenship test", US)], [faq_ld], lastmod=lastmod)


def build_us_senior(site: Site, data: dict, federal: dict) -> None:
    path = f"{US}65-20-questions/"
    r = data["us"]["2025"]["rules"]
    parts = []
    for version in ("2025", "2008"):
        qs = [q for q in data["us"][version]["questions"] if q["senior"]]
        parts.append(f'<h2 id="v{version}">65/20 questions on the {version} civics test</h2>'
                     f'<p class="muted">For {VERSION_INFO[version]["who"]}.</p><ol class="qa">'
                     + "".join(qa_item(version, q, federal) for q in qs) + "</ol>")
    title = "65/20 Citizenship Test Questions and Answers (the 20 Starred Questions)"
    desc = ("The 20 civics questions for applicants who are 65 or older and have been permanent residents for 20 years, "
            "with answers and short explanations, for both the 2025 and 2008 versions of the test.")
    body = f"""<h1>65/20 citizenship test questions and answers</h1>
<p class="lead">If you are <strong>65 or older</strong> and have been a lawful permanent resident for <strong>20 years or more</strong> when you file, you get an easier civics test. You study only 20 marked questions instead of the whole list. The officer asks {r['seniorAsked']} of them and you need {r['seniorPassAt']} correct answers. You may also take the test in the language of your choice.</p>
<p>The 20 questions are different on each version of the test. Use the list that matches the date you filed Form N-400.</p>
<div class="btns noprint"><a class="btn" href="{US}practice-test/">Practice the 65/20 questions</a><button class="btn alt" type="button" onclick="window.print()">Print this list</button></div>
{''.join(parts)}
{cta('senior', 'Study only your 20 questions', 'Turn on 65/20 mode in the Oathly app to see just these questions, hear them read aloud at the speed you choose, and practice a 10-question interview.')}"""
    site.write(path, title, desc, body, [("U.S. citizenship test", US), ("65/20 questions", path)])


def build_us_states(site: Site, data: dict, federal: dict, checked: str, lastmod: str) -> None:
    places = data["officials"]["places"]
    index_path = f"{US}states/"
    nums = {v: {q["dyn"]: q for q in data["us"][v]["questions"] if q.get("dyn")} for v in ("2025", "2008")}

    def group(kind: str) -> str:
        return "".join(f'<li><a href="{index_path}{place_slug(p)}/">{e(p["name"])}</a></li>'
                       for p in places if p["type"] == kind)

    title = "Citizenship Test Answers by State: Senators, Representatives, Governor, Capital"
    desc = ("Four civics test questions depend on where you live. Choose your state or territory to see its current "
            "U.S. senators, representatives, governor and capital, checked every day.")
    body = f"""<h1>Citizenship test answers for your state</h1>
<p class="lead">Four questions on the civics test have a different answer depending on where you live: your U.S. senators, your U.S. representative, your governor and your state capital. Choose your state or territory to see the answers that are correct today.</p>
<p class="muted">Last updated on {checked}. Checked every day against public records.</p>
<h2>States</h2><ul class="cols">{group('state')}</ul>
<h2>Washington, D.C. and U.S. territories</h2><ul class="cols">{group('district')}{group('territory')}</ul>
{cta('states', 'Let the app remember your state', 'Choose your state once in Oathly and it shows your own senators, representative, governor and capital, and notifies you if one of them changes.')}"""
    site.write(index_path, title, desc, body,
               [("U.S. citizenship test", US), ("Answers by state", index_path)], lastmod=lastmod)

    for p in places:
        name, path = p["name"], f"{index_path}{place_slug(p)}/"
        kind = place_kind(p)
        ans = place_answers(p)
        is_state = p["type"] == "state"
        the = "the " if p["code"] in ("DC", "VI", "MP") else ""
        where = f"{the}{name}"

        def qref(key: str) -> str:
            bits = []
            for v in ("2025", "2008"):
                q = nums[v].get(key)
                if q:
                    bits.append(f'<a href="{q_path(v, q)}">question {q["n"]} ({v} test)</a>')
            return " · ".join(bits)

        def block(key: str, heading: str, extra: str = "") -> str:
            a = ans.get(key)
            if key == "representative" and is_state and len(p["representatives"]) > 1:
                return ""
            inner = ul(a) if a else f'<p>This answer is being updated. Check the <a href="{USCIS_UPDATES}" rel="noopener">USCIS test updates page</a>.</p>'
            q25 = nums["2025"].get(key)
            return (f'<h2>{e(heading)}</h2><p class="muted">{qref(key)}</p>'
                    f'<p><em>{e(q25["q"]) if q25 else ""}</em></p>'
                    f'<div class="answer"><p class="label">Answer for {e(where)}</p>{inner}</div>{extra}')

        sen_extra = "<p>Either senator is a correct answer. You only need to name one.</p>" if is_state else ""
        rep_html = block("representative", f"U.S. representative for {where}")
        if is_state and len(p["representatives"]) > 1:
            rows = "".join(
                f"<tr><td>{district_label(r['district'])}</td><td><strong>{e(r['name'])}</strong></td><td>{e(r.get('party', ''))}</td></tr>"
                for r in p["representatives"])
            q25 = nums["2025"]["representative"]
            rep_html = (f'<h2>U.S. representatives for {e(name)}</h2><p class="muted">{qref("representative")}</p>'
                        f'<p><em>{e(q25["q"])}</em></p>'
                        f'<p>{e(name)} has {len(p["representatives"])} congressional districts, and the correct answer is the '
                        f'representative for the district where you live. If you do not know your district, enter your ZIP code at '
                        f'<a href="{FIND_REP}" rel="noopener">house.gov</a>.</p>'
                        f'<table><tr><th>District</th><th>Representative</th><th>Party</th></tr>{rows}</table>')

        fed_rows = ""
        for label, key in (("President", "president"), ("Vice President", "vice_president"),
                           ("Speaker of the House", "speaker"), ("Chief Justice", "chief_justice")):
            if federal.get(key):
                q = nums["2025"].get(key)
                fed_rows += (f'<tr><td><a href="{q_path("2025", q)}">{label}</a></td>'
                             f'<td><strong>{e(federal[key][0])}</strong></td></tr>')

        faqs = []
        if is_state and p["senators"]:
            faqs.append((f"Who are the U.S. senators for {name}?",
                         f"The U.S. senators for {e(name)} are {e(' and '.join(s['name'] for s in p['senators']))}. On the citizenship test you only need to name one of them."))
        elif not is_state:
            faqs.append((f"Who are the U.S. senators for {where}?", e(ans["senators"][0])
                         + " That is the answer to give on the citizenship test."))
        if ans.get("governor"):
            faqs.append((f"Who is the governor of {where}?",
                         e(ans["governor"][0]) if p["type"] == "district"
                         else f"The governor of {e(where)} is {e(ans['governor'][0])}."))
        if ans.get("capital"):
            faqs.append((f"What is the capital of {where}?",
                         e(ans["capital"][0]) if p["type"] == "district"
                         else f"The capital of {e(where)} is {e(ans['capital'][0])}."))
        faq_html, faq_ld = faq_block(faqs)

        short = {"state": "Senators, Representatives, Governor and Capital",
                 "district": "Senators, Delegate, Governor and Capital",
                 "territory": "Senators, Delegate, Governor and Capital"}[p["type"]]
        title = f"{name} Citizenship Test Answers: {short}"
        gov = (ans.get("governor") or [""])[0]
        desc = clip(f"Current civics test answers for {where}"
                    + (f": senators {' and '.join(s['name'] for s in p['senators'])}, governor {gov}, capital {p['capital']}"
                       if is_state and p["senators"] and gov else "")
                    + f". Last updated {checked} and checked every day.", 170)
        body = f"""<h1>{e(name)} citizenship test answers</h1>
<p class="lead">If you live in {e(where)}, these are your answers to the civics test questions that depend on your {kind}. They are checked against public records every day and were last updated on <strong>{checked}</strong>.</p>
{block('senators', f'U.S. senators for {where}', sen_extra)}
{rep_html}
{block('governor', f'Governor of {where}')}
{block('capital', f'Capital of {where}')}
<h2>Answers that are the same in every state</h2>
<table>{fed_rows}</table>
<p><a href="{US}2025-civics-questions/">See all 128 questions and answers</a> or <a href="{US}practice-test/">take a practice interview</a>.</p>
{cta('state', f'Practice with your {name} answers', f'Choose {name} in the Oathly app and the practice interview checks your answers against your own senators, representative and governor.')}
<h2>Common questions</h2>
{faq_html}
<p class="muted">Sources: members of Congress from the public-domain congress-legislators project, governors from Wikidata. Always confirm on the <a href="{USCIS_UPDATES}" rel="noopener">USCIS test updates page</a> before your interview. <a href="{index_path}">Choose a different state</a>.</p>"""
        site.write(path, title, desc, body,
                   [("U.S. citizenship test", US), ("Answers by state", index_path), (name, path)],
                   [faq_ld], lastmod=lastmod)


def build_us_practice(site: Site, data: dict, federal: dict, places: list[dict]) -> None:
    path = f"{US}practice-test/"
    out = OUT / "assets" / "data"
    out.mkdir(parents=True, exist_ok=True)
    for version in ("2025", "2008"):
        bank = data["us"][version]
        payload = {
            "rules": bank["rules"],
            "questions": [{
                "n": q["n"], "q": q["q"], "a": us_answers(q, federal) or [], "need": q["need"],
                "senior": q["senior"], "exp": q.get("exp", ""), "dyn": q.get("dyn"),
                "url": q_path(version, q),
            } for q in bank["questions"]],
        }
        (out / f"us-{version}.json").write_text(
            json.dumps(payload, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    (out / "places.json").write_text(json.dumps(
        {p["code"]: {"name": p["name"], "answers": place_answers(p)} for p in places},
        ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")

    r25, r08 = data["us"]["2025"]["rules"], data["us"]["2008"]["rules"]
    options = "".join(f'<option value="{p["code"]}">{e(p["name"])}</option>' for p in places)
    title = "Free U.S. Citizenship Practice Test: Civics Interview (2025 and 2008)"
    desc = ("Take a free practice civics interview with random questions and the real stopping rules. "
            "Works for the 2025 test (128 questions), the 2008 test (100 questions) and the 65/20 list. No sign-up.")
    body = f"""<h1>Free U.S. citizenship practice test</h1>
<p class="lead">This practice interview works like the real civics test. You get random questions, you say each answer out loud, then you check it. The test stops as soon as you pass or can no longer pass, just as it does with the officer.</p>
<div class="quiz" id="quiz" data-mode="us" data-base="/assets/data/">
<form class="card" id="quiz-setup">
<label for="version">Which test are you taking?</label>
<select id="version" name="version">
<option value="2025">2025 civics test (N-400 filed on or after October 20, 2025)</option>
<option value="2008">2008 civics test (N-400 filed before October 20, 2025)</option>
</select>
<label for="state">Where do you live?</label>
<select id="state" name="state"><option value="">Choose your state or territory</option>{options}</select>
<p class="muted">Used to show your own senators, governor and capital. It stays in your browser.</p>
<label class="check"><input type="checkbox" name="senior"> <span>I am 65 or older and have been a permanent resident for 20 years (65/20 questions only)</span></label>
<div class="btns"><button class="btn" type="submit">Start the practice interview</button></div>
</form>
<div id="quiz-stage" aria-live="polite"></div>
<noscript><p>The practice interview needs JavaScript. You can still study the <a href="{US}2025-civics-questions/">full list of questions and answers</a>.</p></noscript>
</div>
<h2>How the practice interview is scored</h2>
<ul>
<li><strong>2025 civics test:</strong> up to {r25['asked']} questions. You pass with {r25['passAt']} correct answers; the test ends early if you miss {r25['failAt']}.</li>
<li><strong>2008 civics test:</strong> up to {r08['asked']} questions. You pass with {r08['passAt']} correct answers; the test ends early if you miss {r08['failAt']}.</li>
<li><strong>65/20:</strong> {r25['seniorAsked']} questions from the 20 marked ones. You pass with {r25['seniorPassAt']} correct answers.</li>
</ul>
<p>Be honest when you mark yourself. At the interview the officer accepts any answer from the official list, even if your wording is a little different, but not an answer that is only close.</p>
{cta('practice', 'Want your answers checked for you?', 'In the Oathly app you speak or type your answer and the app checks it, with questions read aloud, your state answers built in, and a daily estimate of your chance of passing.')}
<p>Prefer to read first? See the <a href="{US}2025-civics-questions/">128 questions and answers</a>, the <a href="{US}2008-civics-questions/">100 questions of the 2008 test</a> or the <a href="{US}65-20-questions/">65/20 list</a>.</p>"""
    site.write(path, title, desc, body, [("U.S. citizenship test", US), ("Practice test", path)],
               scripts='<script src="/assets/quiz.js" defer></script>\n')


# --------------------------------------------------------------------------- Australian pages

def au_part_path(part: int, title: str) -> str:
    return f"{AU}practice-questions/part-{part}-{slugify(title)}/"


def build_au(site: Site, data: dict) -> None:
    bank = data["au"]
    r = bank["rules"]
    src = bank["source"]
    parts: dict[int, tuple[str, list[dict]]] = {}
    for q in bank["questions"]:
        parts.setdefault(q["part"], (q["partTitle"], []))[1].append(q)
    total = len(bank["questions"])
    pass_mark = -(-r["questions"] * r["passMarkPercent"] // 100)
    attribution = f'<p>{e(src["attribution"])}</p>'
    minutes = r.get("timeLimitMinutes")
    time_text = f" You have {minutes} minutes." if minutes else ""

    # hub
    cards = "".join(
        f'<li><a href="{au_part_path(n, t)}"><strong>Part {n}: {e(t)}</strong>'
        f'<span>{len(qs)} practice questions{" · you must get every values question right" if n == 4 else ""}</span></a></li>'
        for n, (t, qs) in sorted(parts.items()))
    faqs = [
        ("How many questions are on the Australian citizenship test?",
         f"The test has {r['questions']} multiple-choice questions. {r['valuesQuestions']} of them are about Australian values.{time_text}"),
        ("What is the pass mark for the Australian citizenship test?",
         f"You need at least {r['passMarkPercent']}% overall, which is {pass_mark} of {r['questions']}, and you must answer all {r['valuesQuestions']} Australian values questions correctly. One wrong values question means you do not pass, even with a high total score."),
        ("What do I need to study?",
         f'Everything in the test comes from the testable section of the official booklet, <a href="{AU_BOOKLET}" rel="noopener">Australian Citizenship: Our Common Bond</a>. It has four parts: Australia and its people; Australia\'s democratic beliefs, rights and liberties; Government and the law in Australia; and Australian values.'),
        ("Is the test in English?",
         "Yes. The test is a computer-based, multiple-choice test conducted in English."),
        ("Are these the real test questions?",
         "No. The real questions are not published. Oathly's practice questions are original and are written from the testable section of Our Common Bond, so they cover the same facts in the same multiple-choice format."),
    ]
    faq_html, faq_ld = faq_block(faqs)
    title = "Australian Citizenship Test: Free Practice Questions and Practice Test"
    desc = (f"Practise for the Australian citizenship test with {total} free questions written from Our Common Bond, "
            f"including the Australian values questions, plus a {r['questions']}-question practice test with the real pass rule.")
    body = f"""<h1>Australian citizenship test: free practice questions</h1>
<p class="lead">The Australian citizenship test has {r['questions']} multiple-choice questions. To pass you need {r['passMarkPercent']}% overall <strong>and</strong> every one of the {r['valuesQuestions']} Australian values questions correct.{time_text} Practise here with {total} free questions written from the official booklet, <em>Our Common Bond</em>.</p>
<div class="btns"><a class="btn" href="{AU}practice-test/">Take a practice test</a></div>
<h2>Practice questions by part</h2>
<ul class="grid">{cards}</ul>
<h2>How the test works</h2>
<table>
<tr><td>Questions</td><td>{r['questions']}, multiple choice</td></tr>
<tr><td>Australian values questions</td><td>{r['valuesQuestions']}, and all must be correct</td></tr>
<tr><td>Pass mark</td><td>{r['passMarkPercent']}% ({pass_mark} of {r['questions']})</td></tr>
{f'<tr><td>Time allowed</td><td>{minutes} minutes</td></tr>' if minutes else ''}
<tr><td>Language</td><td>English</td></tr>
<tr><td>What it covers</td><td>The testable section of <em>Our Common Bond</em></td></tr>
</table>
<p>Check the <a href="{AU_TEST_INFO}" rel="noopener">Department of Home Affairs website</a> for the current rules before your test.</p>
<h2>How to study</h2>
<ol>
<li><strong>Read the testable section of Our Common Bond.</strong> Every question comes from it.</li>
<li><strong>Start with Part 4, Australian values.</strong> You cannot afford to miss any of these, so learn them until you get them right every time.</li>
<li><strong>Work through the practice questions part by part</strong> and read the explanation when you get one wrong.</li>
<li><strong>Take full practice tests</strong> until you pass comfortably several times in a row.</li>
</ol>
{cta('auhub', 'Practise on your phone', 'The Oathly app has all of these questions, full practice tests with the real pass rule, and brings back the questions you miss until you know them.')}
<h2>Common questions</h2>
{faq_html}"""
    site.write(AU, title, desc, body, [("Australian citizenship test", AU)], [faq_ld], lang="en-AU",
               footer_extra=attribution)

    # one page per part
    ordered = sorted(parts.items())
    for n, (t, qs) in ordered:
        path = au_part_path(n, t)
        items = []
        for i, q in enumerate(qs, 1):
            opts = ""
            for k, o in enumerate(q["options"]):
                if k == q["answerIndex"]:
                    opts += f'<li class="right">{e(o)} <span class="muted">(correct)</span></li>'
                else:
                    opts += f"<li>{e(o)}</li>"
            items.append(f'<li><p class="q"><span class="n">{i}.</span> {e(q["question"])}</p>'
                         f'<ul class="opts">{opts}</ul><p class="why">{e(q["explanation"])}</p></li>')
        others = "".join(f'<li><a href="{au_part_path(m, mt)}">Part {m}: {e(mt)}</a></li>'
                         for m, (mt, _) in ordered if m != n)
        values_note = ("<p><strong>These are the questions you cannot get wrong.</strong> The real test includes "
                       f"{r['valuesQuestions']} Australian values questions and you must answer all of them correctly to pass.</p>"
                       if n == 4 else "")
        title = f"Australian Citizenship Test Practice Questions: Part {n}, {t}"
        desc = clip(f"{len(qs)} free practice questions with answers and explanations for Part {n} of the Australian "
                    f"citizenship test, {t}, written from Our Common Bond.", 170)
        body = f"""<h1>Part {n}: {e(t)}</h1>
<p class="lead">{len(qs)} practice questions with answers and explanations for Part {n} of the Australian citizenship test. The correct answer is marked under each question.</p>
{values_note}
<div class="btns noprint"><a class="btn" href="{AU}practice-test/">Take a practice test</a></div>
<ol class="qa">{''.join(items)}</ol>
<h2>Other parts</h2><ul>{others}</ul>
{cta('aupart', 'Test yourself without seeing the answers', 'In the Oathly app you answer first and see the explanation after, and the questions you miss come back until you know them.')}"""
        site.write(path, title, desc, body,
                   [("Australian citizenship test", AU), (f"Part {n}", path)], lang="en-AU",
                   footer_extra=attribution)

    # practice test
    out = OUT / "assets" / "data"
    out.mkdir(parents=True, exist_ok=True)
    (out / "au.json").write_text(json.dumps({
        "rules": {k: r[k] for k in ("questions", "valuesQuestions", "passMarkPercent")},
        "questions": [{k: q[k] for k in ("question", "options", "answerIndex", "explanation", "values")}
                      for q in bank["questions"]],
    }, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    path = f"{AU}practice-test/"
    title = "Free Australian Citizenship Practice Test (20 Questions, Real Pass Rule)"
    desc = (f"Take a free {r['questions']}-question Australian citizenship practice test, including "
            f"{r['valuesQuestions']} Australian values questions, scored with the real pass rule. New questions every time.")
    body = f"""<h1>Free Australian citizenship practice test</h1>
<p class="lead">{r['questions']} multiple-choice questions, including {r['valuesQuestions']} on Australian values, chosen at random from {total} practice questions. To pass you need {pass_mark} of {r['questions']} and every values question correct, the same rule as the real test.</p>
<div class="quiz" id="quiz" data-mode="au" data-base="/assets/data/">
<div class="btns"><button class="btn" type="button" id="quiz-start">Start the practice test</button></div>
<div id="quiz-stage" aria-live="polite"></div>
<noscript><p>The practice test needs JavaScript. You can still read the <a href="{AU}">practice questions with answers</a>.</p></noscript>
</div>
<p>Want to study first? Read the practice questions by part: {', '.join(f'<a href="{au_part_path(n, t)}">Part {n}</a>' for n, (t, _) in ordered)}.</p>
{cta('autest', 'Practise on your phone', 'The Oathly app has full practice tests, study cards by part and an estimate of your chance of passing today.')}"""
    site.write(path, title, desc, body, [("Australian citizenship test", AU), ("Practice test", path)],
               lang="en-AU", scripts='<script src="/assets/quiz.js" defer></script>\n', footer_extra=attribution)


# --------------------------------------------------------------------------- home, 404, sitemap

def build_home(site: Site, data: dict) -> None:
    au_total = len(data["au"]["questions"])
    title = "Oathly: Citizenship Test Practice for the U.S. and Australia"
    desc = ("Practice for your citizenship test with every civics question and answer, a realistic mock interview, "
            "and answers that stay up to date after every election. Free Android app and free online practice.")
    body = f"""<h1>Pass your citizenship test with confidence</h1>
<p class="lead">Oathly is a free Android app for the U.S. civics test and the Australian citizenship test. It asks the questions aloud like the real interview, checks your answers, and keeps the answers that change, such as your senators and governor, correct every day.</p>
<div class="btns"><a class="btn" href="{play_link('home')}" rel="noopener">Get Oathly on Google Play</a><a class="btn alt" href="{US}practice-test/">Practice online now</a></div>

<h2>U.S. citizenship test</h2>
<ul class="grid">
<li><a href="{US}"><strong>How the civics test works</strong><span>Which version you take, how many questions, how to pass.</span></a></li>
<li><a href="{US}2025-civics-questions/"><strong>128 questions and answers</strong><span>The complete 2025 civics test with explanations.</span></a></li>
<li><a href="{US}2008-civics-questions/"><strong>100 questions and answers</strong><span>The 2008 civics test, for N-400 filed before October 20, 2025.</span></a></li>
<li><a href="{US}practice-test/"><strong>Free practice interview</strong><span>Random questions with the real stopping rules.</span></a></li>
<li><a href="{US}states/"><strong>Answers for your state</strong><span>Senators, representatives, governor and capital.</span></a></li>
<li><a href="{US}65-20-questions/"><strong>65/20 questions</strong><span>The 20 questions for older long-term residents.</span></a></li>
</ul>

<h2>Australian citizenship test</h2>
<ul class="grid">
<li><a href="{AU}"><strong>Practice questions</strong><span>{au_total} questions written from Our Common Bond.</span></a></li>
<li><a href="{AU}practice-test/"><strong>Free practice test</strong><span>20 questions with the real pass rule, including values.</span></a></li>
</ul>

<h2>Why people study with Oathly</h2>
<ul>
<li><strong>It works like the real interview.</strong> Hear each question, say or type your answer, and the mock interview stops the moment you pass or fail.</li>
<li><strong>Your answers are always current.</strong> Choose your state and Oathly shows your own senators, representative, governor and capital, checked every day.</li>
<li><strong>It finds your weak spots.</strong> The questions you miss come back until you know them, and you see your chance of passing today.</li>
<li><strong>It respects you.</strong> No account, works offline, and your progress stays on your phone. Every study feature is free.</li>
</ul>
{cta('homebottom', 'Start practicing today', 'A few minutes a day is enough. Oathly keeps your streak, reminds you to study and tells you when you are ready.')}"""
    ld = [
        {"@context": "https://schema.org", "@type": "WebSite", "name": "Oathly", "url": SITE + "/"},
        {"@context": "https://schema.org", "@type": "MobileApplication", "name": "Oathly: US Citizenship Test",
         "operatingSystem": "Android", "applicationCategory": "EducationalApplication",
         "description": desc, "url": SITE + "/", "installUrl": PLAY,
         "offers": {"@type": "Offer", "price": "0", "priceCurrency": "USD"}},
    ]
    site.write("/", title, desc, body, [], ld)

    site.write("/404.html", "Page not found | Oathly", "This page does not exist.",
               f"""<h1>Page not found</h1>
<p class="lead">That page does not exist. Try one of these instead:</p>
<ul>
<li><a href="{US}2025-civics-questions/">128 civics questions and answers (2025 test)</a></li>
<li><a href="{US}practice-test/">Free U.S. citizenship practice test</a></li>
<li><a href="{US}states/">Citizenship test answers for your state</a></li>
<li><a href="{AU}">Australian citizenship test practice</a></li>
</ul>""", [], index=False)


def build_meta(site: Site) -> None:
    urls = [("/privacy", None)] + site.pages
    rows = []
    for path, lastmod in sorted(urls, key=lambda x: (x[0].count("/"), x[0])):
        mod = f"<lastmod>{lastmod}</lastmod>" if lastmod else ""
        rows.append(f"<url><loc>{SITE}{path}</loc>{mod}</url>")
    (OUT / "sitemap.xml").write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n'
        + "\n".join(rows) + "\n</urlset>\n", encoding="utf-8")
    (OUT / "robots.txt").write_text(
        f"User-agent: *\nAllow: /\n\nSitemap: {SITE}/sitemap.xml\n", encoding="utf-8")


def main() -> None:
    global OUT
    data = load()
    OUT = Path(tempfile.mkdtemp(prefix="oathly-site-"))
    try:
        assets = OUT / "assets"
        assets.mkdir(parents=True)
        for f in ("site.css", "quiz.js", "favicon.svg"):
            shutil.copyfile(STATIC / f, assets / f)
        shutil.copyfile(ROOT / "store" / "graphics" / "feature-graphic-1024x500.png", assets / "og.png")
        shutil.copyfile(ROOT / "store" / "graphics" / "icon-512.png", assets / "icon.png")

        site = Site()
        build_home(site, data)
        build_us(site, data)
        build_au(site, data)
        build_meta(site)

        for name in GENERATED:
            target = PUBLIC / name
            if target.is_dir():
                shutil.rmtree(target)
            elif target.exists():
                target.unlink()
            shutil.move(str(OUT / name), str(target))
    finally:
        shutil.rmtree(OUT, ignore_errors=True)
    print(f"wrote {len(site.pages)} pages to public/ for {SITE}")


if __name__ == "__main__":
    main()
