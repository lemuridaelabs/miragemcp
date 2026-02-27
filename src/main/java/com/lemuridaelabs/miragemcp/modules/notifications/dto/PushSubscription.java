package com.lemuridaelabs.miragemcp.modules.notifications.dto;

public class PushSubscription {

    private String endpoint;
    private Keys keys;

    public String getEndpoint() {
        return endpoint;
    }

    public Keys getKeys() {
        return keys;
    }

    public static class Keys {
        private String p256dh;
        private String auth;

        public String getP256dh() {
            return p256dh;
        }

        public String getAuth() {
            return auth;
        }
    }
}

