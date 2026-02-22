/**
 * HoneyMCP Dashboard Service Worker
 * Handles push notifications for security alerts
 */

console.log('[SW] Service worker script loaded and executing');

// Handle push notification events
self.addEventListener('push', event => {
    console.log('[SW] Push notification received');

    let data = {
        title: 'HoneyMCP Alert',
        body: 'New security event detected',
        icon: '/dashboard/icon.png',
        badge: '/dashboard/badge.png',
        tag: 'honeymcp-alert',
        requireInteraction: true
    };

    // Parse push data if available
    if (event.data) {
        try {
            const payload = event.data.json();
            data = {
                ...data,
                title: payload.title || data.title,
                body: payload.body || data.body,
                icon: payload.icon || data.icon,
                tag: payload.tag || data.tag,
                data: payload.data || {}
            };
        } catch (e) {
            console.error('[SW] Error parsing push data:', e);
            // Try as text if JSON parsing fails
            const text = event.data.text();
            if (text) {
                data.body = text;
            }
        }
    }

    event.waitUntil(
        self.registration.showNotification(data.title, {
            body: data.body,
            icon: data.icon,
            badge: data.badge,
            tag: data.tag,
            requireInteraction: data.requireInteraction,
            data: data.data
        })
    );
});

// Handle notification click events
self.addEventListener('notificationclick', event => {
    console.log('[SW] Notification clicked');

    event.notification.close();

    // Get the dashboard URL - we need to include the token
    // The URL is stored in the notification data or we open a default path
    const urlToOpen = event.notification.data?.url || '/dashboard';

    event.waitUntil(
        clients.matchAll({type: 'window', includeUncontrolled: true})
            .then(clientList => {
                // Check if there's already a dashboard window open
                for (const client of clientList) {
                    if (client.url.includes('/dashboard') && 'focus' in client) {
                        return client.focus();
                    }
                }
                // Open a new window if no existing dashboard window
                if (clients.openWindow) {
                    return clients.openWindow(urlToOpen);
                }
            })
    );
});

// Handle service worker installation
self.addEventListener('install', event => {
    console.log('[SW] Service worker installed');
    // Skip waiting to activate immediately
    self.skipWaiting();
});

// Handle service worker activation
self.addEventListener('activate', event => {
    console.log('[SW] Service worker activated');
    // Claim all clients immediately
    event.waitUntil(clients.claim());
});
