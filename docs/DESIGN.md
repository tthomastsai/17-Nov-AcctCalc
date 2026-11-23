# Design notes / 設計說明

Decisions that are settled, things deliberately left out, and how changes are made. Check here before reopening a question.

已經定案的決定、刻意不做的事，以及之後怎麼改版。想到新點子時，先看看這裡是否已經討論過。

---

## English

### Status

**3.0.1 is feature-complete.** From here on only real bugs are fixed (wrong results, crashes, behaviour that differs from the real calculator). Appearance is not redesigned.

### Settled

- **Look:** a replica of the LIBERTY LB-5003CA: raised opaque keys with a soft shadow, thickness, a lighter top lip and a hairline rim that fades out downwards; LCD; solar panel and "12 digits" header. The settings page uses the same look.
- **Behaviour:** follows the real calculator, checked key by key on the device (see the key table in the README and `test/cases.txt`). When the app and the device disagree, the device wins.
- **Themes:** five (Classic Cream, Obsidian, Mint, Sakura Pink, Sky Blue). Obsidian is used automatically in dark mode if that option is on.
- **Fonts:** Google Sans for the interface and digits; AcctCalc Seven as the optional seven-segment display font.
- **Material 3:** only the dark-mode switch is a Material 3 component; everything else keeps the calculator's look.
- **Platforms:** the native Android app, and the web version (also used on iPad from the home screen).

### Not doing

| Idea | Why not |
|---|---|
| A full Material 3 interface as an option | The replica look is the point of the app; a second style doubles every visual change on two platforms |
| More themes, including one that follows the wallpaper colours | Five are enough |
| Publishing on Google Play | Not planned; the APK in `dist/` is installed directly |
| A native iOS / iPadOS app | The web version added to the home screen does the job |
| An OFF key | A phone app has no use for it; the key opens Settings instead |
| Drawing the curved body, an analogue-style display font | Not wanted |
| Wider keys on iPad | Very wide keys are not easier to use |
| From code reviews: BigDecimal arithmetic, encapsulating the engine's fields, restoring state after the process is killed, an enum state machine, CI | Deferred: the tests cover the behaviour and none of these changes what the user sees |

### How changes are made

1. **Collect, then change.** Note things over a day or two and hand them over as one list; one batch becomes one version.
2. **Visual changes: pictures first.** Comparison images (A / B) are made and a choice is made before the code is changed. Renders are checked zoomed in before a build is sent.
3. **Unreleased first.** Changes are pushed and listed under *Unreleased* in `CHANGELOG.md` without changing the version number. When a batch is done: bump the version (major.feature.fix), tag it, build the APK.
4. **The web version catches up at release time**, not after every small change.

---

## 正體中文

### 目前狀態

**3.0.1 是功能完成版。** 之後只修真正的錯誤（算錯、閃退、和實機不一致的行為），外觀不再大改。

### 已經定案

- **外觀：** 仿利百代 LB-5003CA：不透明的立體按鍵，有柔和陰影、厚度、頂端較亮的邊和往下淡出的細邊線；液晶螢幕；上方的太陽能板與「12 digits」字樣。設定頁使用同一套質感。
- **行為：** 以實機為準，逐一按鍵在實機上確認（見 README 的按鍵表和 `test/cases.txt`）。App 和實機不同時，以實機為準。
- **主題：** 五個（經典奶油、曜石黑、薄荷綠、櫻花粉、天空藍）。開啟選項時，深色模式自動換成曜石黑。
- **字型：** 介面和數字用 Google Sans；螢幕數字可選自製的七段字型 AcctCalc Seven。
- **Material 3：** 只有深色模式開關是 Material 3 元件，其餘維持計算機的質感。
- **平台：** 原生 Android App，以及網頁版（iPad 上加到主畫面使用）。

### 不做的事

| 點子 | 原因 |
|---|---|
| 另外提供整套 Material 3 介面 | 仿實機的外觀就是這個 App 的特色；多一套風格，每個外觀修改都要在兩個平台各改兩次 |
| 再加主題，包括跟隨桌布變色的主題 | 五個就夠了 |
| 上架 Google Play | 沒有打算；直接安裝 `dist/` 裡的 APK |
| 原生 iOS／iPadOS App | 網頁版加到主畫面就夠用 |
| OFF 鍵 | 手機上用不到，那個位置改成設定鍵 |
| 畫出機身曲線、類比風格的螢幕字型 | 不需要 |
| iPad 上把按鍵拉寬 | 太寬的按鍵不一定比較好按 |
| 程式碼審查提過的：改用 BigDecimal、封裝計算引擎的欄位、App 被系統關掉後還原狀態、enum 狀態機、CI | 暫緩：測試已經涵蓋行為，這些都不會改變使用者看到的結果 |

### 之後怎麼改版

1. **先累積再改：** 用一兩天，把想改的地方記成一張清單一次交出；一批改動就是一個版本。
2. **外觀修改先看圖：** 先做 A／B 比較圖、選好再改程式；出貨前先放大檢查細節。
3. **先記成「尚未發布」：** 改動照樣推上 GitHub，記在 `CHANGELOG.md` 的「尚未發布」，不升版本號。一批告一段落時，再升版本（主版本.功能.小修）、打 tag、產生 APK。
4. **網頁版在發版時一次跟上**，不必每個小改都同步。
