import { useEffect, useRef } from 'react';
import websocketService from '../services/websocketService';
import { useAuth } from '../context/AuthContext';

/**
 * Custom hook for WebSocket notifications
 * @param {Function} onNotification - Callback when new notification is received
 * @param {Function} onUnreadCountUpdate - Callback when unread count is updated
 */
export const useWebSocketNotifications = (onNotification, onUnreadCountUpdate) => {
    const { user } = useAuth();
    const callbacksRef = useRef({ onNotification, onUnreadCountUpdate });

    // Update callbacks ref when they change
    useEffect(() => {
        callbacksRef.current = { onNotification, onUnreadCountUpdate };
    }, [onNotification, onUnreadCountUpdate]);

    useEffect(() => {
        const userId = user?.userId || user?.id;
        
        if (!userId) {
            return;
        }

        // Connect to WebSocket
        websocketService.connect(
            userId,
            (notification) => {
                // Call the callback if provided
                if (callbacksRef.current.onNotification) {
                    callbacksRef.current.onNotification(notification);
                }
            },
            (count) => {
                // Call the callback if provided
                if (callbacksRef.current.onUnreadCountUpdate) {
                    callbacksRef.current.onUnreadCountUpdate(count);
                }
            },
            (error) => {
                console.error('WebSocket error:', error);
            }
        );

        // Cleanup on unmount
        return () => {
            websocketService.disconnect();
        };
    }, [user?.userId, user?.id]);
};

