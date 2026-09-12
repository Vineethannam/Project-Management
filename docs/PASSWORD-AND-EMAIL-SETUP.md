# Password screens and Gmail setup

## Configure your local installation

1. Copy root `.env.example` to `.env` if not already done. Preserve existing database credentials.
2. Enable Google 2-Step Verification and create an App Password at https://myaccount.google.com/apppasswords. Some Google accounts do not offer App Passwords.
3. Set `MAIL_USERNAME` to your Gmail address, `MAIL_PASSWORD` to the 16-character App Password without spaces, and `MAIL_FROM` to the same Gmail address. Never use your normal Google password.
4. Keep `MAIL_HOST=smtp.gmail.com` and `MAIL_PORT=587`. The backend requires STARTTLS.
5. Set `MAIL_ENABLED=true`. Set `FRONTEND_URL=http://localhost:5173` for local development, or your HTTPS frontend address for deployment. This is where links open, not the Java API address.
6. For local HTTP only set `COOKIE_SECURE=false`. Use true with HTTPS.
7. Run `docker compose up -d --build api`. Flyway automatically applies the password migration. Database and upload volumes are preserved.
8. Start the frontend from `frontend/` with `SPRING_API_URL=http://localhost:8080 pnpm dev`. In PowerShell set `$env:SPRING_API_URL="http://localhost:8080"` before `pnpm dev`.

Docker Compose passes the root environment settings into Java. If running Java directly, export these variables in the Java process environment; Spring does not automatically read the root `.env`. Never commit `.env`, put mail credentials in frontend variables, or add them to the source ZIP.

## Screens

- `/login`: sign-in screen; includes Forgot password when the backend is connected.
- `/forgot-password`: request a reset email.
- `/reset-password#token=...`: opens from the email. The token is captured in memory and removed from the address bar. Refreshing requires reopening the original email link.
- `/change-password` (also linked in the sidebar): authenticated screen for current password, new password and confirmation. Available to every signed-in user regardless of business-module permissions.
- **Logout**: visible in the top bar and sidebar; invalidates the backend session. It then opens `/login`.

There is no demo mode. First startup creates your administrator, roles and settings only. Existing database records are preserved; no automated deletion is performed. Test fixtures exist only in test resources and are not packaged into the backend application.

## API and security

All POSTs require the session CSRF token from GET `/api/v1/auth/csrf`.

| Endpoint | JSON fields | Behaviour |
| --- | --- | --- |
| POST `/api/v1/auth/change-password` | currentPassword, newPassword | Requires an active authenticated session and correct current password. |
| POST `/api/v1/auth/forgot-password` | email | Always returns the same acknowledgement for existing, missing, inactive and throttled accounts. |
| POST `/api/v1/auth/reset-password` | token, newPassword | Requires an unused, unexpired token. |
| POST `/api/v1/auth/logout` | none | Ends the current session. |

New passwords require at least 12 characters and at most 72 UTF-8 bytes, matching BCrypt's input limit. Reset tokens contain 256 random bits. Only SHA-256 token hashes are stored. Tokens expire after 15 minutes and are consumed transactionally. Requesting another link replaces the previous one. Changing a password or an administrator changing a user's email/password invalidates outstanding reset links.

Password change/reset increments the account authentication version. Every protected API request checks that version and the active account status, so existing sessions become unusable. Reset and change confirmation email contains no password.

Forgot-password requests are limited to 5 per address and 100 globally per 15-minute window. Current-password checks are limited to 5 per account per 15-minute window. Rate counters persist in the database. SMTP reset delivery runs in a bounded background queue so the HTTP response does not wait for Gmail. Queued mail is not durable across a process crash; request a fresh link if necessary. SMTP failures are logged without recipients, tokens or credentials; there is no automatic delivery retry.

## Other emails

The same Gmail configuration sends notification emails for task, bug, assignment and course-enrollment assignments, assessment results, and assignment evaluations. In-app notifications are retained. Notification delivery begins only after the database transaction commits, and only for an active user's stored account email. Marking a notification as read does not send another email. Emails contain no passwords or reset secrets except the dedicated reset link.

SMTP is not a durable message queue: delivery failures are logged and do not undo business operations. Scheduled reminders and other event types are not added by this change.

## Verify locally

Create an active user with an inbox you control. Request a reset from `/forgot-password`, check inbox/spam, open the latest link, set a new password, and log in. Reusing the link must fail. Use Change password and check that other logged-in browser sessions can no longer access workspace data. Check Logout from both navigation locations.

If mail does not arrive, check MAIL_ENABLED, the App Password, MAIL_FROM and backend logs (`docker compose logs api`). The generic forgot-password response deliberately does not confirm account existence or email delivery. With MAIL_ENABLED=false, no reset token is issued and no mail is sent.

Automated tests cover password changes, single-use/expired tokens, session invalidation, rate limits, generic responses, and CSRF. Actual Gmail delivery requires your credentials and is not verified by those tests.
