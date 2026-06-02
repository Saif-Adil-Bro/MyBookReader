import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ── Send notification when admin_notifications doc is created ─────────────
export const onAdminNotification = functions.firestore
  .document("admin_notifications/{notifId}")
  .onCreate(async (snap) => {
    const data = snap.data();
    const { title, body, targetUserIds } = data;

    if (!title || !body) return;

    try {
      if (targetUserIds && targetUserIds.length > 0) {
        // Send to specific users
        const tokens: string[] = [];
        for (const uid of targetUserIds) {
          const userDoc = await db.collection("users").doc(uid).get();
          const token = userDoc.data()?.fcmToken;
          if (token) tokens.push(token);
        }
        if (tokens.length > 0) {
          await messaging.sendEachForMulticast({ tokens, notification: { title, body } });
        }
      } else {
        // Broadcast to all via topic
        await messaging.send({
          topic: "all",
          notification: { title, body },
          android: {
            priority: "high",
            notification: {
              channelId: "general",
              clickAction: "FLUTTER_NOTIFICATION_CLICK",
            },
          },
        });
      }
      functions.logger.info(`Notification sent: ${title}`);
    } catch (err) {
      functions.logger.error("Failed to send notification:", err);
    }
  });

// ── New book trigger: notify subscribers ──────────────────────────────────
export const onNewBook = functions.firestore
  .document("books/{bookId}")
  .onCreate(async (snap) => {
    const book = snap.data();
    if (!book.active) return;

    await messaging.send({
      topic: "all",
      notification: {
        title: "📚 New Book Added!",
        body: `"${book.title}" by ${book.author} is now available.`,
      },
      data: {
        type: "new_book",
        bookId: snap.id,
      },
    });
    functions.logger.info(`New book notification sent for: ${book.title}`);
  });

// ── Daily reading reminder (scheduled) ───────────────────────────────────
export const dailyReadingReminder = functions.pubsub
  .schedule("0 18 * * *")     // 6 PM every day
  .timeZone("Asia/Dhaka")
  .onRun(async () => {
    await messaging.send({
      topic: "all",
      notification: {
        title: "⏰ Reading Reminder",
        body: "Time for your daily reading! Keep your streak alive 🔥",
      },
      data: { type: "reminder" },
    });
    functions.logger.info("Daily reading reminder sent");
  });

// ── Update download stats ─────────────────────────────────────────────────
export const onDownloadIncrement = functions.firestore
  .document("books/{bookId}")
  .onUpdate(async (change) => {
    const before = change.before.data();
    const after  = change.after.data();
    if (before.downloadCount !== after.downloadCount) {
      functions.logger.info(
        `Book ${change.after.id} downloads: ${after.downloadCount}`
      );
    }
  });

// ── Clean up old notifications ────────────────────────────────────────────
export const cleanupOldNotifications = functions.pubsub
  .schedule("0 2 * * 0")    // Every Sunday at 2 AM
  .onRun(async () => {
    const cutoff = admin.firestore.Timestamp.fromDate(
      new Date(Date.now() - 30 * 24 * 60 * 60 * 1000) // 30 days ago
    );
    const old = await db.collection("admin_notifications")
      .where("createdAt", "<", cutoff)
      .get();
    const batch = db.batch();
    old.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();
    functions.logger.info(`Deleted ${old.size} old notifications`);
  });

// ── FCM Token updater ─────────────────────────────────────────────────────
export const updateFcmToken = functions.https.onCall(async (data, context) => {
  if (!context.auth) throw new functions.https.HttpsError("unauthenticated", "Login required");
  const { token } = data;
  if (!token) throw new functions.https.HttpsError("invalid-argument", "Token required");
  await db.collection("users").doc(context.auth.uid).update({ fcmToken: token });
  return { success: true };
});
