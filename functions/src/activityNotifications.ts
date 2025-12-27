import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";
import * as logger from "firebase-functions/logger";
import {sendFcmNotification} from "./shared/fcmSender";

export const onActivityCreated = functions.firestore
  .onDocumentCreated("activities/{activityId}",
    async (event) => {
      try {
        const activitySnapshot = event.data;

        if (!activitySnapshot) {
          logger.error("No activity data in snapshot");
          return;
        }

        const activityData = activitySnapshot.data();
        const activityId = event.params.activityId;

        logger.info(`Processing new activity: ${activityId},
          type: ${activityData.activityType}`);

        const {
          actorUid,
          actorName,
          involvedUids,
          activityType,
          displayText,
          groupName,
          totalAmount,
        } = activityData;

        if (!actorUid ||
          !involvedUids ||
          involvedUids.length === 0
        ) {
          logger
            .warn(`Activity ${activityId} missing required fields. Skipping.`);
          return;
        }

        const usersToNotify = involvedUids
          .filter((uid: string) => uid !== actorUid);

        if (usersToNotify.length === 0) {
          logger
            .info(`No users to notify for activity ${activityId}`);
          return;
        }

        logger
          .info(`Notifying ${usersToNotify.length}
              users for activity ${activityId}`);

        const {title, body} = buildNotificationContent(
          activityType,
          actorName,
          displayText,
          groupName,
          totalAmount
        );

        const notificationPromise = usersToNotify
          .map(async (userId: string) => {
            try {
              const notificationRef = admin.firestore()
                .collection("users")
                .doc(userId)
                .collection("notifications")
                .doc();

              const notificationData = {
                id: notificationRef.id,
                userId: userId,
                activityId: activityId,
                title: title,
                message: body,
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                isRead: false,
                type: "ACTIVITY",
              };

              await notificationRef.set(notificationData);
              logger.info(`Notification document created for user ${userId}`);

              const fcmResult = await sendFcmNotification(
                userId,
                title,
                body,
                {
                  activityId: activityId,
                  activityType: activityType,
                  notificationId: notificationRef.id,
                }
              );

              if (fcmResult.success) {
                logger.info(`FCM sent successfully to user ${userId}`);
              } else {
                logger
                  .error(`Error notifying user ${userId}: ${fcmResult.error}`);
              }
            } catch (error) {
              logger
                .error(`Error notifying user ${userId}:`, error);
            }
          });

        await Promise.all(notificationPromise);
        logger
          .info(`Activity notifications processing complete for ${activityId}`);
      } catch (error) {
        logger
          .error("Error in onActivityCreated function:", error);
      }
    }
  );

/**
 * Helper function to build notification title and body based on activity type.
 *@param {string} activityType Type of activity
 *@param {string} actorName Name of the actor performing the activity
 *@param {string} displayText Display text associated with the activity
 *@param {string} groupName Group name associated with the activity
 *@param {number} totalAmount Total amount associated with the activity
 *@return {object} Object containing notification title and body
 */
function buildNotificationContent(
  activityType: string,
  actorName: string,
  displayText: string,
  groupName: string | undefined,
  totalAmount: number | undefined

): { title: string, body: string} {
  const groupContext = groupName ? ` in ${groupName}` : "";
  const amountText = totalAmount ? `MYR${totalAmount.toFixed(2)}` : "";

  switch (activityType) {
  case "EXPENSE_ADDED":
    return {
      title: "New Expense Added",
      body: `${actorName} added "${displayText}"${amountText}${groupContext}`,
    };

  case "PAYMENT_MADE":
    return {
      title: "Payment Recorded",
      body: `${actorName} recorded a payment${amountText}${groupContext}`,
    };

  case "EXPENSE_UPDATED":
    return {
      title: "Expense Updated",
      body: `${actorName} updated "${displayText}"${groupContext}`,
    };

  case "EXPENSE_DELETED":
    return {
      title: "Expense Deleted",
      body: `${actorName} deleted "${displayText}"${groupContext}`,
    };

  case "GROUP_CREATED":
    return {
      title: "New Group Created",
      body: `${actorName} created a new group "${displayText}"`,
    };

  case "MEMBER_ADDED":
    return {
      title: "Added to Group",
      body: `${actorName} added you to "${groupName}"`,
    };

  case "MEMBER_REMOVED":
    return {
      title: "Removed from Group",
      body: `You were removed from "${groupName}"`,
    };

  case "MEMBER_LEFT":
    return {
      title: "Member left group",
      body: `${actorName} left "${groupName}"`,
    };

  default:
    return {
      title: "SplitPay Update",
      body: `${actorName} made a change${groupContext}`,

    };
  }
}
