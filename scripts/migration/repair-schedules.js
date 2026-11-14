// Repairs schedules saved while Schedule.java had its @DBRef on the wrong field
// (on sourceCurrency instead of user, phases 1 to 4 of the multi-currency branch):
//   - user was stored as an embedded document instead of a reference  -> becomes a reference again
//   - sourceCurrency may have been stored as a reference                -> becomes the plain code again
// Only the copy used for testing can have such schedules; the real database never ran that code.
//
//   mongosh "<copy uri>" --file scripts/migration/repair-schedules.js                 (shows what it would do)
//   $env:APPLY = "true"; mongosh "<copy uri>" --file scripts/migration/repair-schedules.js; Remove-Item Env:APPLY

const option = (name) => {
  const fromEnv = typeof process !== "undefined" && process.env ? process.env[name] : undefined;
  if (fromEnv !== undefined && fromEnv !== "") return fromEnv;
  return typeof globalThis[name] !== "undefined" ? String(globalThis[name]) : undefined;
};

const inShell = typeof db !== "undefined" && typeof db.getName === "function";

if (inShell) {
  const apply = option("APPLY") === "true";
  let fixed = 0;
  db.schedules.find().forEach((s) => {
    const set = {};
    // an embedded user has its own _id; a reference has $id (or oid in the shell)
    if (s.user && s.user._id !== undefined && s.user.oid === undefined && s.user.$id === undefined) {
      set.user = new DBRef("users", s.user._id);
    }
    if (s.sourceCurrency && typeof s.sourceCurrency === "object") {
      set.sourceCurrency = String(s.sourceCurrency.oid ?? s.sourceCurrency.$id ?? "");
    }
    if (Object.keys(set).length) {
      fixed++;
      print(`schedule ${s._id} (${s.name}): ${Object.keys(set).join(", ")}`);
      if (apply) db.schedules.updateOne({ _id: s._id }, { $set: set });
    }
  });
  print(fixed === 0 ? "Nothing to repair." : apply ? `Repaired ${fixed} schedule(s).` : `${fixed} to repair. Set $env:APPLY = "true" to repair.`);
}
