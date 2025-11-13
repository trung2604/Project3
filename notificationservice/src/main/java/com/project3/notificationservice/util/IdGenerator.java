package com.project3.notificationservice.util;

import java.util.UUID;

public class IdGenerator {
    
    public static String generateNotificationId() {
        return "notif-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

