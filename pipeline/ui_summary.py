#!/usr/bin/env python3
"""Report the emulator walkthrough result as GitHub annotations."""
from pathlib import Path


def esc(s: str) -> str:
    return s.replace("%", "%25").replace("\r", "").replace("\n", "%0A")


out = Path("instrument.txt").read_text(errors="replace") if Path("instrument.txt").exists() else ""
shots = sorted(p.name for p in Path("screenshots").glob("*.png")) if Path("screenshots").exists() else []
print(f"::notice title=Screenshots::{len(shots)} captured: {', '.join(shots)}")
if "OK (" in out:
    print("::notice title=UI walkthrough::passed with no crash")
else:
    lines = [l for l in out.splitlines() if l.strip() and not l.startswith("INSTRUMENTATION_STATUS")]
    tail = "\n".join(lines[-60:])
    print(f"::error title=UI walkthrough failed::{esc(tail[-6000:])}")
crash = Path("crash.txt").read_text(errors="replace") if Path("crash.txt").exists() else ""
if "FATAL EXCEPTION" in crash:
    print(f"::error title=App crash::{esc(crash[:6000])}")
