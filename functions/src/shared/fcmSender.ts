import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";


interface FirebaseError {
  code?: string;
  message: string;
}

/**
 * Helper function to build notification title and body based on activity type.
 *@param {string} userId
 *@param {string} title title of the notification
 *@param {string} body body of the notification
 *@param {Object} data data within the body of the notification
 *@return {Promise} Object containing boolean success and error message
 */
export async function sendFcmNotification(
  userId: string,
  title: string,
  body: string,
  data: { [key: string]: string } = {}
): Promise<{ success: boolean, error?: string }> {
  try {
    logger.info(`Attempting to send FCM notification to user: ${userId}`);

    const userDoc = await admin.firestore()
      .collection("users")
      .doc(userId)
      .get();

    if (!userDoc.exists) {
      logger.warn(`User not found: ${userId}`);
      return {success: false, error: "User not found"};
    }

    const userData = userDoc.data();
    const fcmToken = userData?.fcmToken;

    if ( !fcmToken || fcmToken === "" ) {
      logger.warn(`No FCM token found for user: ${userId}`);
      return {success: false, error: "No FCM token found"};
    }

    const message: admin.messaging.Message = {
      token: fcmToken,
      notification: {
        title: title,
        body: body,
      },
      data: {
        ...data,
        click_action: "ACTIVITY_TAB",
        timestamp: Date.now().toString(),
      },
      android: {
        priority: "high",
        notification: {
          channelId: "splitpay_notifications",
          sound: "default",
          priority: "high",
        },
      },
    };

    const response = await admin.messaging().send(message);
    logger
      .info(`FCM notification sent successfully to ${userId}.
        Message ID: ${response}`);

    return {success: true};
  } catch (error: unknown) {
    const firebaseError = error as FirebaseError;
    if ( firebaseError.code === "messaging/invalid-registration-token" ||
        firebaseError.code === "messaging/registration-token-not-registered" ) {
      logger
        .warn(`Invalid or expired FCM token for user ${userId}
            . Clearing token.`);

      await admin.firestore()
        .collection("users")
        .doc(userId)
        .update({fcmToken: ""})
        .catch((err) =>
          logger
            .error("failed to clear invalid token:", err));

      return {success: false, error: "Invalid FCM token (cleared)"};
    }

    logger.error(`Error sending FCM notification to ${userId}:`, error);
    return {success: false, error: firebaseError.message};
  }
}

