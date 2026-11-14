// node scripts/migration/migrate-to-multi-currency.test.js
const assert = require("assert");
const { planMigration } = require("./migrate-to-multi-currency.js");
const { expectedBalances, compare } = require("./verify-balances.js");

const ref = (id) => ({ oid: id });
const data = {
  accounts: [
    { _id: "A", name: "Main", currency: "SEK", balance: 100, totalBalance: 1080 },
    { _id: "B", name: "Euro", currency: "EUR", balance: 0, totalBalance: 85 },
    { _id: "X", name: "Broken" },
  ],
  incomes: [{ _id: "i1", account: ref("A"), amount: 1000 }, { _id: "i2", account: ref("GONE"), amount: 5 }],
  expenses: [{ _id: "e1", account: ref("A"), amount: 5 }],
  transfers: [{ _id: "t1", sourceAccount: ref("A"), destinationAccount: ref("B"), amountSent: 0, amountReceived: 87,
    sendFee: 15, sendFeeDirection: "CHARGE", sendFeeAccount: ref("A"),
    receiveFee: 2, receiveFeeDirection: "CHARGE", receiveFeeAccount: ref("B") }],
  schedules: [{ _id: "s1", sourceAccount: ref("A"), destinationAccount: ref("B") }, { _id: "s2", sourceAccount: ref("B") }],
  movements: [{ _id: "m1", debt: ref("D"), account: ref("B"), movementType: "DISBURSEMENT", amount: 1 }],
  debts: [{ _id: "D", direction: "OWED_BY_ME", currency: "SEK" }],
};

const plan = planMigration(data);

// accounts get one balance each, from their old fields
assert.deepStrictEqual(plan.accounts[0].set.balances, [{ currency: "SEK", openingBalance: 100, totalBalance: 1080 }]);
// records get the currency of their account; a deleted account gives null
assert.strictEqual(plan.incomes[0].set.currency, "SEK");
assert.strictEqual(plan.incomes[1].set.currency, null);
assert.deepStrictEqual(plan.transfers[0].set, { sourceCurrency: "SEK", destinationCurrency: "EUR", sendFeeCurrency: "SEK", receiveFeeCurrency: "EUR" });
assert.deepStrictEqual(plan.schedules[1].set, { sourceCurrency: "EUR", destinationCurrency: null });
// problems are reported, not hidden: an account without currency, a movement in another currency
assert.strictEqual(plan.problems.length, 2);

// after applying the plan, the balance check reads the new shape and finds the same numbers
const apply = (docs, updates) => docs.map((d) => ({ ...d, ...(updates.find((u) => u._id === d._id)?.set ?? {}) }));
const migrated = {
  accounts: apply(data.accounts.filter((a) => a.currency), plan.accounts),
  incomes: apply(data.incomes, plan.incomes),
  expenses: apply(data.expenses, plan.expenses),
  transfers: apply(data.transfers, plan.transfers),
  movements: [], debts: [],
};
const beforeRows = compare(data.accounts.filter((a) => a.currency), expectedBalances({ ...data, movements: [], debts: [] }));
const afterRows = compare(migrated.accounts, expectedBalances(migrated));
assert.deepStrictEqual(afterRows.map((r) => [r.accountId, r.expected, r.diff]), beforeRows.map((r) => [r.accountId, r.expected, r.diff]));

console.log("migrate-to-multi-currency: all checks passed");
