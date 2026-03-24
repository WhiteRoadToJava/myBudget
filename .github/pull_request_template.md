---
name: 🚀 Pull Request Template
about: Standardize code changes for MyBudget
title: '[PR] - '
labels: enhancement, internal
assignees: ''

---

## 📝 Summary
Describe the changes in this PR.
*(e.g., "Updated AccountService to handle duplicate names and fixed ProtectedRoute casing")*

## 🔗 Related Issue
- Closes # (Link your issue number here)

## 🛠️ Type of Change
- [ ] 🐛 **Bug Fix** (Non-breaking change which fixes an issue)
- [ ] ✨ **New Feature** (Adds functionality to Frontend or Backend)
- [ ] 🧹 **Refactor** (Code cleanup, folder renaming, or CSS improvements)
- [ ] 🔒 **Security/Auth** (Changes to Spring Security, JWT, or ProtectedRoutes)

## ✅ Critical Checklist
- [ ] **Casing Check:** I verified that React file names (e.g., `Login.jsx`) match their imports exactly.
- [ ] **Backend Test:** I tested the API endpoints (Postman/Swagger) and they return correct Status Codes.
- [ ] **Database:** I checked that MongoDB isn't saving duplicate unique fields (like Account Name).
- [ ] **Build Check:** I ran `npm run build` or `mvn clean install` locally and it passed.

## 🧪 Testing Proof
**How did you test this?**
- [ ] Manual test in Browser
- [ ] Postman API request
- [ ] Unit Test (JUnit/Vitest)

> [!TIP]
> **Paste a screenshot of the successful result or the Terminal output below:**
> (Drop image here)

## 🌐 Deployment Impact
- [ ] Requires new Environment Variables (e.g., `JWT_SECRET`) on Railway.
- [ ] Requires a database cleanup/migration in MongoDB.