README Instructions for Codex
Project Goal
Build an Android app where users collect animal-based cards inspired by games like Pokémon, Magic, or Hearthstone.
Each card represents a real animal from nature.
The app should use Object Oriented Programming so the code is reusable, organized, and easy to expand.
The app should be designed with performance in mind, especially when choosing data structures and algorithms.
Do not focus on battle gameplay yet. Focus first on collecting cards, building decks, scoring decks, and customizing frames.
Core Gameplay Loop
The user plays a simple food-matching mini game.
The mini game shows a randomly selected animal card.
The user must match the animal to its correct food.
After 3 correct matches, the animal card is added to the user’s inventory.
The user can view collected cards in their collection.
The user can add collected cards into decks.
The user can have up to 5 decks.
Each deck can contain up to 5 cards.
Decks are scored using health, danger, and real-life symbiosis bonuses.
The user can customize card frames.
The default card frame should be a black border.
Card Design Requirements
Each animal card must have a consistent layout.
The top left of the card should show the animal’s danger value.
The top right of the card should show the animal’s health value.
Below the danger and health values, the card should show a picture of the animal.
The bottom one-third of the card should show a real-life description of the animal.
The entire card should be surrounded by a frame.
The default frame should be black.
The user should be able to swap card frames.
Frames should be cosmetic only and should not change card stats.
Animal Card Stats
Health should be based on how much damage the animal could realistically take from an average human.
Danger should be based on how dangerous the animal is to an average human.
Health and danger should use clear number scales.
Small fragile animals should usually have low health.
Large or durable animals should usually have high health.
Venomous, aggressive, diseased, or powerful animals should have high danger.
Small animals can still have high danger if they are venomous or carry disease.
Required Main Screens
Create a Home Screen.
Create a Collection Screen.
Create a Card Detail Screen.
Create a Deck Builder Screen.
Create a Mini Game Screen.
Create a Frame Customization Screen.
Home Screen Requirements
The Home Screen should act as the main entry point.
It should include navigation buttons for:
Play Mini Game
View Collection
Build Decks
Customize Frames
View Card Details
Collection Screen Requirements
Show all cards the player owns.
Display cards in a grid or scrollable list.
Each card preview should show:
Animal name
Image
Health
Danger
Current frame
Allow the user to open a card detail view.
Allow the user to add owned cards to decks.
Use efficient scrolling and avoid loading all large images at once.
Card Detail Screen Requirements
Show a larger preview of the selected card.
Show the animal’s full real-life description.
Show the animal’s habitat.
Show the animal’s food.
Show an explanation of the health value.
Show an explanation of the danger value.
Show known symbiotic partners, if any.
Allow the user to open frame customization for that card.
Deck Builder Requirements
The user can create up to 5 decks.
Each deck can contain up to 5 cards.
The user can name decks.
The user can rename decks.
The user can delete decks.
The user can add owned cards to a deck.
The user can remove cards from a deck.
The app must prevent adding cards the user does not own.
The app must prevent adding more than 5 cards to a deck.
The app must prevent creating more than 5 decks.
Show each deck’s current score.
Show each deck’s symbiosis multiplier.
Show which card pairs are receiving symbiosis bonuses.
Mini Game Requirements
The mini game should randomly select an animal card.
The mini game should show food choices.
One food choice must be correct.
The incorrect food choices should be randomly selected.
Food choices should be shuffled before display.
The user selects the food they think matches the animal.
Correct answers increase the match count by 1.
Incorrect answers should show feedback but should not increase the match count.
Once the user reaches 3 correct matches, the card should be added to inventory.
Show a reward message when the card is earned.
Let the user return to the collection or play again.
Frame Customization Requirements
Every card starts with the default black border.
The user can select a card they own.
The user can preview that card.
The user can choose from available frames.
The user can apply a selected frame to the card.
The user can reset the card to the default black border.
Locked frames should be visible but not usable.
Frames should not affect health, danger, or deck score.
Suggested Starting Frames
Black Border
Forest Frame
Ocean Frame
Desert Frame
Arctic Frame
Legendary Gold Frame
Object Oriented Programming Requirements
Use separate classes or models for major parts of the app.
Do not place all logic in one file.
Keep UI logic separate from game rules where possible.
Reuse card components across the app.
Reuse deck logic instead of duplicating deck rules.
Reuse mini game logic instead of writing it directly inside the UI.
Reuse scoring logic through a dedicated scoring or symbiosis manager.
Required Data Models and Classes
Create an AnimalCard model.
Create a PlayerInventory model or manager.
Create a Deck model.
Create a DeckManager.
Create a CardFrame model.
Create a FrameManager.
Create a SymbiosisRelationship model.
Create a SymbiosisManager.
Create a MiniGameSession model.
Create a MiniGameManager.
AnimalCard Model Requirements
Each AnimalCard should include:
Card ID
Animal name
Species or category
Health value
Danger value
Animal image reference
Real-life description
Food type
Habitat
Rarity
Default frame
Current selected frame
PlayerInventory Requirements
Store all cards the player owns.
Allow cards to be added to inventory.
Allow the app to check whether a card is owned.
Allow the app to retrieve all owned cards.
Use a hash map or similar structure keyed by card ID for fast lookup.
Inventory card lookup should be close to O(1).
Adding a card should be close to O(1).
Getting all owned cards should be O(n).
Deck Requirements
Each Deck should include:
Deck ID
Deck name
List of cards
Maximum card limit of 5
Current deck score
Current symbiosis multiplier
A deck cannot contain more than 5 cards.
A deck cannot contain cards the player does not own.
A deck should update its score when cards are added or removed.
DeckManager Requirements
Manage all player decks.
Allow up to 5 total decks.
Create decks.
Delete decks.
Rename decks.
Add cards to decks.
Remove cards from decks.
Validate deck size.
Validate card ownership.
Calculate or request deck score updates.
Because the deck limit is small, deck operations will be very fast in practice.
SymbiosisRelationship Requirements
Each symbiosis relationship should include:
Animal A ID
Animal B ID
Relationship type
Multiplier value
Real-life description of the relationship
Relationship types should include:
Mutualism
Commensalism
Parasitism
SymbiosisManager Requirements
Store all valid real-life symbiotic animal pairs.
Check whether two animals have a real-life symbiotic relationship.
Calculate the total symbiosis multiplier for a deck.
Use a hash set or hash map for fast pair lookups.
Symbiosis pair lookup should be close to O(1).
Deck symbiosis scoring can compare all card pairs.
Comparing all pairs is O(n²), but decks only contain 5 cards, so this is acceptable.
For a 5-card deck, the maximum number of pair checks is 10.
CardFrame Requirements
Each CardFrame should include:
Frame ID
Frame name
Border style
Color or image asset
Unlock status
Rarity or unlock requirement
FrameManager Requirements
Store available frames.
Store player-owned or unlocked frames.
Apply a frame to a card.
Reset a card to the default black border.
Prevent locked frames from being applied.
Prevent frames from being applied to cards the player does not own.
MiniGameSession Requirements
Each MiniGameSession should include:
Current target animal card
Current food options
Correct food answer
Match count
Required match count of 3
Win or reward state
MiniGameManager Requirements
Start a mini game session.
Randomly choose an animal card.
Generate food options.
Make sure one food option is correct.
Shuffle food options.
Check the player’s selected food.
Increase match count after correct choices.
Award the card after 3 correct matches.
Reset the session after the card is awarded.
Deck Scoring Requirements
Deck score should be based on:
Total health
Total danger
Symbiosis multiplier
Calculate base score from the deck’s card stats.
Check every pair of cards in the deck for symbiosis.
Apply multipliers only for real-life symbiotic relationships.
Display the final deck score clearly.
Suggested Deck Scoring Formula
Add all card health values.
Add all card danger values.
Combine those values into a base score.
Check every pair of animals for symbiosis.
Apply relationship multipliers.
Final score equals base score multiplied by the symbiosis multiplier.
Suggested Symbiosis Multipliers
Mutualism: x1.5
Commensalism: x1.25
Parasitism: x1.1
Starting Animal Cards
Begin with a small test set before expanding.
Include around 10–15 animals in the first version.
Suggested first animals:
Lion
Elephant
Crocodile
Wolf
Rabbit
Eagle
Shark
Clownfish
Sea Anemone
Rhinoceros
Oxpecker
Pistol Shrimp
Goby Fish
Remora
Use animals only for the first version.
Plants or ecosystem cards can be added later if needed.
Starting Symbiosis Relationships
Include real-life symbiosis examples.
Start with:
Clownfish and Sea Anemone
Oxpecker and Rhinoceros
Goby Fish and Pistol Shrimp
Remora and Shark
Store a short description for each relationship.
Show the player why the symbiosis bonus applies.
Data Storage Requirements
Use local storage for the first version.
Save owned cards.
Save decks.
Save selected card frames.
Save unlocked frames.
Save mini game progress only if needed.
The app should restore player data after closing and reopening.
Performance and Big-O Requirements
Use hash maps for inventory lookup.
Inventory add operation should be close to O(1).
Inventory ownership check should be close to O(1).
Getting all inventory cards is O(n).
Deck size checks should be O(1).
Checking whether a card is already in a deck is O(n), but n is at most 5.
Validating all cards in a deck is O(n), but n is at most 5.
Symbiosis checking across a deck is O(n²), but n is at most 5.
Use a hash set or hash map for individual symbiosis pair lookup so each pair check is close to O(1).
Random card selection from a list should be O(1).
Food answer checking should be O(1).
Shuffling food options is O(n), but the food option list should be small.
Card filtering and searching can start as O(n).
If the animal database becomes large later, add maps grouped by habitat, rarity, food type, or animal category.
Error Handling Requirements
Show a message if the user tries to create more than 5 decks.
Show a message if the user tries to add more than 5 cards to a deck.
Show a message if the user tries to add a card they do not own.
Show a message if the user tries to add the same card twice, if duplicates are not allowed.
Show a message if card data is missing.
Show a message if an image is missing.
Show a message if no animal cards are available for the mini game.
Show a message if no food options are available.
Make sure the mini game always includes the correct food answer.
Prevent the same reward from being granted multiple times by accident.
Show a message if the user tries to apply a locked frame.
Show a message if the user tries to apply a frame to a card they do not own.
Show a message if a frame asset is missing.
Required User Feedback Messages
“Deck is full.”
“You can only have 5 decks.”
“You do not own this card yet.”
“Correct match!”
“Incorrect food.”
“Card added to inventory.”
“Frame applied.”
“This frame is locked.”
Development Order
Step 1: Create the base Android project.
Step 2: Create the main navigation structure.
Step 3: Add placeholder screens.
Step 4: Create the main object-oriented data models.
Step 5: Add sample animal card data.
Step 6: Add sample symbiosis relationship data.
Step 7: Build the reusable card UI component.
Step 8: Build the inventory system.
Step 9: Build the collection screen.
Step 10: Build the card detail screen.
Step 11: Build the deck system.
Step 12: Build the deck builder screen.
Step 13: Add deck validation rules.
Step 14: Add deck scoring.
Step 15: Add symbiosis multiplier scoring.
Step 16: Build the mini game system.
Step 17: Build the mini game screen.
Step 18: Award cards after 3 correct matches.
Step 19: Build the frame system.
Step 20: Build the frame customization screen.
Step 21: Add local save/load functionality.
Step 22: Add error handling and user feedback.
Step 23: Add unit tests for game rules.
Step 24: Add UI tests for card layout and main interactions.
Step 25: Polish the app’s layout, spacing, text, and usability.
Testing Requirements
Test that inventory can add cards.
Test that inventory can detect owned cards.
Test that inventory can return all owned cards.
Test that a deck cannot exceed 5 cards.
Test that the player cannot create more than 5 decks.
Test that a deck cannot use unowned cards.
Test that symbiosis pairs are detected correctly.
Test that non-symbiotic pairs do not give bonuses.
Test that deck score calculates correctly.
Test that the mini game awards a card after 3 correct matches.
Test that incorrect mini game answers do not increase the match count.
Test that card frames can be changed.
Test that locked frames cannot be applied.
Test that card data saves and loads correctly.
Test that deck data saves and loads correctly.
Test that frame choices save and load correctly.
UI Testing Requirements
Test that health appears in the top right of the card.
Test that danger appears in the top left of the card.
Test that the animal image appears below the stats.
Test that the description appears in the bottom one-third of the card.
Test that the default frame is black.
Test that changing frames updates the card visually.
Test that the deck builder blocks invalid actions.
Test that mini game buttons respond correctly.
Test that reward messages appear after earning a card.
First Version Priority
Focus on the core loop first.
The first version should allow the user to:
Play the mini game.
Earn an animal card.
Add the card to inventory.
View the collection.
Add cards to decks.
Score decks using health, danger, and symbiosis.
Customize card frames.
Do not add complex battles yet.
Do not add online multiplayer yet.
Do not add cloud saving yet.
Do not add too many animals before the core loop works.
Future Expansion Ideas
Add more animals.
Add more habitats.
Add more frame types.
Add card rarity.
Add card packs.
Add animal categories.
Add daily mini game challenges.
Add timed food-matching mode.
Add streak bonuses.
Add wrong-answer penalties.
Add cloud saving.
Add player profiles.
Add deck battles.
Add encyclopedia mode.
Add achievements.
Add rewards for collecting full symbiosis pairs.
Add biome-based decks.
Add more detailed biology facts.
Final Instruction for Codex
Build the project in small working steps.
Keep the code object-oriented and reusable.
Keep game rules separate from UI when possible.
Prioritize working gameplay over visual polish at first.
Enforce the 5-deck limit.
Enforce the 5-card-per-deck limit.
Use real-life symbiosis only for symbiosis bonuses.
Make the card layout match the required design.
Make the default frame black.
Make the food-matching mini game award a card after 3 correct matches.
Save the player’s inventory, decks, and frame choices locally.
Test the game rules before polishing the UI.

## Google Play Store Release Checklist

This section turns the Google Play publishing process into project-specific action steps for WildDeck. Requirements can change, so re-check the linked official Google pages before submitting a production release.

Official references:

- Google Play Console app setup: https://support.google.com/googleplay/android-developer/answer/9859152
- Target API level requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756
- Data safety form: https://support.google.com/googleplay/android-developer/answer/10787469
- New personal account testing requirements: https://support.google.com/googleplay/android-developer/answer/14151465
- Content ratings and target audience: https://support.google.com/googleplay/android-developer/answer/9859655
- Prepare app for review: https://support.google.com/googleplay/android-developer/answer/9859455

### 1. Prepare the developer account

- Create or sign into a Google Play Console developer account.
- Complete identity verification and account payments/tax setup if Google asks for it.
- Decide whether the publisher is an individual or organization. If this is a personal developer account created after November 13, 2023, plan for Google Play's closed-testing requirement before production release.
- Keep the Play Console account email, support email, recovery methods, and two-factor authentication current.

### 2. Lock down the app identity

- Confirm the package/application ID before first upload: `com.wilddeck.app`.
- Treat the package name as permanent. Google Play package names cannot be reused after publishing.
- Confirm app type/category in Play Console. WildDeck should probably be submitted as a game unless the educational collection side becomes the main purpose.
- Choose the final public app name, short description, and full description.
- Prepare a support email address. A contact email is required for the Play listing.

### 3. Confirm technical Play requirements

- Keep `targetSdk` at the current Google Play requirement or higher. This project currently targets API 35 in `app/build.gradle.kts`, which matches the Android 15 requirement that applies to new apps and updates starting August 31, 2025.
- Keep `minSdk` at a realistic supported level. This project currently uses `minSdk = 26`.
- Increment `versionCode` for every upload to Play Console. Current value is `1`; every release/update needs a higher number.
- Update `versionName` for human-readable releases, such as `1.0.0`, `1.0.1`, etc.
- Build a release Android App Bundle (`.aab`), not just a debug APK. Google Play uses Android App Bundles for modern app delivery.
- Keep the app's compressed download size below Google Play's app bundle generated APK limit. Google lists 200 MB as the maximum compressed download size for APKs generated from app bundles.

Project release build command:

```powershell
$env:JAVA_HOME='C:\Users\brian\.jdks\temurin-17.0.19'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat clean testDebugUnitTest bundleRelease
```

Expected artifact:

```text
app/build/outputs/bundle/release/app-release.aab
```

### 4. Set up release signing safely

- Enroll in Play App Signing during the first Play Console release setup.
- Create an upload key/keystore for signing the release app bundle.
- Store the keystore outside the Git repository.
- Store keystore passwords in a local/private Gradle properties file or environment variables, not in source control.
- Back up the upload key securely. Google can reset an upload key, but losing signing material still slows releases.
- Never commit `.jks`, `.keystore`, passwords, or generated signing config secrets.

Suggested local-only files:

```text
keystore/wilddeck-upload-key.jks
keystore.properties
```

Add these to `.gitignore` if release signing is added.

### 5. Audit privacy and data collection

Current project behavior appears local-first:

- Player cards, decks, frames, and progress are saved locally.
- No network permission was found in the current Android manifest.
- No ad SDK, analytics SDK, account login, location access, contacts, camera, microphone, or payment system is currently apparent.

Before submission, verify this again by checking:

- `app/src/main/AndroidManifest.xml`
- Gradle dependencies
- Any future SDKs, analytics, crash reporting, ads, cloud save, or login features

Then complete Google Play's Data safety form:

- If the app still does not transmit user data off-device, declare that accurately.
- If any SDK is added later, include that SDK's data collection/sharing in the Data safety form.
- If the app collects or shares user data in the future, disclose the data types, purposes, whether the data is encrypted in transit, whether collection is required or optional, and whether users can request deletion.
- Keep the Data safety form updated every time app behavior or SDKs change.

### 6. Create a privacy policy

Even if the app collects no personal data, publish a simple privacy policy URL before release. This is especially important because WildDeck may appeal to younger users and may later add analytics, cloud saves, ads, or accounts.

The privacy policy should state:

- What data the app stores locally.
- Whether any data leaves the device.
- Whether the app uses ads, analytics, crash reporting, or third-party SDKs.
- Whether the app is intended for children or a general audience.
- How users can contact support.
- How users can request data deletion if cloud/account features are ever added.

Host the policy somewhere stable, such as a personal website, GitHub Pages, or another public URL, and add it in Play Console.

### 7. Complete Play Console app content declarations

In Play Console, go through the App content section and complete every declaration:

- Data safety
- Privacy policy
- Ads: currently answer "No" if no ads or ad SDKs are added.
- App access: if the app does not require login, say all features are available without special access. If a login or tester code is added later, provide review instructions.
- Content rating questionnaire: answer based on actual content. WildDeck includes animal combat/gameplay, so describe fantasy/card battle content honestly even if it is non-graphic.
- Target audience and content: choose the intended age group carefully. Because this is animal/trivia content, decide whether it is for all ages, teens, or a children-focused audience. If selecting children under 13, expect stricter Families policy obligations.
- News, financial features, health features, government features, permissions, and other declarations if Play Console shows them.

### 8. Prepare store listing assets

Create production-quality store assets:

- App icon: 512 x 512 PNG.
- Feature graphic: 1024 x 500 PNG.
- Phone screenshots: at least 2, showing the main page, card collection, deck builder, Wild Run, frame store, and learn-more page.
- Optional promo video.
- Short description: maximum 80 characters.
- Full description: maximum 4000 characters.
- Category and tags.
- Support email.
- Optional support website.

Suggested listing angle:

- WildDeck is an animal card collection and trivia game.
- Players learn real-world animal facts, unlock cards, build decks, equip frames, and play Wild Run battles.
- Avoid unsupported claims like "scientifically complete" unless the content is fully reviewed.

### 9. Run local quality checks before upload

Before every Play upload:

```powershell
$env:JAVA_HOME='C:\Users\brian\.jdks\temurin-17.0.19'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin bundleRelease
```

Manual smoke test checklist:

- Fresh install opens to the main page.
- Swipe navigation works across all 5 home pages.
- Mini game spends/uses points correctly.
- Unlocking cards works and does not repeat owned animals.
- Collection shows locked/unlocked cards correctly.
- Deck page can create all 5 decks from plus slots.
- Cards can be added to and removed from decks.
- Wild Run can start from a built deck.
- Frame store works and frame prices are correct.
- Learn More page locks trivia until the animal is unlocked.
- App still works after closing and reopening.
- No debug-only text, placeholder assets, or broken images appear.

### 10. Create Play Console app and upload internal test

- In Play Console, select Create app.
- Enter app name, default language, app/game type, free/paid status, support email, policy declarations, and Play App Signing terms.
- Upload the first signed release `.aab` to an internal testing track first.
- Add internal testers by email or Google Group.
- Install from the Play testing link and verify the Play-delivered build, not just the Android Studio build.
- Fix any pre-review warnings from Play Console.

### 11. Closed testing and production access

If using a personal developer account created after November 13, 2023:

- Create a closed testing track after app setup is complete.
- Add at least 12 testers.
- Make sure at least 12 testers opt in and remain opted in continuously for at least 14 days.
- Gather feedback and fix issues during the closed test.
- Apply for production access in Play Console after the requirement is met.
- Be ready to answer questions about the app, test results, and production readiness.

If using an organization account or an older account, still use internal/closed testing before production because it catches Play-specific problems early.

### 12. Submit production release

- Create a production release using the approved `.aab`.
- Add release notes.
- Review Play Console policy warnings, pre-launch report results, device compatibility, Android vitals, and app bundle explorer output.
- Use managed publishing if you want review approval and public release to happen separately.
- Consider a staged rollout instead of immediately releasing to 100% of users.
- After launch, monitor crashes, ANRs, reviews, ratings, and user feedback.

### 13. Ongoing maintenance after launch

- Increment `versionCode` for every update.
- Keep target API updated yearly.
- Re-run the Data safety audit whenever adding SDKs or features.
- Keep screenshots and descriptions accurate as the UI changes.
- Watch Play Console policy status and Android vitals.
- Test updates through internal testing before production.
- Keep animal facts, image rights, and commercial-use asset records organized.
