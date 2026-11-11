// node scripts/migration/number-transactions.test.js
const assert = require("assert");
const { planNumbers } = require("./number-transactions.js");

const u = (id) => ({ oid: id });
const at = (s) => new Date(s);
const data = {
    incomes: [
        { _id: "i1", user: u("A"), createdAt: at("2026-01-03") },
        { _id: "mirror", user: u("A"), createdAt: at("2026-01-05") },        // made by movement m1
    ],
    expenses: [{ _id: "e1", user: u("A"), createdAt: at("2026-01-01") }],
    transfers: [
        { _id: "t1", user: u("A"), createdAt: at("2026-01-02") },
        { _id: "t9", user: u("B"), createdAt: at("2026-01-01") },
    ],
    movements: [{ _id: "m1", user: u("A"), date: at("2026-01-05"), mirroredTransactionId: "mirror" }],
};

const { updates, counters } = planNumbers(data);
const numberOf = (kind, id) => updates[kind].find((x) => x._id === id)?.number;

// per user, in the order things happened
assert.strictEqual(numberOf("expenses", "e1"), 1);
assert.strictEqual(numberOf("transfers", "t1"), 2);
assert.strictEqual(numberOf("incomes", "i1"), 3);
assert.strictEqual(numberOf("movements", "m1"), 4);
// the movement's record has the movement's number
assert.strictEqual(numberOf("incomes", "mirror"), 4);
// another user starts at 1
assert.strictEqual(numberOf("transfers", "t9"), 1);
assert.deepStrictEqual(counters, { A: 4, B: 1 });

// numbers already given stay, and new ones continue after the highest
const again = planNumbers({ ...data, transfers: [{ _id: "t1", user: u("A"), createdAt: at("2026-01-02"), number: 10 }] });
assert.strictEqual(again.updates.transfers.length, 0);
assert.strictEqual(again.updates.expenses[0].number, 11);
assert.strictEqual(again.counters.A, 13);

console.log("number-transactions: all checks passed");