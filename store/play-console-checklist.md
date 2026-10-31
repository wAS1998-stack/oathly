# Play Console checklist for Oathly

Everything below is a one-time setup. After launch, answer updates run by themselves (see README).

## 1. Before you start
- [ ] **Support email.** Create one (for example a new Gmail just for Oathly) and put it in `public/privacy.html` in place of `SUPPORT_EMAIL`.
- [ ] **AdMob.** Create the Android app in AdMob with two ad units: *Banner* (anchored adaptive) and *Interstitial*. Note the App ID and both unit IDs.
- [ ] **app-ads.txt.** Replace `pub-0000000000000000` in `public/app-ads.txt` with your publisher ID.
- [ ] **Cloudflare Pages.** Create a Pages project named `oathly`, connect this GitHub repo, framework preset *None*, build command empty, output directory `public`. The site must answer at `https://oathly.pages.dev` (if that name is taken, use what Cloudflare gives you and set `OATHLY_DATA_BASE_URL` as a repository variable/secret; see README).
- [ ] **Upload key.** Create it once (`keytool -genkeypair -v -keystore upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`), keep a backup, and add the GitHub secrets listed in `.github/workflows/release.yml`.

## 2. Create the app
- App name: **Oathly: US Citizenship Test**
- Default language: English (United States)
- App or game: App · Free · Category: **Education**
- Package name: `com.oathly.app`

## 3. App content (Policy → App content)
| Section | Answer |
|---|---|
| Privacy policy | `https://oathly.pages.dev/privacy` |
| App access | All functionality is available without special access |
| Ads | **Yes**, the app contains ads |
| Content rating | Reference/education questionnaire; no violence, no user content, no gambling → expect "Everyone" / "3+" |
| Target audience | **18 and over** only (keeps the app outside the Families program; citizenship applicants are adults) |
| News app | No |
| Government apps | **No**, not developed by or for a government |
| Financial features | None |
| Health | None |
| Data safety | See section 4 |

## 4. Data safety form
Oathly itself sends no personal data anywhere. The Google Mobile Ads SDK does. Check Google's current guidance before submitting, as it changes: https://developers.google.com/admob/android/privacy/play-data-disclosure

Answers that match the SDKs used:
- Does the app collect or share user data? **Yes**
- Is all data encrypted in transit? **Yes**
- Can users request deletion? Choose the option that fits: progress is on-device only (users delete it via Settings → Reset progress or by uninstalling); ad data is controlled through Google (reset advertising ID).
- Data types (collected **and** shared with Google for ads):
  - **Location → Approximate location** (from IP address): Advertising or marketing; Fraud prevention, security and compliance
  - **App activity → App interactions**: Advertising or marketing; Analytics
  - **App info and performance → Crash logs, Diagnostics**: Analytics; Fraud prevention
  - **Device or other IDs** (advertising ID, app set ID): Advertising or marketing; Analytics; Fraud prevention
- Purchase history: payments are processed by Google Play Billing; follow Google's guidance for the Play Billing Library when you fill this in.
- Not collected: name, email, phone, precise location, contacts, photos, audio (speech is converted to text by the phone's own service and never leaves the app), files, health, financial info.

## 5. Store listings
- Default listing (United States): text in `store/listing-us.md`
- Custom store listing (target country **Australia**): text in `store/listing-au.md`
- Graphics: `store/graphics/icon-512.png`, `store/graphics/feature-graphic-1024x500.png`
- Phone screenshots (2 to 8): install the debug APK from the latest *Android build* run (Actions → artifact `oathly-debug-apk`) and capture: Home, Mock interview, Answers that change, Results, Study card. Use the same set for Australia after switching the test in Settings.
- Contact details: support email, website `https://oathly.pages.dev`

## 6. Monetisation
- In-app product: Monetize → Products → In-app products → **`oathly_pro`** (one-time), name "Oathly Pro: remove ads", description "Removes all ads. Every study feature stays free." Suggested price: US$4.99 / A$7.99.
- AdMob: link the Play app to the AdMob app once it is published.

## 7. Countries
Production → Countries / regions: **United States** and **Australia** only. (Add the UK, Canada and New Zealand later when their content ships.)

## 8. Release
1. Actions → **Release build** → Run workflow. Download `oathly-release-aab`.
2. If your developer account is a **personal account created after 13 Nov 2023**: first create a **Closed testing** track, add at least 12 testers (friends' Gmail addresses or a Google Group), and keep them opted in for 14 days. Then apply for production access. Organization accounts skip this.
3. Upload the AAB to Production (or closed testing first), add release notes, and submit for review.

## 9. Compliance rules to keep forever
- Never use "Official", "USCIS", "Home Affairs", seals, flags styled as government marks, or government logos in the name, icon or screenshots.
- Keep the "not affiliated" disclaimer and official source links in both descriptions and in the app (About & sources).
- Ads only on menu screens and after a finished test; never on a question, never at app launch.
- Do not add features that give immigration legal advice (filing, eligibility, fees). Test practice only.
