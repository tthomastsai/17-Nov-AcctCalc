// Runs test/cases.txt against the JavaScript engine inside index.html (node test/web-engine.test.js)
const fs = require("fs"), path = require("path"), vm = require("vm");
const root = path.join(__dirname, "..");
const html = fs.readFileSync(path.join(root, "index.html"), "utf8");
const src = html.slice(html.indexOf("/* ENGINE START"), html.indexOf("/* ENGINE END */"));
const ctx = {}; vm.createContext(ctx);
vm.runInContext(src + "\nthis.CalcEngine = CalcEngine;", ctx);
let n = 0, fail = 0;
for (const raw of fs.readFileSync(path.join(__dirname, "cases.txt"), "utf8").split("\n")) {
  const line = raw.includes("#") ? raw.slice(0, raw.indexOf("#")) : raw;
  if (!line.includes("|")) continue;
  const [keys, want] = line.split("|").map(s => s.trim());
  const e = new ctx.CalcEngine();
  for (const k of keys.split(/\s+/)) e.press(k);
  n++;
  if (e.entry() !== want) { fail++; console.log(`FAIL  ${keys}  ->  ${e.entry()}  (want ${want})`); }
}
console.log(`${n} cases, ${fail} failed`);
process.exit(fail ? 1 : 0);
