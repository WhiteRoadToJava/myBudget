const { expectedBalances, compare } = require("./verify-balances.js");
const assert = require("assert");
const ref = (id) => ({ oid: id });
// before: one currency per account
const before = {
  accounts: [ { _id: "A", name: "Main", currency: "SEK", balance: 100, totalBalance: 1100 - 300 - 15 + 0 - 50 + 200 },
              { _id: "B", name: "Euro", currency: "EUR", balance: 0, totalBalance: 87 - 2 },
              { _id: "C", name: "Cash", currency: "SEK", balance: 0, totalBalance: 999 } ],
  incomes: [ { _id: "i1", account: ref("A"), amount: 1000 }, { _id: "i2", account: ref("A"), amount: 200 /* mirror */ } ],
  expenses: [ { _id: "e1", account: ref("A"), amount: 300 } ],
  transfers: [ { sourceAccount: ref("A"), destinationAccount: ref("B"), amountSent: 0, amountReceived: 87,
                 sendFee: 15, sendFeeDirection: "CHARGE", sendFeeAccount: ref("A"), receiveFee: 2, receiveFeeDirection: "CHARGE", receiveFeeAccount: ref("B") } ],
  movements: [ { debt: ref("D1"), account: ref("A"), movementType: "DISBURSEMENT", amount: 200, mirroredTransactionId: "i2" },
               { debt: ref("D2"), account: ref("A"), movementType: "DISBURSEMENT", amount: 50 } ],
  debts: [ { _id: "D1", direction: "OWED_BY_ME", currency: "SEK" }, { _id: "D2", direction: "OWED_TO_ME", currency: "SEK" } ],
};
let rows = compare(before.accounts, expectedBalances(before));
assert.deepStrictEqual(rows.map(r => [r.accountId, r.expected, r.diff]), [["A", 935, 0], ["B", 85, 0], ["C", 0, 999]]);
// after: balances per currency, currency on records; exchange inside one account
const after = {
  accounts: [ { _id: "A", name: "Wallet", balances: [ { currency: "USD", openingBalance: 50, totalBalance: 40 }, { currency: "SYP", openingBalance: 0, totalBalance: 130000 } ] } ],
  incomes: [], expenses: [],
  transfers: [ { sourceAccount: ref("A"), sourceCurrency: "USD", destinationAccount: ref("A"), destinationCurrency: "SYP", amountSent: 10, amountReceived: 130000 } ],
  movements: [], debts: [],
};
rows = compare(after.accounts, expectedBalances(after));
assert.deepStrictEqual(rows.map(r => [r.currency, r.expected, r.diff]), [["USD", 40, 0], ["SYP", 130000, 0]]);
// the breakdown explains account A line by line
const { breakdown } = require("./verify-balances.js");
const b = breakdown(before, "A");
assert.strictEqual(b.incomes.total, 1000);
assert.strictEqual(b.incomes.skippedAsDebtMirror, 1);
assert.strictEqual(b.expenses.total, -300);
assert.strictEqual(b.fees, -15);
assert.deepStrictEqual(b.debtMovements.map((m) => m.amount), [200, -50]);
// a migrated account (with balances) and a transfer saved without currencies, from a deleted account:
// the transfer counts in the account's first currency
const mixed = {
  accounts: [{ _id: "B", name: "B", currency: "SEK", balances: [{ currency: "SEK", openingBalance: 1000, totalBalance: 1050 }] }],
  incomes: [], expenses: [], movements: [], debts: [],
  transfers: [{ sourceAccount: ref("GONE"), destinationAccount: ref("B"), amountSent: 50, amountReceived: 50 }],
};
rows = compare(mixed.accounts, expectedBalances(mixed));
assert.deepStrictEqual(rows.map((r) => [r.currency, r.expected, r.diff]), [["SEK", 1050, 0]]);
console.log("verify-balances: all checks passed");
