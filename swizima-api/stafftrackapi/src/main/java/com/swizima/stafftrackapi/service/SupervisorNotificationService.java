package com.swizima.stafftrackapi.service;

import com.swizima.stafftrackapi.entity.OutOfOfficeNotification;
import com.swizima.stafftrackapi.entity.OutOfOfficeReportBack;

/**
 * Dispatches Email and WhatsApp notifications to all active supervisors
 * whenever an Out-of-Office Notification is created or a Report Back is submitted.
 */
public interface SupervisorNotificationService {

    /**
     * Notify all active supervisors (Email + WhatsApp) that a new Out-of-Office Notification was logged.
     */
    void notifyOutOfOfficeCreated(OutOfOfficeNotification notification);

    /**
     * Notify all active supervisors (Email + WhatsApp) that an officer has submitted their Report Back.
     */
    void notifyReportBackSubmitted(OutOfOfficeNotification notification, OutOfOfficeReportBack reportBack);
}

