# Oathly

Android app (Kotlin, Jetpack Compose) for practising citizenship tests in tier-1 countries.
Launch markets: **United States** (USCIS civics test, 2025 and 2008 versions) and **Australia** (citizenship test).

## How it runs by itself

| Job | Where | How often |
|---|---|---|
| Refresh officeholders (President, VP, Speaker, Chief Justice, all senators and representatives, governors) | `.github/workflows/update-data.yml` → `pipeline/build_officials.py` | Daily |
| Publish the data feed (`public/v1/officials.json`, privacy page, app-ads.txt) | Cloudflare Pages, deploys `public/` on every push | Automatic |
| Watch USCIS and Home Affairs pages; open a GitHub issue (you get an email) if a test page changes or an officeholder doesn't match USCIS | `pipeline/watch_sources.py` | Daily |
| App downloads the feed and notifies users whose answers changed | `UpdateWorker` in the app | Daily, battery friendly |
| Build, test and lint every commit; downloadable debug APK | `.github/workflows/android.yml` | Every push |
| Signed release bundle for Google Play | `.github/workflows/release.yml` | When you run it |

Bad upstream data can't reach users: the pipeline validates every section and keeps the last good value, and the app refuses any download that fails its own checks.

The only manual moment: if USCIS or Home Affairs **changes the test itself**, the watcher opens an issue and the content files need an update.

## Content and sources
- **US questions**: USCIS civics questions (public domain, 17 U.S.C. §105). Source banks in `content/us/source/` were transcribed by the open-source civics-test-kit project (MIT), then cross-checked for Oathly against a second independent transcription and the official M-1778 (09/25) PDF: all 128 question texts, all 20 starred 65/20 questions and all answers matched.
- **Australian questions**: original practice questions written from *Australian Citizenship: Our Common Bond* (© Commonwealth of Australia, Creative Commons Attribution; see `content/au/NOTES.md`).
- **Officeholders**: unitedstates/congress-legislators (public domain) and Wikidata (CC0).

After editing anything in `content/`, run `python3 pipeline/build_content.py` and commit the generated files in `app/src/main/assets/content/` (CI checks this).

## Project layout
```
app/                      Android app (single module)
  src/main/java/com/oathly/app/
    data/                 models, content + officials loading, progress, settings
    domain/               answer checking, test rules, spaced repetition, readiness
    platform/             text-to-speech, notifications, background workers
    ads/ billing/         AdMob + consent, Play Billing (Oathly Pro)
    ui/                   Compose screens
  src/test/               unit tests (answers, test rules, content files)
content/                  question sources
pipeline/                 Python scripts run by GitHub Actions
public/                   Cloudflare Pages site: data feed, privacy policy, app-ads.txt
store/                    Play listings, graphics, Play Console checklist
```

## Build locally
Open the folder in Android Studio, or run `./gradlew assembleDebug`. Debug builds always use Google's test ads.

## Secrets and settings (GitHub → Settings → Secrets and variables → Actions)
- `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`: upload key for release builds
- `OATHLY_ADMOB_APP_ID`, `OATHLY_ADMOB_BANNER_ID`, `OATHLY_ADMOB_INTERSTITIAL_ID`: real AdMob IDs (release only)
- Optional `OATHLY_DATA_BASE_URL` if the Cloudflare site isn't `https://oathly.pages.dev/v1/`

See `store/play-console-checklist.md` for the full launch checklist.
