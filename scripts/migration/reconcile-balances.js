// Settles the accounts whose stored balance does not match their records, one decision per account:
//
//   "records"  the records are right: the stored balance is set to what they add up to
//   "stored"   the balance the user sees is right: one adjustment record (income or expense,
//              category "Adjustment", dated now) is added, so the records add up to it
//
// The decisions come as JSON in an environment variable, by account id:
//   $env:DECISIONS = '{"69d2d4eb4238c76d4e45cb82":"stored","6a57c5d759ad286629dcc71e":"records"}'
//   mongosh "<copy uri>" --file scripts/migration/reconcile-balances.js                 (shows what it would do)
//   $env:APPLY = "true"; mongosh "<copy uri>" --file scripts/migration/reconcile-balances.js; Remove-Item Env:APPLY
// Afterwards verify-balances.js must print OK.

const option = (name) => {
  const fromEnv = typeof process !== "undefined" && process.env ? process.env[name] : undefined;
  if (fromEnv !== undefined && fromEnv !== "") return fromEnv;
  return typeof globalThis[name] !== "undefined" ? String(globalThis[name]) : undefined;
};

const ADJUSTMENT_CATEGORY = "Adjustment";
const ADJUSTMENT_NOTE = "تسوية قبل الترحيل إلى العملات المتعددة";

const inShell = typeof db !== "undefined" && typeof db.getName === "function";

if (inShell) {
  // the balance rules are the ones of verify-balances.js (same folder)
  const here = typeof __dirname !== "undefined" ? __dirname : ".";
  const verify = require(require("path").join(here, "verify-balances.js"));
  const apply = option("APPLY") === "true";
  let decisions = {};
  try {
    decisions = JSON.parse(option("DECISIONS") ?? "{}");
  } catch (e) {
    print("DECISIONS is not valid JSON: " + e.message);
    quit(1);
  }

  const data = {
    accounts: db.accounts.find().toArray(),
    incomes: db.incomes.find().toArray(),
    expenses: db.expenses.find().toArray(),
    transfers: db.transfers.find().toArray(),
    movements: db.debtMovement.find().toArray(),
    debts: db.debts.find().toArray(),
  };
  const rows = verify.compare(data.accounts, verify.expectedBalances(data)).filter((r) => Math.abs(r.diff) > 0.005);

  for (const row of rows) {
    const decision = decisions[row.accountId];
    const account = data.accounts.find((a) => String(a._id) === row.accountId);
    if (decision === "records") {
      print(`${row.name}: stored ${row.stored} -> ${row.expected} (records are right)`);
      if (apply) db.accounts.updateOne({ _id: account._id }, { $set: { totalBalance: row.expected } });
    } else if (decision === "stored") {
      // stored - expected > 0: money the records miss (an income); < 0: an expense
      const isIncome = row.diff > 0;
      const amount = Math.round(Math.abs(row.diff) * 100) / 100;
      print(`${row.name}: adds ${isIncome ? "income" : "expense"} ${amount} ${row.currency} (balance stays ${row.stored})`);
      if (apply) {
        const record = {
          amount, category: ADJUSTMENT_CATEGORY, description: ADJUSTMENT_NOTE,
          user: account.user, account: new DBRef("accounts", account._id),
          createdAt: new Date(), type: isIncome ? "incomse" : "expense",
        };
        (isIncome ? db.incomes : db.expenses).insertOne(record);
      }
    } else {
      print(`${row.name} (${row.accountId}): no decision, left as it is (difference ${row.diff})`);
    }
  }
  print(apply ? "Done. Now run verify-balances.js." : "Nothing written. Set $env:APPLY = \"true\" to apply.");
}
