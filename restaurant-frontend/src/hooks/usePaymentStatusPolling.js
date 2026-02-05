import { useEffect, useState } from "react";
import paymentAPI from "../services/paymentService";
import { PAYMENT_STATUS } from "../constants";

/**
 * Hook to poll payment status automatically
 * @param {string} paymentId - Payment ID to poll
 * @param {string} initialStatus - Initial payment status
 * @param {function} onComplete - Callback when payment completes
 * @param {boolean} enabled - Whether polling is enabled
 * @returns {object} { status, isPolling, error }
 */
const usePaymentStatusPolling = (
    paymentId,
    initialStatus,
    onComplete,
    enabled = true
) => {
    const [status, setStatus] = useState(initialStatus);
    const [isPolling, setIsPolling] = useState(false);
    const [error, setError] = useState(null);
    const [attemptCount, setAttemptCount] = useState(0);

    useEffect(() => {
        if (!enabled || !paymentId || status !== PAYMENT_STATUS.PENDING) {
            setIsPolling(false);
            return;
        }

        setIsPolling(true);
        setAttemptCount(0);

        const POLLING_INTERVAL = 5000; // 5 seconds
        const MAX_ATTEMPTS = 60; // 5 minutes max (60 * 5s = 300s)

        const interval = setInterval(async () => {
            try {
                setAttemptCount((prev) => {
                    const newCount = prev + 1;

                    // Stop polling after max attempts
                    if (newCount >= MAX_ATTEMPTS) {
                        clearInterval(interval);
                        setIsPolling(false);
                        setError("Payment timeout: Maximum polling time exceeded");
                        return prev;
                    }

                    return newCount;
                });

                const response = await paymentAPI.getPaymentById(paymentId);
                const newStatus = response?.data?.paymentStatus;

                if (newStatus && newStatus !== PAYMENT_STATUS.PENDING) {
                    setStatus(newStatus);
                    setIsPolling(false);
                    clearInterval(interval);

                    if (onComplete) {
                        onComplete(newStatus, response.data);
                    }
                }
            } catch (err) {
                console.error("Error polling payment status:", err);
                setError(err.message || "Failed to poll payment status");
                // Continue polling despite error
            }
        }, POLLING_INTERVAL);

        return () => {
            clearInterval(interval);
            setIsPolling(false);
        };
    }, [paymentId, initialStatus, enabled, onComplete, status]);

    return {
        status,
        isPolling,
        error,
        attemptCount,
    };
};

export default usePaymentStatusPolling;
