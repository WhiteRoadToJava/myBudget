# Multi-currency migration: working on a copy

Everything here runs against a **copy** of the database. The real database is only
touched on the day of the release, after the whole procedure has passed on the copy.

## 0. Safety first

When the app runs against the copy, the hourly Atlas sync would push the copy to Atlas
(and delete there what the copy does not have). In `.env` while testing:

```
MONGODB_LOCAL_DB=myBudget_migration_test
APP_SYNC_ENABLED=false
```

## 1. Make the copy

```
mongodump --uri="mongodb://localhost:27017" --db=local_test_db --out=backup-before-migration
mongorestore --uri="mongodb://localhost:27017" --drop \
  --nsFrom="local_test_db.*" --nsTo="myBudget_migration_test.*" backup-before-migration
```

Start again at any time: run `mongorestore` again (`--drop` replaces the copy).

## 2. Check the balances before anything changes

```
mongosh "mongodb://localhost:27017/myBudget_migration_test" --file scripts/migration/verify-balances.js
```

`OK` means every stored balance matches its records. Any difference is listed with the
account, currency and amount; it is an old error, to fix or explain before migrating.
Every run is saved in the collection `migration_reports`.

## 3. Test of the check itself

```
node scripts/migration/verify-balances.test.js
```

## 4. Migrate the copy (step 1: add the new fields)

```
mongosh "<copy uri>" --file scripts/migration/migrate-to-multi-currency.js
$env:APPLY = "true"; mongosh "<copy uri>" --file scripts/migration/migrate-to-multi-currency.js; Remove-Item Env:APPLY
mongosh "mongodb://localhost:27017/myBudget_migration_test" --file scripts/migration/verify-balances.js
```

The first line only counts (dry run is the default). The second adds the fields; nothing old is removed, so it can be
run again at any time. The third must print `OK`, with the same balances as in step 2.
Test of the migration logic: `node scripts/migration/migrate-to-multi-currency.test.js`

## When the balance check finds differences

Details of one account (where its expected balance comes from, record by record):
```
$env:ACCOUNT = "<accountId>"; mongosh "<copy uri>" --file scripts/migration/verify-balances.js; Remove-Item Env:ACCOUNT
```
Accounts left by Postman runs ("Review B 1793189620310"): list, then delete with APPLY:
```
mongosh "<copy uri>" --file scripts/migration/cleanup-test-accounts.js
$env:APPLY = "true"; mongosh "<copy uri>" --file scripts/migration/cleanup-test-accounts.js; Remove-Item Env:APPLY
```

Options are environment variables (mongosh does not reliably pass `--eval` variables to the file).
Always remove them afterwards (`Remove-Item Env:APPLY`), so the next run is a dry run again.
Tests of the scripts: `node scripts/migration/shell-options.test.js` (and the two other *.test.js).

## Settling the differences

One decision per account: `records` (the records are right, the stored balance is corrected)
or `stored` (the balance in the app is right, an "Adjustment" record is added):
```
$env:DECISIONS = '{"<accountId>":"stored","<accountId>":"records"}'
mongosh "<copy uri>" --file scripts/migration/reconcile-balances.js
$env:APPLY = "true"; mongosh "<copy uri>" --file scripts/migration/reconcile-balances.js; Remove-Item Env:APPLY
Remove-Item Env:DECISIONS
```
