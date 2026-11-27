package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;

import java.util.Map;

/**
 * Dispatches Email and WhatsApp notifications to all active supervisors
 * whenever an Out-of-Office Notification is created or a Report Back is submitted.
 */
public interface SupervisorNotificationService {

    /**
     * Notify the selected Unit Head (Email + WhatsApp) to review and accept/reject a new Out-of-Office Notification
     * (falls back to active supervisors if no Unit Head was specified).
     */
    void notifyOutOfOfficeCreated(OutOfOfficeNotification notification);

    /**
     * Notify all active Overall Heads (Email + WhatsApp — informational only, no accept/reject required)
     * once the Unit Head has viewed and accepted the Out-of-Office Notification.
     */
    void notifyOverallHeadsAfterUnitHeadAcceptance(OutOfOfficeNotification notification);

    /**
     * Notify both the selected Unit Head and all active Overall Heads (Email + WhatsApp)
     * when the officer returns and submits their Report Back.
     */
    void notifyReportBackSubmitted(OutOfOfficeNotification notification, OutOfOfficeReportBack reportBack);

    /**
     * Fetch phone numbers for all active supervisors and dispatch a WhatsApp notification via Meta Cloud API.
     */
    Map<String, Object> notifyActiveSupervisorsViaWhatsApp(String customMessage);
}

