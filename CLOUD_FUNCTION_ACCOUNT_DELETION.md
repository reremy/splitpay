# Firebase Cloud Function for Automatic Account Deletion

This document provides the Cloud Function code for automatically deleting user accounts after the 30-day grace period.

## Overview

The Cloud Function runs **daily** and checks for user accounts where `deletionScheduledAt` timestamp is older than 30 days. It then permanently deletes these accounts.

## Prerequisites

1. Firebase CLI installed: `npm install -g firebase-tools`
2. Firebase project initialized: `firebase init functions`
3. Node.js version 18+ (Firebase Functions Gen 2 requirement)

## Setup Instructions

### 1. Navigate to Functions Directory

```bash
cd functions
```

### 2. Install Required Dependencies

```bash
npm install firebase-admin firebase-functions@latest
```

### 3. Create the Cloud Function

Create a new file `functions/src/index.ts` (or add to existing) with the following code:

```typescript
import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";

// Initialize Firebase Admin SDK (only once)
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();
const auth = admin.auth();

/**
 * Scheduled function that runs daily to delete accounts past grace period
 * Runs every day at 2:00 AM UTC
 */
export const deleteExpiredAccounts = functions.scheduler.onSchedule(
  {
    schedule: "0 2 * * *", // Cron: Every day at 2:00 AM UTC
    timeZone: "UTC",
    memory: "256MiB",
    timeoutSeconds: 540, // 9 minutes max execution time
  },
  async (event) => {
    logger.info("Starting deleteExpiredAccounts function");

    try {
      const gracePeriodMs = 30 * 24 * 60 * 60 * 1000; // 30 days in milliseconds
      const now = Date.now();
      const cutoffTimestamp = now - gracePeriodMs;

      logger.info(`Checking for accounts scheduled before ${new Date(cutoffTimestamp).toISOString()}`);

      // Query users with deletionScheduledAt older than 30 days
      const usersSnapshot = await db.collection("users")
        .where("deletionScheduledAt", "<=", cutoffTimestamp)
        .get();

      if (usersSnapshot.empty) {
        logger.info("No accounts found for deletion");
        return null;
      }

      logger.info(`Found ${usersSnapshot.size} account(s) to delete`);

      const deletePromises = usersSnapshot.docs.map(async (doc) => {
        const userData = doc.data();
        const uid = doc.id;
        const deletionScheduledAt = userData.deletionScheduledAt;

        logger.info(`Processing deletion for user ${uid} (scheduled at ${new Date(deletionScheduledAt).toISOString()})`);

        try {
          // 1. Remove user from all friends' friend lists
          const friendUids = userData.friends || [];
          for (const friendUid of friendUids) {
            try {
              await db.collection("users").doc(friendUid).update({
                friends: admin.firestore.FieldValue.arrayRemove(uid)
              });
              logger.info(`Removed ${uid} from friend ${friendUid}'s friend list`);
            } catch (error) {
              logger.error(`Error removing ${uid} from friend ${friendUid}:`, error);
              // Continue with other friends even if one fails
            }
          }

          // 2. Leave all archived groups (active groups should already be left)
          // Note: We don't delete expenses - they remain with [Deleted User] displayed
          const groupsSnapshot = await db.collection("groups")
            .where("members", "array-contains", uid)
            .get();

          for (const groupDoc of groupsSnapshot.docs) {
            try {
              await groupDoc.ref.update({
                members: admin.firestore.FieldValue.arrayRemove(uid)
              });
              logger.info(`Removed ${uid} from group ${groupDoc.id}`);
            } catch (error) {
              logger.error(`Error removing ${uid} from group ${groupDoc.id}:`, error);
            }
          }

          // 3. Delete user document from Firestore
          await db.collection("users").doc(uid).delete();
          logger.info(`Deleted Firestore document for user ${uid}`);

          // 4. Delete Firebase Auth account
          await auth.deleteUser(uid);
          logger.info(`Deleted Firebase Auth account for user ${uid}`);

          logger.info(`✅ Successfully deleted account for user ${uid}`);

        } catch (error) {
          logger.error(`❌ Error deleting account for user ${uid}:`, error);
          // Continue with other users even if one fails
        }
      });

      await Promise.all(deletePromises);

      logger.info(`Account deletion job completed. Processed ${usersSnapshot.size} account(s)`);
      return null;

    } catch (error) {
      logger.error("Fatal error in deleteExpiredAccounts function:", error);
      throw error; // Re-throw to mark function as failed
    }
  }
);
```

### 4. Update package.json

Ensure your `functions/package.json` has these dependencies:

```json
{
  "name": "functions",
  "scripts": {
    "build": "tsc",
    "serve": "npm run build && firebase emulators:start --only functions",
    "shell": "npm run build && firebase functions:shell",
    "start": "npm run shell",
    "deploy": "firebase deploy --only functions",
    "logs": "firebase functions:log"
  },
  "engines": {
    "node": "18"
  },
  "main": "lib/index.js",
  "dependencies": {
    "firebase-admin": "^12.0.0",
    "firebase-functions": "^5.0.0"
  },
  "devDependencies": {
    "typescript": "^5.0.0"
  }
}
```

### 5. Configure TypeScript (if using TypeScript)

Create/update `functions/tsconfig.json`:

```json
{
  "compilerOptions": {
    "module": "commonjs",
    "noImplicitReturns": true,
    "noUnusedLocals": true,
    "outDir": "lib",
    "sourceMap": true,
    "strict": true,
    "target": "es2017"
  },
  "compileOnSave": true,
  "include": ["src"]
}
```

## Deployment

### 1. Build the Function

```bash
npm run build
```

### 2. Deploy to Firebase

```bash
firebase deploy --only functions:deleteExpiredAccounts
```

### 3. Verify Deployment

Check Firebase Console > Functions to see the deployed function.

## Testing

### Test Locally with Emulator

```bash
# Start emulator
firebase emulators:start

# Manually trigger function (in another terminal)
curl -X POST "http://localhost:5001/YOUR_PROJECT_ID/us-central1/deleteExpiredAccounts"
```

### Test in Production (Manual Trigger)

You can manually trigger the function from Firebase Console:
1. Go to Firebase Console > Functions
2. Find `deleteExpiredAccounts`
3. Click "Run" to manually trigger

Or use Firebase CLI:

```bash
firebase functions:shell
# Then in the shell:
deleteExpiredAccounts()
```

## Monitoring

### View Logs

```bash
firebase functions:log --only deleteExpiredAccounts
```

Or check Firebase Console > Functions > Logs

### Set Up Alerts

1. Go to Firebase Console > Functions
2. Click on `deleteExpiredAccounts`
3. Set up alerts for errors or failures

## Schedule Details

- **Frequency:** Daily at 2:00 AM UTC
- **Cron Expression:** `0 2 * * *`
- **Timezone:** UTC
- **Timeout:** 9 minutes
- **Memory:** 256 MiB

### Modify Schedule

To change the schedule, update the `schedule` parameter:

```typescript
schedule: "0 3 * * *",  // 3:00 AM UTC
schedule: "0 */12 * * *",  // Every 12 hours
schedule: "0 0 * * 0",  // Weekly on Sunday at midnight
```

## Cost Estimation

- **Function Invocations:** 1 per day = ~30/month
- **Firestore Reads:** ~1 per user check
- **Firestore Writes:** ~2-5 per deleted user (friends, groups)
- **Auth Deletion:** Free

**Estimated Cost:** <$0.10/month for typical usage

## Security

- Function uses Firebase Admin SDK with full permissions
- Only runs on server-side (secure)
- No API endpoints exposed to clients
- Authenticated via service account

## Troubleshooting

### Function Not Running

1. Check Firebase Console > Functions > Logs for errors
2. Verify schedule is correct: `firebase functions:config:get`
3. Ensure billing is enabled (required for scheduled functions)

### Users Not Being Deleted

1. Check Firestore rules allow admin SDK access
2. Verify `deletionScheduledAt` field exists and is a timestamp
3. Check function logs for specific errors

### Permission Errors

Ensure the Firebase Admin SDK service account has these permissions:
- Cloud Firestore: Read/Write
- Firebase Authentication: User Management

## Rollback

If you need to disable the function:

```bash
firebase functions:delete deleteExpiredAccounts
```

## Additional Notes

- **Grace Period:** The 30-day grace period is hardcoded in the function. Modify `gracePeriodMs` if needed.
- **Batch Processing:** The function processes all expired accounts in one run. For very large numbers, consider batching.
- **Idempotency:** The function is idempotent - safe to run multiple times.
- **Data Retention:** Expenses are NOT deleted - they show as [Deleted User] in the app.

---

## Support

For questions or issues:
1. Check Firebase Functions documentation: https://firebase.google.com/docs/functions
2. Check Firebase Console logs
3. Review function execution history

