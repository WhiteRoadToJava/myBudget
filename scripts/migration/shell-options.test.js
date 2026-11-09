// node scripts/migration/shell-options.test.js
// Runs the scripts the way mongosh does (with a fake db) and checks the options.
const assert = require("assert");
const vm = require("vm"), fs = require("fs");
const col = (docs) => ({ find: () => ({ toArray: () => docs }), insertOne: () => {}, findOne: () => null, replaceOne: () => {}, bulkWrite: (ops) => { written.push(ops.length); } });
let written = [];
const ref = (id) => ({ oid: id });
const db = { getName: () => "sim", accounts: col([{ _id: "A", name: "Main", currency: "SEK", balance: 0, totalBalance: 10 }]),
  incomes: col([{ _id: "i", account: ref("A"), amount: 12 }]), expenses: col([]), transfers: col([]), schedules: col([]),
  debtMovement: col([]), debts: col([]), migration_reports: col([]), schema_version: col([]) };
function run(file, env) {
  const out = [];
  const ctx = { db, process: { env }, print: (...a) => out.push(a.join(" ")), printjson: (o) => out.push(JSON.stringify(o)), module: {}, globalThis: {},
    require, __dirname, quit: () => {}, DBRef: function (c, id) { this.c = c; this.id = id; } };
  ctx.globalThis = ctx;
  vm.runInNewContext(fs.readFileSync(file, "utf8"), ctx);
  return out.join("\n");
}
assert.ok(run(__dirname + "/verify-balances.js", {}).startsWith("Balance check (before): 1 balances, 1 different"));
assert.ok(run(__dirname + "/verify-balances.js", { ACCOUNT: "A" }).startsWith('{"account":"Main"'));
written = []; assert.ok(run(__dirname + "/migrate-to-multi-currency.js", {}).startsWith("DRY RUN")); assert.strictEqual(written.length, 0);
written = []; assert.ok(run(__dirname + "/migrate-to-multi-currency.js", { APPLY: "true" }).startsWith("Updated")); assert.strictEqual(written.length, 2);
// after the migration, the known differences of the "before" report are accepted, new ones fail
const reports = [];
db.migration_reports = { insertOne: (r) => reports.push(r),
  find: (q) => ({ sort: () => ({ limit: () => ({ toArray: () => reports.filter((r) => r.phase === q.phase).slice(-1) }) }) }) };
run(__dirname + "/verify-balances.js", {});                       // before: A differs by -2
const migratedA = { _id: "A", name: "Main", currency: "SEK", balance: 0, totalBalance: 10,
  balances: [{ currency: "SEK", openingBalance: 0, totalBalance: 10 }] };
db.accounts = col([migratedA]);
db.incomes = col([{ _id: "i", account: ref("A"), amount: 12, currency: "SEK" }]);
assert.ok(run(__dirname + "/verify-balances.js", {}).includes("OK: no new difference"));
migratedA.balances[0].totalBalance = 11;                           // the migration changed a balance
assert.ok(run(__dirname + "/verify-balances.js", {}).includes("FAILED: 1 difference(s) that were not there"));
migratedA.balances[0].totalBalance = 10;
db.accounts = col([{ _id: "A", name: "Main", currency: "SEK", balance: 0, totalBalance: 10 }]);
db.incomes = col([{ _id: "i", account: ref("A"), amount: 12 }]);

// reconcile: "records" sets the stored balance, "stored" adds an adjustment record
const inserted = [];
db.incomes.insertOne = (r) => inserted.push(r);
db.accounts.updateOne = (q, u) => inserted.push(u);
let out = run(__dirname + "/reconcile-balances.js", { DECISIONS: '{"A":"stored"}' });
assert.ok(out.includes("adds expense 2 SEK"), out); assert.strictEqual(inserted.length, 0);   // dry run
out = run(__dirname + "/reconcile-balances.js", { DECISIONS: '{"A":"records"}', APPLY: "true" });
assert.strictEqual(JSON.stringify(inserted[0]), JSON.stringify({ $set: { totalBalance: 12 } }));   // objects from the vm context: compare as JSON
console.log("shell options: all checks passed");
