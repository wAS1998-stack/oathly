# Australian question bank: sources and notes

Bank: `content/au/questions.json` has 213 original questions (Part 1: 51, Part 2: 42, Part 3: 70, Part 4: 50). `values` is `true` for every Part 4 question and only those. Rules are in `content/au/test_rules.json`. Everything was checked on 31 October 2026.

## Sources used

| What | URL |
|---|---|
| Booklet, testable section (the only fact source) | https://immi.homeaffairs.gov.au/citizenship-subsite/files/our-common-bond-testable.pdf |
| Booklet landing page (links the PDF; says the test is "conducted in English only") | https://immi.homeaffairs.gov.au/citizenship/test-and-interview/our-common-bond |
| Test rules (20 questions, 5/5 values, 15/20 = 75%, 45 minutes) | https://immi.homeaffairs.gov.au/citizenship/test-and-interview/learn-about-citizenship-interview-and-test/learn-about-citizenship-test |
| Official practice test start page (repeats the same rules) | https://citizenshippracticetest.homeaffairs.gov.au/ |
| Prepare-for-test page ("All test questions are based on the OCB resource booklet") | https://immi.homeaffairs.gov.au/citizenship/test-and-interview/prepare-for-test/practice-test-new |
| Home Affairs website copyright page (checked only for attribution wording; it covers the website, not the booklet) | https://www.homeaffairs.gov.au/access-and-accountability/using-our-website/copyright-and-disclaimer |

The booklet was read from the official immi.homeaffairs.gov.au PDF itself, not a mirror. The sandbox shell could not download it (the egress proxy blocks that host), so the text was read through the web-fetch tool, which returns extracts and short verbatim quotes. Every number, date, name, colour and symbol used in the bank was checked against a verbatim quote from the PDF.

The booklet has no practice questions of its own (checked), so nothing could be copied from it. The official online practice test is behind an app shell, and none of its questions were seen or used.

## Edition

- Title: *Australian Citizenship: Our Common Bond*, testable section (Parts 1 to 4 plus the glossary).
- Publisher: Australian Government Department of Home Affairs.
- The only date printed is "© Commonwealth of Australia 2020". There is no edition number, ISBN, print date or revision date. The PDF's metadata could not be read.
- The current PDF is a revised printing even though it still says 2020: it names the Head of State as "the King of Australia, His Majesty King Charles III" and uses "Türkiye" for Gallipoli. Both changes date from after 2022. Re-check the PDF if Home Affairs puts out a new version (`pipeline/watch_sources.py` already watches the landing page).

## Licence (from the booklet's copyright page)

Verbatim text, joined from consecutive quotes:

> © Commonwealth of Australia 2020
>
> With the exception of the Commonwealth Coat of Arms, all material presented in this publication is provided under a Creative Commons Attribution 4.0 International license at https://creativecommons.org/licenses/by/4.0/legalcode.
>
> This means this license only applies to material as set out in this document.
>
> The details of the relevant license conditions are available on the Creative Commons website at https://creativecommons.org/ as is the full legal code for the CC BY 4.0 license at https://creativecommons.org/licenses/by/4.0/legalcode .
>
> Use of the Coat of Arms
>
> The terms under which the Coat of Arms can be used are detailed at the Department of Prime Minister and Cabinet website—https://www.pmc.gov.au/government/commonwealth-coat-arms.
>
> Contact us: Enquiries regarding the licence and any use of this document are welcome at: Portfolio Media and Engagement Branch, Department of Home Affairs, PO Box 25, BELCONNEN ACT 2616

**Attribution wording: the booklet does not specify any.** The copyright page has no "please attribute as" sentence. So the `attribution` string in `questions.json` was written by Oathly to meet CC BY 4.0 section 3(a): it names the creator and the copyright notice, links the licence, and says the material was adapted. It also says Oathly is not endorsed, which the licence requires (no implied endorsement). For reference, the Home Affairs *website* copyright page asks for website material to be attributed "as Australian Government Department of Home Affairs" under CC BY 3.0 AU. That page covers the website, not this booklet.

The attribution sentence used in the app:

> Oathly's practice questions are original and are based on ‘Australian Citizenship: Our Common Bond’ by the Australian Government Department of Home Affairs (© Commonwealth of Australia 2020), used under a Creative Commons Attribution 4.0 International licence (https://creativecommons.org/licenses/by/4.0/); the material has been adapted by Oathly, and Oathly is not affiliated with or endorsed by the Australian Government.

Other licence points:

- Do not reproduce the Commonwealth Coat of Arms image in the app or the store listing. It is excluded from the CC licence and has its own use terms. The questions only describe it in words.
- Do not use any Home Affairs or Australian Government logo or branding.

## Test rules as officially stated

- "answer 20 multiple choice questions"
- "answer 5/5 (100%) of the Australian values questions correctly"
- "achieve an overall mark of at least 15/20 (75%)"
- "You will have 45 minutes to complete the test."
- "The test is conducted in English only."
- The booklet (page 3) says: "as of 15 November 2020, it will also include five questions on Australian values" and "you must answer all five of the values questions correctly, with a mark of at least 75 per cent overall."

Your guesses (20 questions, 75%) were correct. The time limit is 45 minutes.

## Supporting booklet sentence for each question

Each line gives the question id, the booklet heading, the correct answer, and a paraphrase (under 15 words) of the booklet sentence that supports it.


### Part 1: Australia and its people

- **au-001** (Aboriginal and Torres Strait Islander peoples). Answer: Aboriginal and Torres Strait Islander peoples. Booklet: Australia's first inhabitants are Aboriginal and Torres Strait Islander peoples.
- **au-002** (Aboriginal and Torres Strait Islander peoples). Answer: They have the oldest continuous cultures and traditions in the world. Booklet: They have the oldest continuous cultures and traditions in the world.
- **au-003** (Aboriginal and Torres Strait Islander peoples). Answer: Between 65,000 and 40,000 years ago. Booklet: Archaeological record: Aboriginal peoples arrived 65,000 to 40,000 years ago.
- **au-004** (Aboriginal and Torres Strait Islander peoples). Answer: Islands between the northern tip of Queensland and Papua New Guinea. Booklet: Torres Strait Islanders are from islands between northern Queensland and Papua New Guinea.
- **au-005** (Early days of European settlement). Answer: 11. Booklet: European settlement began with the first 11 convict ships, the First Fleet.
- **au-006** (Early days of European settlement). Answer: 26 January 1788. Booklet: The First Fleet arrived from Great Britain on 26 January 1788.
- **au-007** (Early days of European settlement). Answer: British jails could not hold the large number of prisoners. Booklet: British laws were harsh and jails could not hold all the prisoners.
- **au-008** (Early days of European settlement). Answer: Captain Arthur Phillip. Booklet: The first Governor of New South Wales was Captain Arthur Phillip.
- **au-009** (Early days of European settlement). Answer: Great Britain and Ireland. Booklet: Early free settlers came from Great Britain and Ireland.
- **au-010** (Early days of European settlement). Answer: 1851. Booklet: In 1851 a gold rush began in New South Wales and Victoria.
- **au-011** (Early days of European settlement). Answer: Chinese people. Booklet: Chinese gold rush arrivals were the first large non-European migrant group.
- **au-012** (Early days of European settlement). Answer: It more than doubled. Booklet: In 10 years, Australia's population more than doubled.
- **au-013** (The nation of Australia). Answer: The Commonwealth of Australia. Booklet: In 1901 the colonies united as the Commonwealth of Australia.
- **au-014** (The nation of Australia). Answer: The 1967 Referendum. Booklet: Indigenous peoples were included in population estimates only after the 1967 Referendum.
- **au-015** (The nation of Australia). Answer: People from Europe who had to leave their homelands. Booklet: Non-British migration followed World War II as millions left Europe.
- **au-016** (The nation of Australia). Answer: More than 100. Booklet: There are more than 100 distinct Aboriginal and Torres Strait Islander languages.
- **au-017** (Australia's states and territories). Answer: Six states and two mainland territories. Booklet: There are six states and two mainland territories.
- **au-018** (Australia's states and territories). Answer: Canberra. Booklet: Canberra is Australia's capital city.
- **au-019** (The states). Answer: Perth. Booklet: Western Australia's capital is Perth.
- **au-020** (The states). Answer: Queensland. Booklet: Queensland's capital is Brisbane.
- **au-021** (The states). Answer: New South Wales. Booklet: New South Wales was the first colony established by the British.
- **au-022** (The states). Answer: Western Australia. Booklet: Western Australia is the largest state.
- **au-023** (The states). Answer: Tasmania. Booklet: Tasmania is the smallest state.
- **au-024** (The states). Answer: Bass Strait. Booklet: Tasmania is separated from the mainland by Bass Strait.
- **au-025** (The states). Answer: Queensland. Booklet: The Great Barrier Reef runs along Queensland's eastern coast.
- **au-026** (The territories). Answer: The Northern Territory. Booklet: Northern Territory icons include Uluru, Kata Tjuta and Kings Canyon.
- **au-027** (The states). Answer: Tasmania. Booklet: Tasmanian icons include Cradle Mountain, Port Arthur and the Bay of Fires.
- **au-028** (The territories). Answer: Canberra, in the Australian Capital Territory. Booklet: The ACT contains Canberra, home of Parliament House and the High Court.
- **au-029** (Welcome to Country and Acknowledgement of Country protocols). Answer: An Aboriginal or Torres Strait Islander custodian of the local region. Booklet: A local Aboriginal or Torres Strait Islander custodian performs a Welcome to Country.
- **au-030** (Welcome to Country and Acknowledgement of Country protocols). Answer: Anyone. Booklet: Anyone can deliver an Acknowledgement of Country.
- **au-031** (Australia Day). Answer: 26 January. Booklet: Australia Day is celebrated on 26 January each year.
- **au-032** (Australia Day). Answer: The arrival of the First Fleet from Great Britain in 1788. Booklet: Australia Day is the anniversary of the First Fleet's 1788 arrival.
- **au-033** (Australia Day). Answer: The Prime Minister. Booklet: The Prime Minister announces the Australian of the Year Awards in Canberra.
- **au-034** (Anzac Day). Answer: 25 April. Booklet: Anzac Day is commemorated on 25 April each year.
- **au-035** (Anzac Day). Answer: Gallipoli, in Türkiye. Booklet: The Anzacs landed at Gallipoli in Türkiye on 25 April 1915.
- **au-036** (Anzac Day). Answer: All Australians who served and died in wars, conflicts and peacekeeping operations. Booklet: Anzac Day remembers Australians who served and died in wars and conflicts.
- **au-037** (The Australian National Flag). Answer: Blue, white and red. Booklet: The Australian National Flag is blue, white and red.
- **au-038** (The Australian National Flag). Answer: Seven. Booklet: The Commonwealth Star has seven points: six states plus the territories.
- **au-039** (The Australian National Flag). Answer: Australia's history of British settlement. Booklet: The Union Jack represents British settlement and inherited laws and institutions.
- **au-040** (The Australian Aboriginal Flag). Answer: The Australian Aboriginal Flag. Booklet: The Australian Aboriginal Flag is black, red and yellow.
- **au-041** (The Australian Aboriginal Flag). Answer: The sun. Booklet: On the Aboriginal Flag, the yellow circle represents the sun.
- **au-042** (The Torres Strait Islander Flag). Answer: The sea. Booklet: On the Torres Strait Islander Flag, the blue panel represents the sea.
- **au-043** (The Torres Strait Islander Flag). Answer: The white dancer's headdress. Booklet: The white dancer's headdress symbolises all Torres Strait Islanders.
- **au-044** (Commonwealth Coat of Arms). Answer: A kangaroo and an emu. Booklet: A kangaroo and an emu support the shield, one each side.
- **au-045** (Commonwealth Coat of Arms). Answer: The authority and property of the Commonwealth of Australia. Booklet: The Coat of Arms identifies Commonwealth authority and property.
- **au-046** (Commonwealth Coat of Arms). Answer: The six states and federation. Booklet: The shield represents the six states and federation.
- **au-047** (Australia's national flower). Answer: The golden wattle. Booklet: Australia's national flower is the golden wattle.
- **au-048** (Australia's national flower). Answer: Spring. Booklet: The golden wattle has golden yellow flowers in spring.
- **au-049** (Australia's national colours). Answer: Green and gold. Booklet: National colours are green and gold, the colours of the golden wattle.
- **au-050** (Australia's national gemstone). Answer: The opal. Booklet: The opal is Australia's national gemstone.
- **au-051** (Australia's national anthem). Answer: Advance Australia Fair. Booklet: Advance Australia Fair is Australia's national anthem.

### Part 2: Australia's democratic beliefs, rights and liberties

- **au-052** (Parliamentary democracy). Answer: A parliamentary democracy. Booklet: Australia's system of government is a parliamentary democracy.
- **au-053** (Parliamentary democracy). Answer: The Australian people. Booklet: Government power comes from the people, who vote for representatives.
- **au-054** (Parliamentary democracy). Answer: Through elections. Booklet: Representatives answer to the people, through elections, for their decisions.
- **au-055** (The Rule of Law). Answer: All Australians are equal under the law. Booklet: All Australians are equal under the law.
- **au-056** (The Rule of Law). Answer: Everyone, including leaders and the police. Booklet: No person, group or religious rule is above the law.
- **au-057** (Living peacefully). Answer: Through discussion, peaceful persuasion and the democratic process. Booklet: Change should occur through discussion, peaceful persuasion and the democratic process.
- **au-058** (Respect for all individuals regardless of background). Answer: With dignity and respect. Booklet: Australians are expected to treat each other with dignity and respect.
- **au-059** (Freedom of speech and freedom of expression). Answer: Freedom of speech. Booklet: Freedom of speech means people can say and write what they think.
- **au-060** (Freedom of speech and freedom of expression). Answer: Criticise the government and protest peacefully. Booklet: People can criticise government and protest peacefully, while obeying the law.
- **au-061** (Freedom of speech and freedom of expression). Answer: Freedom of expression. Booklet: Freedom of expression includes art, film, music and literature.
- **au-062** (Freedom of association). Answer: The right to form and join groups to pursue common goals. Booklet: Freedom of association is forming and joining associations for common goals.
- **au-063** (Freedom of association). Answer: No, people can choose not to join. Booklet: People can decide not to join and cannot be forced.
- **au-064** (Freedom of association). Answer: Peaceful, with no one hurt and no property damaged. Booklet: Protest gatherings must be peaceful, not injure people or damage property.
- **au-065** (Freedom of religion). Answer: A Judaeo-Christian heritage. Booklet: Australia has a Judaeo-Christian heritage.
- **au-066** (Freedom of religion). Answer: Good Friday. Booklet: Public holidays include Christian days like Good Friday, Easter Sunday, Christmas Day.
- **au-067** (Freedom of religion). Answer: It operates separately from churches and other religious groups. Booklet: The government is secular: it operates separately from religious entities.
- **au-068** (Freedom of religion). Answer: No, people can follow any religion or none. Booklet: Australia has no official national religion.
- **au-069** (Freedom of religion). Answer: The Australian law. Booklet: Where law and religious practice conflict, Australian law prevails.
- **au-070** (Gender equality). Answer: Equal rights. Booklet: Men and women have equal rights in Australia.
- **au-071** (Equality of opportunity and a 'fair go'). Answer: That there are no formal class distinctions. Booklet: Fair go ensures no formal class distinctions in Australian society.
- **au-072** (Responsibilities—what you will give Australia). Answer: Serving on a jury if called to do so. Booklet: Responsibilities include serving on a jury if called.
- **au-073** (Privileges—what Australia will give you). Answer: Applying for an Australian passport. Booklet: Privileges include applying for an Australian passport and re-entering freely.
- **au-074** (Responsibilities and privileges of Australian citizenship). Answer: Voting in elections and referendums. Booklet: Voting appears in both the responsibilities and privileges lists.
- **au-075** (Voting in federal and state or territory elections, and in a referendum). Answer: 18 years or over. Booklet: Voting is a responsibility for citizens aged 18 or over.
- **au-076** (Voting in federal and state or territory elections, and in a referendum). Answer: A vote to change the Australian Constitution. Booklet: A referendum is a vote to change the Australian Constitution.
- **au-077** (Voting in federal and state or territory elections, and in a referendum). Answer: Federal and state or territory elections. Booklet: Voting is compulsory in federal and state or territory elections.
- **au-078** (Voting in federal and state or territory elections, and in a referendum). Answer: Local government elections. Booklet: Local government voting may not be compulsory in some states.
- **au-079** (Defend Australia should the need arise). Answer: No, it is voluntary. Booklet: Service in the Australian Defence Force is voluntary.
- **au-080** (Serve on a jury if called to do so). Answer: Hears the evidence and decides if a person is guilty. Booklet: A jury hears evidence and decides guilty or not guilty.
- **au-081** (Serve on a jury if called to do so). Answer: Citizens who are on the electoral roll. Booklet: Citizens on the electoral roll can be called for jury service.
- **au-082** (Serve on a jury if called to do so). Answer: That the court system is open and fair. Booklet: Jury service helps ensure the court system is open and fair.
- **au-083** (Apply for work in the Australian Public Service and the Australian Defence Force). Answer: Working at the Australian Taxation Office. Booklet: Public Service jobs include Services Australia and the Australian Taxation Office.
- **au-084** (Apply for work in the Australian Public Service and the Australian Defence Force). Answer: The Army, Navy and Air Force. Booklet: The Australian Defence Force is the Army, Navy and Air Force.
- **au-085** (Seek election to parliament). Answer: Those aged 18 or over who are not dual citizens. Booklet: Citizens 18 or over who aren't dual citizens can seek election.
- **au-086** (Apply for an Australian passport and re-enter Australia freely). Answer: No, citizens can return without a visa. Booklet: Citizens travelling overseas can return without needing a visa.
- **au-087** (Ask for consular assistance from an Australian official while overseas). Answer: The laws of the country they are in. Booklet: When in another country, you must obey that country's laws.
- **au-088** (Ask for consular assistance from an Australian official while overseas). Answer: By issuing an emergency passport. Booklet: Officials can help citizens overseas with an emergency passport.
- **au-089** (Apply for children born overseas to become Australian citizens by descent). Answer: For the child to become a citizen by descent. Booklet: Citizens can apply for overseas-born children to become citizens by descent.
- **au-090** (Participating in Australian society). Answer: The Australian Taxation Office (ATO). Booklet: Tax is paid from earnings and collected by the ATO.
- **au-091** (Participating in Australian society). Answer: Services such as healthcare, education and defence. Booklet: Taxes fund healthcare, education, defence, roads, railways and social security.
- **au-092** (Freedom of speech and freedom of expression). Answer: It underpins Australia's democratic system. Booklet: Freedom of speech is a core value underpinning our democracy.
- **au-093** (Obey the laws of Australia). Answer: To keep society orderly, free and safe and protect rights. Booklet: Laws maintain an orderly, free and safe society and protect rights.

### Part 3: Government and the law in Australia

- **au-094** (Voting). Answer: Enrol to vote. Booklet: Citizens 18 or over must enrol; unenrolled people cannot vote.
- **au-095** (Voting). Answer: The Australian Electoral Commission (AEC). Booklet: The AEC conducts federal elections and referendums and keeps the roll.
- **au-096** (Voting). Answer: It is independent of the government. Booklet: The AEC is independent; parties and government cannot influence it.
- **au-097** (Voting). Answer: You vote in private and are free to choose any candidate. Booklet: Secret ballot means you are free and safe to vote for anyone.
- **au-098** (Voting). Answer: You may have to pay a fine. Booklet: Not voting without a good reason may mean paying a fine.
- **au-099** (Raising matters with your representatives). Answer: By contacting their elected representative. Booklet: Citizens can contact their elected representative about policy concerns.
- **au-100** (Federation). Answer: As six separate, self-governing British colonies. Booklet: Before 1901, Australia was six separate, self-governing British colonies.
- **au-101** (Federation). Answer: Trade and transport between them was expensive and slow. Booklet: Trade and transport between colonies was expensive and slow.
- **au-102** (The Australian Constitution). Answer: 1 January 1901. Booklet: The Constitution came into effect on 1 January 1901.
- **au-103** (The Australian Constitution). Answer: The legal document setting out the basic rules of government. Booklet: The Constitution sets out the basic rules for Australia's government.
- **au-104** (The Australian Constitution). Answer: As part of a British Act of Parliament in 1900. Booklet: The Constitution was passed within a British Act of Parliament, 1900.
- **au-105** (The Australian Constitution). Answer: The House of Representatives and the Senate. Booklet: The Constitution established Parliament: House of Representatives and Senate.
- **au-106** (The Australian Constitution). Answer: The High Court of Australia. Booklet: The High Court has ultimate power to apply and interpret laws.
- **au-107** (The Australian Constitution). Answer: The Australian people, by voting in a referendum. Booklet: The people can change the Constitution by voting in a referendum.
- **au-108** (The Australian Constitution). Answer: More than 90 per cent. Booklet: In 1967, more than 90 per cent of Australians voted 'Yes'.
- **au-109** (The Australian Constitution). Answer: Aboriginal peoples to be counted in the Census. Booklet: The 1967 Referendum allowed Aboriginal peoples to be counted in the Census.
- **au-110** (The Australian Constitution). Answer: A 'double majority'. Booklet: A referendum needs a double majority: nationally and in most states.
- **au-111** (How is the power of government controlled?). Answer: To stop one person or group from holding all the power. Booklet: Power is divided to stop one person or group holding all power.
- **au-112** (Legislative power). Answer: The power to make and change laws. Booklet: Parliament has the power to make and change the laws.
- **au-113** (Executive power). Answer: Executive power. Booklet: Executive power is the power to put the laws into practice.
- **au-114** (Judicial power). Answer: Judges. Booklet: Judges have the power to interpret and apply the law.
- **au-115** (Who is Australia's Head of State?). Answer: The King of Australia. Booklet: Australia's Head of State is the King of Australia.
- **au-116** (Who is Australia's Head of State?). Answer: The King, on the advice of the Prime Minister. Booklet: The King appoints the Governor-General on the Prime Minister's advice.
- **au-117** (Constitutional monarchy). Answer: The King is Head of State but must act in line with the Constitution. Booklet: The King is Head of State but must follow the Constitution.
- **au-118** (The role of the Governor-General). Answer: The Governor-General must remain neutral. Booklet: The Governor-General is not part of government; must remain neutral.
- **au-119** (The role of the Governor-General). Answer: Royal Assent. Booklet: Signing Bills passed by Parliament into law is called Royal Assent.
- **au-120** (The role of the Governor-General). Answer: The Governor-General. Booklet: The Governor-General acts as Commander-in-Chief of the Defence Force.
- **au-121** (The role of the Governor-General). Answer: Starting the process for a federal election. Booklet: The Governor-General starts the process for a federal election.
- **au-122** (Who are some of Australia's leaders?). Answer: Premier. Booklet: Premier: the leader of a state government.
- **au-123** (Who are some of Australia's leaders?). Answer: Chief Minister. Booklet: Chief Minister: the leader of a territory government.
- **au-124** (Who are some of Australia's leaders?). Answer: The Governor. Booklet: Governor: the Head of State's representative in each state.
- **au-125** (Who are some of Australia's leaders?). Answer: A Mayor or Shire President. Booklet: Mayor or Shire President: the leader of a local government.
- **au-126** (The Australian Government). Answer: The Federal Government or the Commonwealth Government. Booklet: The Australian Government is also called Federal or Commonwealth Government.
- **au-127** (The House of Representatives). Answer: The People's House. Booklet: House of Representatives: also the Lower House or People's House.
- **au-128** (The House of Representatives). Answer: The people in their federal electorate. Booklet: Australia has federal electorates; each MP represents their electorate's people.
- **au-129** (The House of Representatives). Answer: By the number of people living there. Booklet: MP numbers per state and territory are based on population.
- **au-130** (The Senate). Answer: The Senate. Booklet: The Senate: Upper House, House of Review or States' House.
- **au-131** (The Senate). Answer: 76. Booklet: There is a total of 76 senators.
- **au-132** (The Senate). Answer: 12. Booklet: Each state elects 12 senators.
- **au-133** (The Senate). Answer: Two. Booklet: The ACT and the Northern Territory elect two senators each.
- **au-134** (State and territory government). Answer: The Governor-General. Booklet: In the Northern Territory, the Governor-General appoints an Administrator.
- **au-135** (State and territory government). Answer: States have rights recognised by the Constitution; territories do not. Booklet: States have constitutional rights; territories do not.
- **au-136** (State and territory government). Answer: Their own parliament and constitution. Booklet: Each state government has its own parliament and constitution.
- **au-137** (What do the three levels of government do?). Answer: Local government. Booklet: Local governments handle rubbish collection.
- **au-138** (What do the three levels of government do?). Answer: State and territory governments. Booklet: States and territories handle hospitals, health services and schools.
- **au-139** (What do the three levels of government do?). Answer: The Australian Government. Booklet: The Australian Government handles immigration and citizenship.
- **au-140** (What do the three levels of government do?). Answer: Defence. Booklet: Defence is federal; rubbish collection is local; public transport is state.
- **au-141** (What do the three levels of government do?). Answer: Local government. Booklet: Local governments handle building permits.
- **au-142** (What role do political parties play in the way Australia is governed?). Answer: A group of people who share ideas about how a country should be governed. Booklet: A political party shares ideas about how a country should be governed.
- **au-143** (What role do political parties play in the way Australia is governed?). Answer: Independents. Booklet: Parliamentarians without a party are called independents.
- **au-144** (How is the Australian Government formed?). Answer: The party or coalition with a majority in the House of Representatives. Booklet: Government is formed by the party or coalition with a House majority.
- **au-145** (How is the Australian Government formed?). Answer: The Opposition. Booklet: The second-largest party or coalition in the House forms the Opposition.
- **au-146** (How is the Australian Government formed?). Answer: The key decision-making body of government. Booklet: Ministers with key portfolios form Cabinet, the key decision-making body.
- **au-147** (How is the Australian Government formed?). Answer: The Governor-General. Booklet: The Governor-General approves appointing the Prime Minister and ministers.
- **au-148** (How are laws made?). Answer: A Bill. Booklet: A proposal for a new or changed law is called a Bill.
- **au-149** (How are laws made?). Answer: It goes to the Governor-General to be signed into law. Booklet: A Bill passed by both Houses goes to the Governor-General.
- **au-150** (The courts). Answer: Whether a person has broken the law and, if so, the penalty. Booklet: A court decides whether the law was broken and the penalty.
- **au-151** (The courts). Answer: Only the evidence before them. Booklet: Courts can only base their decision on the evidence.
- **au-152** (Judges and magistrates). Answer: No, judges are independent. Booklet: The government cannot remove judges for disagreeing with their decisions.
- **au-153** (Juries). Answer: As innocent. Booklet: People are considered innocent until found guilty in court.
- **au-154** (Juries). Answer: The judge. Booklet: If the jury finds a person guilty, the judge decides the penalty.
- **au-155** (The police). Answer: The police are independent of the government. Booklet: Police are independent of the government.
- **au-156** (The police). Answer: The Australian Federal Police (AFP). Booklet: The national police force is the Australian Federal Police (AFP).
- **au-157** (The police). Answer: Crimes against federal laws, such as drug trafficking. Booklet: The AFP investigates federal crimes, for example drug trafficking.
- **au-158** (The police). Answer: No, even offering a bribe is a crime. Booklet: It is a serious crime to bribe police, even to offer.
- **au-159** (Criminal offences in Australia). Answer: No, not knowing the law is no excuse. Booklet: Not knowing the law is no excuse.
- **au-160** (Criminal offences in Australia). Answer: It is against the law. Booklet: Domestic and family violence is against the law.
- **au-161** (Traffic offences). Answer: State and territory governments. Booklet: Road and traffic rules are controlled by state and territory governments.
- **au-162** (Traffic offences). Answer: Everybody in the car. Booklet: Everybody travelling in a car must wear a seatbelt.
- **au-163** (Traffic offences). Answer: Using a hand-held mobile device. Booklet: Using a hand-held mobile device while driving is against the law.

### Part 4: Australian values

- **au-164** (Australian values). Answer: Freedom, respect, fairness and equality of opportunity. Booklet: Values based on freedom, respect, fairness and equality of opportunity are central.
- **au-165** (Our values). Answer: Respect, share and uphold them. Booklet: All Australians are expected to respect, share and uphold these values.
- **au-166** (Commitment to the Rule of Law). Answer: At all times, even when no one is watching. Booklet: You should follow the law even if no one is watching.
- **au-167** (Commitment to the Rule of Law). Answer: Yes, everyone in Australia must follow Australian laws. Booklet: Regardless of background or culture, you must follow Australian laws.
- **au-168** (Commitment to the Rule of Law). Answer: They face penalties. Booklet: Everybody should obey the law at all times, or face penalties.
- **au-169** (Parliamentary democracy). Answer: Parliaments elected by the people. Booklet: Our laws are determined by parliaments elected by the people.
- **au-170** (Freedom of speech). Answer: Freely, as long as it is within the law. Booklet: People should be able to express ideas freely, within the law.
- **au-171** (Freedom of speech). Answer: They have the same freedom of speech as people. Booklet: Newspapers, television and radio outlets have the same freedom.
- **au-172** (Freedom of speech). Answer: No, it is against Australian values and law. Booklet: Promoting violence against people or groups is against Australian values and law.
- **au-173** (Freedom of speech). Answer: Making false allegations. Booklet: It is illegal to make false allegations or encourage law-breaking.
- **au-174** (Freedom of speech). Answer: It is an essential part of democracy. Booklet: Tolerance of peaceful public protest is an essential part of democracy.
- **au-175** (Freedom of association). Answer: No, people are free to join or leave groups voluntarily. Booklet: No one can be compelled to join an organisation or forced to leave.
- **au-176** (Freedom of religion). Answer: They have no legal status. Booklet: Religious laws have no legal status in Australia.
- **au-177** (Freedom of religion). Answer: It is against the law in Australia. Booklet: Polygamy is against the law; penalties can include imprisonment.
- **au-178** (Freedom of religion). Answer: It is against the law and can lead to imprisonment. Booklet: Forced marriage is against the law and can result in imprisonment.
- **au-179** (Freedom of religion). Answer: It is not acceptable. Booklet: Religious intolerance is not acceptable in Australian society.
- **au-180** (Equality of all people under the law). Answer: They receive the same treatment. Booklet: Buddhists and people of all faiths receive the same treatment as Christians.
- **au-181** (Equality of all people under the law). Answer: Two people, including two men or two women. Booklet: Two people can marry, including two men or two women.
- **au-182** (Equality of all people under the law). Answer: Yes, women and men have equal rights. Booklet: Men and women can vote, stand for parliament, join ADF and police.
- **au-183** (Equality of all people under the law). Answer: Yes, if she has better qualifications and skills. Booklet: A woman may get a job ahead of a man with better qualifications.
- **au-184** (Equality of all people under the law). Answer: The person themselves. Booklet: Everyone makes independent choices about personal matters like marriage and religion.
- **au-185** (Equality of all people under the law). Answer: It is never acceptable and is a criminal offence. Booklet: Physical violence against a spouse or partner is a criminal offence.
- **au-186** (Equality of all people under the law). Answer: Either a husband or a wife. Booklet: Either husband or wife may apply for divorce, even if the other objects.
- **au-187** (Equality of opportunity and a 'fair go'). Answer: Their skills and experience. Booklet: Jobs should be based on skills and experience, not gender, wealth, ethnicity.
- **au-188** (Equality of opportunity and a 'fair go'). Answer: Their hard work and talents. Booklet: Achievements in life should result from hard work and talents.
- **au-189** (Equality of opportunity and a 'fair go'). Answer: Through hard work and enterprise. Booklet: Many new migrants became leaders through hard work and enterprise.
- **au-190** (Mutual respect and tolerance for others). Answer: Both are illegal. Booklet: Violence of any kind, including verbal and physical abuse, is illegal.
- **au-191** (Mutual respect and tolerance for others). Answer: Peacefully, with everyone free from violence and intimidation. Booklet: Australians believe in peaceful disagreement, free from violence and intimidation.
- **au-192** (Mutual respect and tolerance for others). Answer: 16 or 17, depending on the state or territory. Booklet: Age of sexual consent is 16 or 17, depending on state or territory.
- **au-193** (Mutual respect and tolerance for others). Answer: It is illegal in every state and territory. Booklet: A 20-year-old man cannot have sex with a 15-year-old girl.
- **au-194** (Mutual respect and tolerance for others). Answer: Obey it. Booklet: Obey a lawful police request; Australians commit to following the law.
- **au-195** (Mutual respect and tolerance for others). Answer: Listening to others and respecting their views, even when they differ from yours. Booklet: Mutual respect means listening to others and respecting different views.
- **au-196** (Mutual respect and tolerance for others). Answer: Making racially abusive comments at a sporting event. Booklet: Racism includes racially abusive comments in public or at sporting events.
- **au-197** (Making a contribution). Answer: Support themselves and their family. Booklet: Everyone should try to support themselves and their families when able.
- **au-198** (Compassion for those in need). Answer: Helping each other in times of need. Booklet: Australians value mateship and help each other in times of need.
- **au-199** (Compassion for those in need). Answer: Driving a friend to a medical appointment. Booklet: Examples include driving a friend to a medical appointment.
- **au-200** (Compassion for those in need). Answer: Strong, with many opportunities to volunteer. Booklet: Strong tradition of community service and volunteering; many opportunities to volunteer.
- **au-201** (English as the national language). Answer: It is the national language and an important unifying element. Booklet: English is the national language and an important unifying element.
- **au-202** (English as the national language). Answer: Make reasonable efforts to learn English. Booklet: Applicants must make reasonable efforts to learn English if not native.
- **au-203** (English as the national language). Answer: It helps them get an education and a job, and integrate. Booklet: English helps to get an education, a job, and better integrate.
- **au-204** (Helping to keep our society safe). Answer: Report it to Australian government authorities as soon as possible. Booklet: Report a suspected serious crime to government authorities as soon as possible.
- **au-205** (Helping to keep our society safe). Answer: Report it to the police. Booklet: Report child abuse to the police to investigate.
- **au-206** (Helping to keep our society safe). Answer: Sharing sexual photos of someone online without their consent. Booklet: Cyber abuse includes sharing sexual photos or videos without consent.
- **au-207** (Loyalty to Australia). Answer: Australia and its people. Booklet: Citizens pledge their loyalty to Australia and its people.
- **au-208** (Loyalty to Australia). Answer: Yes, if the laws of that country allow it. Booklet: Citizens may hold other citizenships if those countries' laws allow.
- **au-209** (Loyalty to Australia). Answer: All Australian laws, at all times. Booklet: A citizen in Australia must follow all Australian laws at all times.
- **au-210** (Loyalty to Australia). Answer: The ban on sexual activity with a child under 16. Booklet: Sexual activity with a child under 16 is illegal, here or overseas.
- **au-211** (Loyalty to Australia). Answer: Using social media to share official government secrets. Booklet: Sharing official government secrets on social media undermines Australia's security.
- **au-212** (Loyalty to Australia). Answer: Damage Australian community relations. Booklet: Promoting distrust and fear of an ethnic community damages community relations.
- **au-213** (Freedom of speech). Answer: In public or private places. Booklet: People can meet in public or private for social or political discussion.

## Doubts and judgement calls

1. **Edition.** The PDF says only "© Commonwealth of Australia 2020", but it has clearly been updated since 2022 (King Charles III, Türkiye). There is no version string to record. The `edition` field says this.
2. **Attribution wording.** The booklet does not ask for any. The attribution sentence is Oathly's own, written to the CC BY 4.0 requirements. You may want to shorten it for the About screen.
3. **Are the 5 values questions part of the 20?** The official pages give "20 multiple choice questions" and "5/5 values" without saying how they overlap. The booklet says the test "will also include five questions on Australian values". The usual reading, and what the app's `AuTest` already does, is 5 values questions inside the 20. `test_rules.json` notes this.
4. **House of Representatives size.** The booklet says only "over 150 members". The AEC's 2025 fact sheet says exactly 150 seats were contested in 2025, and "over 150" does not cleanly match that. No question asks for the number of MPs; au-128 asks who an MP represents instead.
5. **Torres Strait Islander flag star.** The booklet does not say how many points the white star has, so no question asks.
6. **"Parliamentary democracy" in Parts 2 and 4.** Both parts have a heading with this name. Questions were placed in the part whose wording they use.
7. **Sensitive-topic questions** (au-177 polygamy, au-178 forced marriage, au-185 partner violence, au-192/au-193 age of consent, au-205 child abuse, au-206 cyber abuse, au-210 child under 16 overseas). All are taken straight from booklet examples and phrased neutrally. Check that they suit the app's tone and age rating.
8. **Political party names.** au-142's explanation lists the main parties the booklet names (Liberal, Labor, Nationals, Greens). These are party names, not officeholders.
9. **No current officeholders are named.** The booklet names King Charles III; the bank asks only about "the King of Australia" as a role.
10. **Answer-length cue.** In 95 of 213 questions the correct option is the longest of the three, mostly where the booklet's wording is long. The app shuffles options, but you could balance this further.
11. **Similar-looking questions.** These pairs share a structure but test different facts, and were kept on purpose: au-022/au-023 (largest/smallest state), au-031/au-034 (Australia Day/Anzac Day dates), au-047/049/050 (flower/colours/gemstone), au-072/073/140, au-103/107, au-122/123 (Premier/Chief Minister).
