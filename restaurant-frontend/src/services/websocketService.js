import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.client = null;
        this.subscriptions = new Map();
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
    }

    /**
     * Connect to WebSocket server
     * @param {string} userId - User ID for authentication
     * @param {Function} onNotification - Callback when notification is received
     * @param {Function} onUnreadCount - Callback when unread count is updated
     * @param {Function} onError - Callback when error occurs
     */
    connect(userId, onNotification, onUnreadCount, onError) {
        if (!userId) {
            console.warn('Cannot connect WebSocket: userId is required');
            return;
        }

        // Disconnect existing connection if any
        this.disconnect();

        // Create SockJS connection
        const socket = new SockJS(`${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'}/ws/notifications?userId=${userId}`);
        
        // Create STOMP client
        this.client = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: this.reconnectDelay,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            debug: (str) => {
                // Only log in development
                if (import.meta.env.DEV) {
                    console.log('STOMP:', str);
                }
            },
            onConnect: (frame) => {
                console.log('WebSocket connected:', frame);
                this.reconnectAttempts = 0;

                // Subscribe to user-specific notification destination
                const notificationSub = this.client.subscribe(
                    `/user/${userId}/notifications`,
                    (message) => {
                        try {
                            const notification = JSON.parse(message.body);
                            if (onNotification) {
                                onNotification(notification);
                            }
                        } catch (error) {
                            console.error('Error parsing notification:', error);
                        }
                    }
                );
                this.subscriptions.set('notifications', notificationSub);

                // Subscribe to unread count updates
                const countSub = this.client.subscribe(
                    `/user/${userId}/notifications/count`,
                    (message) => {
                        try {
                            const count = parseInt(message.body, 10);
                            if (onUnreadCount) {
                                onUnreadCount(count);
                            }
                        } catch (error) {
                            console.error('Error parsing unread count:', error);
                        }
                    }
                );
                this.subscriptions.set('count', countSub);
            },
            onStompError: (frame) => {
                console.error('STOMP error:', frame);
                if (onError) {
                    onError(new Error(frame.headers['message'] || 'WebSocket error'));
                }
            },
            onWebSocketClose: () => {
                console.log('WebSocket closed');
                this.subscriptions.clear();
            },
            onDisconnect: () => {
                console.log('WebSocket disconnected');
                this.subscriptions.clear();
            }
        });

        // Handle reconnection
        this.client.onWebSocketError = (error) => {
            console.error('WebSocket error:', error);
            if (this.reconnectAttempts < this.maxReconnectAttempts) {
                this.reconnectAttempts++;
                console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
                setTimeout(() => {
                    if (this.client && !this.client.connected) {
                        this.client.activate();
                    }
                }, this.reconnectDelay);
            } else {
                console.error('Max reconnection attempts reached');
                if (onError) {
                    onError(new Error('Failed to connect to WebSocket server'));
                }
            }
        };

        // Activate the client
        this.client.activate();
    }

    /**
     * Disconnect from WebSocket server
     */
    disconnect() {
        if (this.client) {
            // Unsubscribe from all subscriptions
            this.subscriptions.forEach((sub) => {
                try {
                    sub.unsubscribe();
                } catch (error) {
                    console.error('Error unsubscribing:', error);
                }
            });
            this.subscriptions.clear();

            // Deactivate client
            if (this.client.connected) {
                this.client.deactivate();
            }
            this.client = null;
        }
    }

    /**
     * Check if WebSocket is connected
     */
    isConnected() {
        return this.client && this.client.connected;
    }
}

// Export singleton instance
const websocketService = new WebSocketService();
export default websocketService;

