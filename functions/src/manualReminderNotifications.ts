import * as functions from "firebase-functions/v2";
import * as logger from "firebase-functions/logger";
import {sendFcmNotification} from "./shared/fcmSender";

export const sendManualReminder = functions.https.onCall(
  async (request) => {
    try {
      // 1. Validate authentication
      if (!request.auth) {
        logger.warn("Unauthenticated reminder request");
        throw new functions.https.HttpsError(
          "unauthenticated",
          "User must be authenticated"
        );
      }

      const senderUid = request.auth.uid;
      const senderName = request.auth.token.name || "A friend";

      // 2. Validate request data
      const {friendId, customMessage} = request.data;

      if (!friendId || typeof friendId !== "string") {
        logger.warn("Invalid friendId in reminder request");
        throw new functions.https.HttpsError(
          "invalid-argument",
          "friendId is required and must be a string"
        );
      }

      // Prevent self-reminders
      if (friendId === senderUid) {
        throw new functions.https.HttpsError(
          "invalid-argument",
          "Cannot send reminder to yourself"
        );
      }

      logger.info(`User ${senderUid} sending reminder to ${friendId}`);

      // 3. Build notification content
      const title = "Payment Reminder";
      const body = customMessage && customMessage.trim() !== "" ?
        customMessage :
        `${senderName} sent you a reminder about outstanding balances`;

      // 4. Send FCM notification
      const fcmResult = await sendFcmNotification(
        friendId,
        title,
        body,
        {
          type: "REMINDER",
          senderId: senderUid,
          senderName: senderName,
        }
      );

      if (fcmResult.success) {
        logger
          .info(`Reminder sent successfully from ${senderUid} to ${friendId}`);
        return {
          success: true,
          message: "Reminder sent successfully",
        };
      } else {
        logger.warn(`Failed to send reminder: ${fcmResult.error}`);
        throw new functions.https.HttpsError(
          "internal",
          `Failed to send reminder: ${fcmResult.error}`
        );
      }
    } catch (error: unknown) {
      logger.error("Error in sendManualReminder:", error);

      // Re-throw HttpsErrors as-is
      if (error instanceof functions.https.HttpsError) {
        throw error;
      }

      // Wrap other errors
      throw new functions.https.HttpsError(
        "internal",
        "An error occurred while sending the reminder"
      );
    }
  }
);
