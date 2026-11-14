// Gives the existing transactions their reference numbers: per user, 1, 2, 3... in the order
// they happened (incomes, expenses, transfers and debt movements together), then sets the
// user's counter to the last number, so new transactions continue from there.
// The income/expense a debt movement created gets the number of that movement.
//
// Run it while the app is STOPPED (on the day of the release, before the new version starts):
//   mongosh "<db uri>" --file scripts/migration/number-transactions.js                  (counts only)
//   $env:APPLY = "true"; mongosh "<db uri>" --file scripts/migration/number-transactions.js; Remove-Item Env:APPLY
// It numbers only what has no number yet, and never changes a number already given.

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

// when it happened: createdAt, else a movement's date, else the time inside its ObjectId
const timeOf = (doc) => {
    if (doc.createdAt) return new Date(doc.createdAt).getTime();
    if (doc.date) return new Date(doc.date).getTime();
    if (doc._id && typeof doc._id.getTimestamp === "function") return doc._id.getTimestamp().getTime();
    return 0;
};

// ---------- pure planning (also used by the test) ----------

// returns { updates: { incomes: [{_id, number}], ... }, counters: { userId: last } }
function planNumbers({ incomes, expenses, transfers, movements }) {
    const updates = { incomes: [], expenses: [], transfers: [], movements: [] };
    const counters = {};
    const mirrorOf = new Map(movements.filter((m) => m.mirroredTransactionId)
        .map((m) => [String(m.mirroredTransactionId), m]));

    // the highest number each user already has (never reused)
    const all = [
        ...incomes.map((d) => ["incomes", d]), ...expenses.map((d) => ["expenses", d]),
        ...transfers.map((d) => ["transfers", d]), ...movements.map((d) => ["movements", d]),
    ];
    for (const [, d] of all) {
        const user = refId(d.user);
        if (user && d.number) counters[user] = Math.max(counters[user] ?? 0, Number(d.number));
    }

    // what gets a number of its own: everything without one, except the records of debt movements
    const toNumber = all
        .filter(([kind, d]) => !d.number && refId(d.user) && !(kind !== "movements" && mirrorOf.has(String(d._id))))
        .sort((a, b) => timeOf(a[1]) - timeOf(b[1]) || String(a[1]._id).localeCompare(String(b[1]._id)));

    const given = new Map(); // movement id -> number
    for (const [kind, d] of toNumber) {
        const user = refId(d.user);
        const number = (counters[user] ?? 0) + 1;
        counters[user] = number;
        updates[kind].push({ _id: d._id, number });
        if (kind === "movements") given.set(String(d._id), number);
    }

    // a debt movement's income/expense: the movement's number
    for (const [kind, list] of [["incomes", incomes], ["expenses", expenses]]) {
        for (const d of list) {
            const movement = mirrorOf.get(String(d._id));
            if (!movement || d.number) continue;
            const number = movement.number ?? given.get(String(movement._id));
            if (number) updates[kind].push({ _id: d._id, number });
        }
    }
    return { updates, counters };
}

// ---------- shell entry ----------

const inShell = typeof db !== "undefined" && typeof db.getName === "function";

if (inShell) {
    const apply = option("APPLY") === "true";
    const { updates, counters } = planNumbers({
        incomes: db.incomes.find().toArray(),
        expenses: db.expenses.find().toArray(),
        transfers: db.transfers.find().toArray(),
        movements: db.debtMovement.find().toArray(),
    });
    const counts = Object.fromEntries(Object.entries(updates).map(([k, v]) => [k, v.length]));
    print(`${apply ? "Numbering" : "DRY RUN (set $env:APPLY = \"true\" to write). Would number"}: ${JSON.stringify(counts)}`);
    print(`Users: ${Object.keys(counters).length}`);

    if (apply) {
        const collections = { incomes: db.incomes, expenses: db.expenses, transfers: db.transfers, movements: db.debtMovement };
        for (const [kind, list] of Object.entries(updates)) {
            const ops = list.map((u) => ({ updateOne: { filter: { _id: u._id, number: { $exists: false } }, update: { $set: { number: u.number } } } }));
            if (ops.length) collections[kind].bulkWrite(ops, { ordered: false });
        }
        // the counter never goes down: $max keeps a higher value already there
        for (const [user, last] of Object.entries(counters)) {
            db.counters.updateOne({ _id: `transactions:${user}` }, { $max: { seq: last } }, { upsert: true });
        }
        print("Done.");
    }
}

if (!inShell && typeof module !== "undefined") module.exports = { planNumbers };