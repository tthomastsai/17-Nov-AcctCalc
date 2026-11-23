# Changelog / 版本紀錄

Versions follow **major.feature.fix**: the middle number goes up for a new feature or a visible change, the last for small fixes and tweaks. Releases before 3.0.0 were first numbered 2.1 to 2.23; the old number is shown in brackets. Each version has a git tag (`v2.1.0` … `v3.0.0`); see the repository's Tags page.

版本號採用「**主版本.功能.小修**」：新增功能或明顯改變時加中間的數字，小修正、微調時加最後一碼。3.0.0 之前的版本原本編為 2.1 到 2.23，括號內是舊編號。每個版本都有對應的 git tag（`v2.1.0` … `v3.0.0`），可在 repository 的 Tags 頁面查看。

---

## English

### 3.0.1
- The settings page shares the calculator's look: raised cards with soft shadows and a little thickness, a round back key, a raised accent pill in a recessed groove for the position choice, a raised slider knob and raised theme swatches.
- The dark-mode switch is a Material 3 switch.
- Settings slide in from the right and slide back out the same way.

### 3.0.0
- Version numbers tidied into major.feature.fix; this changelog added.
- Softer key edges: the rim fades out downwards, so there is no line along the bottom, and the top highlight is a lighter shade of the key rather than white, so it no longer looks like a gap.
- About reads “由 tthomastsai 開發” in Chinese.

### 2.9 — opaque keys
- **2.9.0** (2.20) Key faces are fully opaque; light themes get a hairline rim.
- **2.9.1** (2.21) The rim goes around the whole key, thickness included.
- **2.9.2** (2.22) Thinner √; the settings key is an outlined gear.
- **2.9.3** (2.23) The author's name is bold in About.

### 2.8 — `5 =` counts towards GT; acknowledgements
- **2.8.0** (2.18) `=` after a single number adds it to GT, as on the real calculator. Acknowledgements and the full font licences in About.
- **2.8.1** (2.19) Acknowledgements get a card of their own; the decimal point sits a little lower.

### 2.7 — seven-segment display font
- **2.7.0** (2.14) Choice of display digits: Google Sans or AcctCalc Seven, a seamless upright seven-segment font made for this app.
- **2.7.1** (2.15) English README rewritten; licence name fixed.
- **2.7.2** (2.16) The build stops on compile errors; random key-sequence tests; edge cases.
- **2.7.3** (2.17) No permissions at all; more key-sequence tests.

### 2.6.0 (2.13) — settings page
- Settings on a page of their own with an About section; the language can also be set per app in Android 13 and later.

### 2.5.0 (2.12) — first-run tour
- A short guided tour on first launch. The theme setting is labelled light-mode-only while dark mode is followed.

### 2.4.0 (2.11) — dark mode
- Switches to Obsidian when the phone is in dark mode. The calculator sits at the bottom by default.

### 2.3 — header as on the real calculator
- **2.3.0** (2.7) Header layout follows the LB-5003CA.
- **2.3.1** (2.8) '12 digits' moved towards the middle.
- **2.3.2** (2.9) Header aligned; smaller gap above the display.
- **2.3.3** (2.10) The solar panel stays dark after the first frame.

### 2.2 — Accounting Calculator
- **2.2.0** (2.4) Renamed Accounting Calculator; the original key look restored.
- **2.2.1** (2.5) Shown as 會計計算機 on Chinese-language phones.
- **2.2.2** (2.6) Results of M+ and M− no longer go into GT.

### 2.1 — first version in this repository
- **2.1.0** (2.1) Native Android app modelled on the LIBERTY LB-5003CA.
- **2.1.1** (2.2) and **2.1.2** (2.3) Attempted fixes to the key shading, later reverted.

---

## 正體中文

### 3.0.1
- 設定頁改成和計算機一致的質感：卡片浮起、有柔和陰影和一點厚度；返回鍵是圓形按鍵；機身位置的選項是凹槽裡浮起的強調色按鍵；滑桿的圓鈕和主題色票也是浮起的。
- 深色模式開關改成 Material 3 樣式。
- 設定頁從右邊滑入，離開時同樣滑出。

### 3.0.0
- 版本號整理成「主版本.功能.小修」，並新增這份版本紀錄。
- 按鍵邊緣更柔和：邊線往下淡出，下緣不再有一圈線；頂端高光改成按鍵本身顏色再亮一點，不再像一條縫。
- 「關於」改成「由 tthomastsai 開發」。

### 2.9：不透明按鍵
- **2.9.0**（2.20）按鍵表面改成完全不透明，淺色主題加上細邊線。
- **2.9.1**（2.21）邊線改成包住整顆鍵，連厚度一起。
- **2.9.2**（2.22）根號變細；設定鍵改成線條齒輪。
- **2.9.3**（2.23）「關於」裡的作者名字改成粗體。

### 2.8：`5 =` 也進 GT；致謝
- **2.8.0**（2.18）只輸入一個數字再按 `=`，也會加進 GT，與實機相同。「關於」新增致謝和字型授權全文。
- **2.8.1**（2.19）致謝獨立成一張卡片；小數點稍微往下移。

### 2.7：七段顯示字型
- **2.7.0**（2.14）螢幕數字可選 Google Sans 或 AcctCalc Seven（本 App 自製、筆畫無接縫的直立七段字型）。
- **2.7.1**（2.15）重寫英文 README，修正授權名稱。
- **2.7.2**（2.16）編譯錯誤時建置會停止；新增隨機按鍵序列測試與邊界案例。
- **2.7.3**（2.17）完全不需要任何權限；更多按鍵序列測試。

### 2.6.0（2.13）：獨立設定頁
- 設定改成獨立一頁，並新增「關於」；Android 13 以上也能在系統設定裡為本 App 單獨設定語言。

### 2.5.0（2.12）：首次導覽
- 第一次開啟時有簡短導覽。跟隨深色模式時，主題設定會標明是淺色模式用的。

### 2.4.0（2.11）：深色模式
- 手機進入深色模式時自動換成曜石黑。機身預設靠下。

### 2.3：上方排版比照實機
- **2.3.0**（2.7）上方排版比照 LB-5003CA。
- **2.3.1**（2.8）「12 digits」往中間移。
- **2.3.2**（2.9）上方元素對齊，縮小與螢幕的距離。
- **2.3.3**（2.10）太陽能板在第一格畫面之後不再變灰。

### 2.2：改名會計計算機
- **2.2.0**（2.4）改名為 Accounting Calculator，恢復原本的按鍵外觀。
- **2.2.1**（2.5）中文手機上顯示為「會計計算機」。
- **2.2.2**（2.6）M+／M− 算出的結果不再加進 GT。

### 2.1：本 repository 的第一版
- **2.1.0**（2.1）仿利百代 LB-5003CA 的原生 Android App。
- **2.1.1**（2.2）、**2.1.2**（2.3）嘗試修正按鍵明暗，之後已還原。
