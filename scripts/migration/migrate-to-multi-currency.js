// Multi-currency migration, step 1: ADD the new fields. Nothing is removed or renamed,
// so the running app keeps working and the step can be repeated safely.
//
//   accounts      balances: [{ currency, openingBalance, totalBalance }]  (from currency / balance / totalBalance)
//   incomes       currency            = the account's currency
//   expenses      currency            = the account's currency
//   transfers     sourceCurrency, destinationCurrency, sendFeeCurrency, receiveFeeCurrency
//   schedules     sourceCurrency, destinationCurrency
//   debtMovement  currency            = the debt's currency
//
// Run on the COPY:
//   mongosh "mongodb://localhost:27017/myBudget_migration_test" --file scripts/migration/migrate-to-multi-currency.js
// Without $env:APPLY = "true" it is a dry run: it only counts and writes nothing.
// Then: verify-balances.js must print OK with the same numbers as before.
//
// Repeating it recomputes the same values from the old fields (useful while the old code
// still changes them on the copy). The old fields are removed by a later step, only after
// the new code is released and checked.

const MIGRATION_ID = "multi-currency";
const MIGRATION_VERSION = 1;

// Options come from environment variables: mongosh does not reliably share a variable
// set with --eval with the --file script. In PowerShell:  $env:ACCOUNT = "..."
const option = (name) => {
  const fromEnv = typeof process !== "undefined" && process.env ? process.env[name] : undefined;
  if (fromEnv !== undefined && fromEnv !== "") return fromEnv;
  return typeof globalThis[name] !== "undefined" ? String(globalThis[name]) : undefined;
};


const refId = (ref) => {
  if (ref === null || ref === undefined) return null;
  const id = ref.oid ?? ref.$id ?? ref._id ?? ref;
  return id === null || id === undefined ? null : String(id);
};

// ---------- pure planning (also used by the test) ----------

// returns { accounts: [{ _id, set }], incomes: [...], ..., problems: [...] }
function planMigration({ accounts, incomes, expenses, transfers, schedules, movements, debts }) {
  const plan = { accounts: [], incomes: [], expenses: [], transfers: [], schedules: [], movements: [], problems: [] };
  const currencyOf = new Map();

  for (const a of accounts) {
    if (!a.currency) {
      plan.problems.push(`account ${a._id} (${a.name}) has no currency`);
      continue;
    }
    currencyOf.set(String(a._id), a.currency);
    const opening = Number(a.balance ?? 0);
    const total = Number(a.totalBalance ?? a.balance ?? 0);
    plan.accounts.push({ _id: a._id, set: { balances: [{ currency: a.currency, openingBalance: opening, totalBalance: total }] } });
  }

  // a record whose account was deleted keeps currency null: it moves no balance any more
  const cur = (ref) => currencyOf.get(refId(ref)) ?? null;

  for (const r of incomes) plan.incomes.push({ _id: r._id, set: { currency: cur(r.account) } });
  for (const r of expenses) plan.expenses.push({ _id: r._id, set: { currency: cur(r.account) } });
  for (const t of transfers) {
    plan.transfers.push({
      _id: t._id,
      set: {
        sourceCurrency: cur(t.sourceAccount),
        destinationCurrency: cur(t.destinationAccount),
        sendFeeCurrency: t.sendFeeAccount ? cur(t.sendFeeAccount) : null,
        receiveFeeCurrency: t.receiveFeeAccount ? cur(t.receiveFeeAccount) : null,
      },
    });
  }
  for (const s of schedules) {
    plan.schedules.push({
      _id: s._id,
      set: { sourceCurrency: cur(s.sourceAccount), destinationCurrency: s.destinationAccount ? cur(s.destinationAccount) : null },
    });
  }
  const debtCurrency = new Map(debts.map((d) => [String(d._id), d.currency]));
  for (const m of movements) {
    const currency = debtCurrency.get(refId(m.debt)) ?? cur(m.account);
    const accountCurrency = cur(m.account);
    if (accountCurrency && currency && accountCurrency !== currency) {
      plan.problems.push(`debt movement ${m._id}: account in ${accountCurrency}, debt in ${currency}`);
    }
    plan.movements.push({ _id: m._id, set: { currency } });
  }
  return plan;
}

// ---------- shell entry ----------

// mongosh also defines "module", so the shell is recognised by its "db" object instead
const inShell = typeof db !== "undefined" && typeof db.getName === "function";

if (inShell) {
  // writes only when asked:  $env:APPLY = "true"   (otherwise a dry run that only counts)
  const dryRun = option("APPLY") !== "true";
  const previous = db.schema_version.findOne({ _id: MIGRATION_ID });
  if (previous) print(`Note: ${MIGRATION_ID} v${previous.version} already ran at ${previous.at}; running again recomputes the same fields.`);

  const plan = planMigration({
    accounts: db.accounts.find().toArray(),
    incomes: db.incomes.find().toArray(),
    expenses: db.expenses.find().toArray(),
    transfers: db.transfers.find().toArray(),
    schedules: db.schedules.find().toArray(),
    movements: db.debtMovement.find().toArray(),
    debts: db.debts.find().toArray(),
  });

  const targets = [
    ["accounts", db.accounts], ["incomes", db.incomes], ["expenses", db.expenses],
    ["transfers", db.transfers], ["schedules", db.schedules], ["movements", db.debtMovement],
  ];
  const counts = {};
  for (const [name, collection] of targets) {
    const ops = plan[name].map((p) => ({ updateOne: { filter: { _id: p._id }, update: { $set: p.set } } }));
    counts[name] = ops.length;
    if (!dryRun && ops.length) collection.bulkWrite(ops, { ordered: false });
  }

  print(`${dryRun ? "DRY RUN (set $env:APPLY = \"true\" to write), nothing written. Would update: " : "Updated: "}`+` ${JSON.stringify(counts)}`);
  if (plan.problems.length) {
    print(`${plan.problems.length} problem(s) to look at:`);
    plan.problems.forEach((p) => print(" - " + p));
  }
  if (!dryRun) {
    db.schema_version.replaceOne(
      { _id: MIGRATION_ID },
      { _id: MIGRATION_ID, version: MIGRATION_VERSION, at: new Date(), counts, problems: plan.problems },
      { upsert: true },
    );
    print("Done. Now run verify-balances.js: it must print OK.");
  }
}

if (!inShell && typeof module !== "undefined") module.exports = { planMigration };
