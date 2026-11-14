// Balance check for the multi-currency migration. Run with mongosh against a COPY:
//
//   mongosh "mongodb://localhost:27017/myBudget_migration_test" --file scripts/migration/verify-balances.js
//
// It recomputes every balance from the raw records and compares it with the stored one:
//   expected = opening balance
//            + incomes - expenses            (records made by a debt movement are skipped:
//                                              the movement itself moved the money)
//            - sent + received ± fees        (transfers, on the account and currency of each side)
//            ± debt movements                (disbursement / repayment, by debt direction)
//
// Before the migration an account has one currency ("currency", "totalBalance").
// After it, an account has "balances": [{ currency, openingBalance, totalBalance }] and every
// record says its currency; the script detects which shape it reads, per account.
//
// The result is printed and saved in the collection "migration_reports". Run it before the
// migration (the differences found then are known and kept) and after it: after the migration
// it only fails on a difference that is new or changed.

const TOLERANCE = 0.005;

// Options come from environment variables: mongosh does not reliably share a variable
// set with --eval with the --file script. In PowerShell:  $env:ACCOUNT = "..."
const option = (name) => {
  const fromEnv = typeof process !== "undefined" && process.env ? process.env[name] : undefined;
  if (fromEnv !== undefined && fromEnv !== "") return fromEnv;
  return typeof globalThis[name] !== "undefined" ? String(globalThis[name]) : undefined;
};

const round2 = (n) => Math.round(n * 100) / 100;

// a DBRef as the shell shows it (.oid), as stored ($id), or a plain id
const refId = (ref) => {
  if (ref === null || ref === undefined) return null;
  const id = ref.oid ?? ref.$id ?? ref._id ?? ref;
  return id === null || id === undefined ? null : String(id);
};

// ---------- pure computation (also used by the test) ----------

function expectedBalances({ accounts, incomes, expenses, transfers, movements, debts }) {
  const pockets = new Map(); // "accountId|CUR" -> expected
  const accountCurrency = new Map();
  const key = (accountId, currency) => `${accountId}|${currency}`;
  const add = (accountId, currency, amount) => {
    if (!accountId || !currency || !amount) return;
    const k = key(accountId, currency);
    pockets.set(k, round2((pockets.get(k) ?? 0) + amount));
  };

  for (const a of accounts) {
    const id = String(a._id);
    if (Array.isArray(a.balances)) {
      for (const b of a.balances) {
        pockets.set(key(id, b.currency), round2(Number(b.openingBalance ?? 0)));
      }
      // a record without currency (written by code from before the currencies) is in the
      // account's first currency, for a migrated account too
      const first = a.currency ?? a.balances[0]?.currency;
      if (first) accountCurrency.set(id, first);
    } else {
      accountCurrency.set(id, a.currency);
      pockets.set(key(id, a.currency), round2(Number(a.balance ?? 0)));
    }
  }
  // the record's currency after the migration, the account's currency before it
  const currencyOf = (record, accountId, field = "currency") => record[field] ?? accountCurrency.get(accountId);

  const mirrored = new Set(movements.map((m) => m.mirroredTransactionId).filter(Boolean).map(String));
  // a cancelled transaction moves no balance
  const active = (r) => r.cancelled !== true;
  incomes = incomes.filter(active);
  expenses = expenses.filter(active);
  transfers = transfers.filter(active);

  for (const r of incomes) {
    if (mirrored.has(String(r._id))) continue;
    const acc = refId(r.account);
    add(acc, currencyOf(r, acc), Number(r.amount ?? 0));
  }
  for (const r of expenses) {
    if (mirrored.has(String(r._id))) continue;
    const acc = refId(r.account);
    add(acc, currencyOf(r, acc), -Number(r.amount ?? 0));
  }
  for (const t of transfers) {
    const src = refId(t.sourceAccount);
    const dst = refId(t.destinationAccount);
    add(src, currencyOf(t, src, "sourceCurrency"), -Number(t.amountSent ?? 0));
    add(dst, currencyOf(t, dst, "destinationCurrency"), Number(t.amountReceived ?? 0));
    const fee = (amount, direction, accountRef, currencyField) => {
      const acc = refId(accountRef);
      const value = Number(amount ?? 0);
      add(acc, currencyOf(t, acc, currencyField), direction === "CREDIT" ? value : -value);
    };
    fee(t.sendFee, t.sendFeeDirection, t.sendFeeAccount, "sendFeeCurrency");
    fee(t.receiveFee, t.receiveFeeDirection, t.receiveFeeAccount, "receiveFeeCurrency");
  }
  const debtById = new Map(debts.map((d) => [String(d._id), d]));
  for (const m of movements) {
    const debt = debtById.get(refId(m.debt));
    const acc = refId(m.account);
    if (!debt || !acc) continue;
    // DebtDirection.cashFlowOf: in when (disbursement == borrowed)
    const inflow = (m.movementType === "DISBURSEMENT") === (debt.direction === "OWED_BY_ME");
    add(acc, m.currency ?? debt.currency ?? accountCurrency.get(acc), inflow ? Number(m.amount) : -Number(m.amount));
  }
  return pockets;
}

function compare(accounts, pockets) {
  const rows = [];
  for (const a of accounts) {
    const id = String(a._id);
    const stored = Array.isArray(a.balances)
      ? a.balances.map((b) => ({ currency: b.currency, value: Number(b.totalBalance ?? 0) }))
      : [{ currency: a.currency, value: Number(a.totalBalance ?? a.balance ?? 0) }];
    for (const s of stored) {
      const expected = pockets.get(`${id}|${s.currency}`) ?? 0;
      rows.push({
        accountId: id, name: a.name, currency: s.currency,
        stored: round2(s.value), expected: round2(expected), diff: round2(s.value - expected),
      });
    }
  }
  return rows;
}

// Where an account's expected balance comes from, line by line (for one account)
function breakdown(data, accountId) {
  const id = String(accountId);
  const account = data.accounts.find((a) => String(a._id) === id);
  if (!account) return null;
  const mirrored = new Set(data.movements.map((m) => m.mirroredTransactionId).filter(Boolean).map(String));
  const mine = (ref) => refId(ref) === id;
  const sum = (list, f) => round2(list.reduce((s, x) => s + Number(f(x) ?? 0), 0));
  const debtById = new Map(data.debts.map((d) => [String(d._id), d]));

  const incomes = data.incomes.filter((r) => mine(r.account));
  const expenses = data.expenses.filter((r) => mine(r.account));
  const sent = data.transfers.filter((t) => mine(t.sourceAccount));
  const received = data.transfers.filter((t) => mine(t.destinationAccount));
  const signedFee = (amount, dir) => (dir === "CREDIT" ? 1 : -1) * Number(amount ?? 0);
  const fees = round2(
    data.transfers.filter((t) => mine(t.sendFeeAccount)).reduce((s, t) => s + signedFee(t.sendFee, t.sendFeeDirection), 0)
    + data.transfers.filter((t) => mine(t.receiveFeeAccount)).reduce((s, t) => s + signedFee(t.receiveFee, t.receiveFeeDirection), 0));
  const movements = data.movements.filter((m) => mine(m.account)).map((m) => {
    const debt = debtById.get(refId(m.debt));
    const inflow = debt && (m.movementType === "DISBURSEMENT") === (debt.direction === "OWED_BY_ME");
    return { debt: debt?.name ?? refId(m.debt), type: m.movementType, amount: inflow ? Number(m.amount) : -Number(m.amount), mirrored: Boolean(m.mirroredTransactionId) };
  });

  return {
    account: account.name, currency: account.currency, storedBalance: account.totalBalance, openingBalance: account.balance,
    incomes: { count: incomes.length, total: sum(incomes.filter((r) => !mirrored.has(String(r._id))), (r) => r.amount),
      skippedAsDebtMirror: incomes.filter((r) => mirrored.has(String(r._id))).length },
    expenses: { count: expenses.length, total: -sum(expenses.filter((r) => !mirrored.has(String(r._id))), (r) => r.amount),
      skippedAsDebtMirror: expenses.filter((r) => mirrored.has(String(r._id))).length },
    transfersOut: { count: sent.length, total: -sum(sent, (t) => t.amountSent) },
    transfersIn: { count: received.length, total: sum(received, (t) => t.amountReceived) },
    fees,
    debtMovements: movements,
    records: {
      incomes: incomes.map((r) => ({ id: String(r._id), amount: r.amount, at: r.createdAt, category: r.category, mirror: mirrored.has(String(r._id)) })),
      expenses: expenses.map((r) => ({ id: String(r._id), amount: r.amount, at: r.createdAt, category: r.category, mirror: mirrored.has(String(r._id)) })),
      transfers: [...sent, ...received].map((t) => ({ id: String(t._id), sent: mine(t.sourceAccount) ? t.amountSent : null,
        received: mine(t.destinationAccount) ? t.amountReceived : null, at: t.createdAt,
        otherSide: mine(t.sourceAccount) ? refId(t.destinationAccount) : refId(t.sourceAccount) })),
    },
  };
}

// ---------- shell entry ----------

// mongosh also defines "module", so the shell is recognised by its "db" object instead
const inShell = typeof db !== "undefined" && typeof db.getName === "function";

if (inShell) {
  const data = {
    accounts: db.accounts.find().toArray(),
    incomes: db.incomes.find().toArray(),
    expenses: db.expenses.find().toArray(),
    transfers: db.transfers.find().toArray(),
    movements: db.debtMovement.find().toArray(),
    debts: db.debts.find().toArray(),
  };
  // details for one account:  $env:ACCOUNT = "<accountId>"
  const account = option("ACCOUNT");
  if (account) {
    const details = breakdown(data, account);
    if (details) printjson(details);
    else print(`No account with id ${account}`);
  } else {
  const rows = compare(data.accounts, expectedBalances(data));
  const wrong = rows.filter((r) => Math.abs(r.diff) > TOLERANCE);
  const phase = data.accounts.some((a) => Array.isArray(a.balances)) ? "after" : "before";

  print(`Balance check (${phase}): ${rows.length} balances, ${wrong.length} different`);

  // Differences that existed before the migration are known and kept on purpose (the balances
  // the users see stay as they are). After the migration they must be exactly the same: same
  // account, same currency, same amount. Anything new or changed is a migration error.
  const baseline = phase === "after"
    ? db.migration_reports.find({ phase: "before", database: db.getName() }).sort({ at: -1 }).limit(1).toArray()[0]
    : null;
  const known = new Map((baseline?.wrong ?? []).map((w) => [`${w.accountId}|${w.currency}`, w.diff]));
  const unexpected = wrong.filter((w) => known.get(`${w.accountId}|${w.currency}`) !== w.diff);

  db.migration_reports.insertOne({ phase, at: new Date(), database: db.getName(), checked: rows.length, wrong, unexpected, rows });
  if (phase === "after" && baseline) {
    if (unexpected.length) {
      print(`FAILED: ${unexpected.length} difference(s) that were not there before the migration:`);
      printjson(unexpected);
    } else {
      print(`OK: no new difference; the ${wrong.length} known one(s) from before the migration are unchanged`);
    }
  } else {
    if (wrong.length) printjson(wrong);
    print(wrong.length
      ? "Differences found (listed above). They are kept as they are and will be compared after the migration."
      : "OK: every balance matches its records");
  }
  }
}

if (!inShell && typeof module !== "undefined") module.exports = { expectedBalances, compare, refId, breakdown };
