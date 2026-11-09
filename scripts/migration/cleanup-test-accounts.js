// Removes the accounts left behind by Postman runs ("Review B 1793189620310"...) and
// everything that points to them. Shows the list first; deletes only with APPLY.
//
//   list:    mongosh "<db uri>" --file scripts/migration/cleanup-test-accounts.js
//   delete:  $env:APPLY = "true"; mongosh "<db uri>" --file scripts/migration/cleanup-test-accounts.js; Remove-Item Env:APPLY
//
// Safety: a transfer between a test account and a real account is NOT deleted (it is
// listed instead), because deleting it would change the real account's records.

// Options come from environment variables: mongosh does not reliably share a variable
// set with --eval with the --file script. In PowerShell:  $env:ACCOUNT = "..."
const option = (name) => {
  const fromEnv = typeof process !== "undefined" && process.env ? process.env[name] : undefined;
  if (fromEnv !== undefined && fromEnv !== "") return fromEnv;
  return typeof globalThis[name] !== "undefined" ? String(globalThis[name]) : undefined;
};

const TEST_NAME = /^Review [A-Z]+ \d{13}$/;      // the names the Postman collection gives
const TEST_DEBT = /^Review .*\d{13}$/;

const refId = (ref) => {
  if (ref === null || ref === undefined) return null;
  const id = ref.oid ?? ref.$id ?? ref._id ?? ref;
  return id === null || id === undefined ? null : String(id);
};

const inShell = typeof db !== "undefined" && typeof db.getName === "function";

if (inShell) {
  const apply = option("APPLY") === "true";
  const accounts = db.accounts.find().toArray().filter((a) => TEST_NAME.test(a.name ?? ""));
  const ids = new Set(accounts.map((a) => String(a._id)));
  const objectIds = accounts.map((a) => a._id);
  const isTest = (ref) => ids.has(refId(ref));

  const transfers = db.transfers.find().toArray();
  const mixed = transfers.filter((t) => isTest(t.sourceAccount) !== isTest(t.destinationAccount)
    && refId(t.sourceAccount) && refId(t.destinationAccount));
  const testTransfers = transfers.filter((t) => (isTest(t.sourceAccount) || isTest(t.destinationAccount)) && !mixed.includes(t));
  const debts = db.debts.find().toArray().filter((d) => TEST_DEBT.test(d.name ?? ""));
  const debtIds = new Set(debts.map((d) => String(d._id)));

  const byAccount = (collection, field) =>
    collection.find().toArray().filter((r) => isTest(r[field])).map((r) => r._id);

  const plan = {
    accounts: objectIds,
    incomes: byAccount(db.incomes, "account"),
    expenses: byAccount(db.expenses, "account"),
    transfers: testTransfers.map((t) => t._id),
    schedules: db.schedules.find().toArray().filter((s) => isTest(s.sourceAccount) || isTest(s.destinationAccount)).map((s) => s._id),
    debtMovement: db.debtMovement.find().toArray().filter((m) => isTest(m.account) || debtIds.has(refId(m.debt))).map((m) => m._id),
    debts: debts.map((d) => d._id),
  };

  print(`Test accounts found: ${accounts.length}`);
  accounts.forEach((a) => print(`  ${a._id}  ${a.name}  ${a.currency}  balance ${a.totalBalance}`));
  print("To delete: " + JSON.stringify(Object.fromEntries(Object.entries(plan).map(([k, v]) => [k, v.length]))));
  if (mixed.length) {
    print(`KEPT (between a test account and a real one), please look: ${mixed.length}`);
    mixed.forEach((t) => print(`  transfer ${t._id}: ${refId(t.sourceAccount)} -> ${refId(t.destinationAccount)}  ${t.amountSent}`));
  }

  if (!apply) {
    print("Nothing deleted. Set $env:APPLY = \"true\" and run again to delete.");
  } else {
    const collections = { accounts: db.accounts, incomes: db.incomes, expenses: db.expenses, transfers: db.transfers,
      schedules: db.schedules, debtMovement: db.debtMovement, debts: db.debts };
    for (const [name, list] of Object.entries(plan)) {
      if (list.length) collections[name].deleteMany({ _id: { $in: list } });
    }
    print("Deleted.");
  }
}
