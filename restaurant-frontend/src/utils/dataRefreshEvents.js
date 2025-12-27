/**
 * Utility for dispatching and listening to data refresh events
 * This allows components to automatically reload when related data changes
 */

// Event names
export const DATA_REFRESH_EVENTS = {
  CATEGORY_CREATED: "category:created",
  CATEGORY_UPDATED: "category:updated",
  CATEGORY_DELETED: "category:deleted",
  MENU_ITEM_CREATED: "menuItem:created",
  MENU_ITEM_UPDATED: "menuItem:updated",
  MENU_ITEM_DELETED: "menuItem:deleted",
  INGREDIENT_CREATED: "ingredient:created",
  INGREDIENT_UPDATED: "ingredient:updated",
  INGREDIENT_DELETED: "ingredient:deleted",
  ORDER_CREATED: "order:created",
  ORDER_UPDATED: "order:updated",
  ORDER_CANCELLED: "order:cancelled",
  USER_CREATED: "user:created",
  USER_UPDATED: "user:updated",
  USER_DELETED: "user:deleted",
  VOUCHER_CREATED: "voucher:created",
  VOUCHER_UPDATED: "voucher:updated",
  VOUCHER_DELETED: "voucher:deleted",
  COMBO_CREATED: "combo:created",
  COMBO_UPDATED: "combo:updated",
  COMBO_DELETED: "combo:deleted",
};

/**
 * Dispatch a data refresh event
 * @param {string} eventName - The event name from DATA_REFRESH_EVENTS
 * @param {object} data - Optional data to pass with the event
 */
export const dispatchDataRefresh = (eventName, data = null) => {
  if (typeof window !== "undefined") {
    const event = new CustomEvent(eventName, { detail: data });
    window.dispatchEvent(event);
  }
};

/**
 * Listen to a data refresh event
 * @param {string} eventName - The event name from DATA_REFRESH_EVENTS
 * @param {function} callback - Callback function to execute when event is fired
 * @returns {function} - Cleanup function to remove the event listener
 */
export const listenToDataRefresh = (eventName, callback) => {
  if (typeof window !== "undefined") {
    window.addEventListener(eventName, callback);
    // Return cleanup function
    return () => {
      window.removeEventListener(eventName, callback);
    };
  }
  return () => {}; // No-op cleanup if window is not available
};

/**
 * React hook to listen to data refresh events
 * This is a factory function that returns a hook
 * Components should use it like: useDataRefresh(eventNames, callback, deps)
 *
 * Usage in component:
 * import { useEffect } from 'react';
 * import { useDataRefresh, DATA_REFRESH_EVENTS } from '../utils/dataRefreshEvents';
 *
 * const MyComponent = () => {
 *   useDataRefresh([DATA_REFRESH_EVENTS.CATEGORY_CREATED], () => { loadData(); }, []);
 *   ...
 * }
 */
export const useDataRefresh = (eventNames, callback, deps = []) => {
  // This will be called in components that have React available
  // We'll use a pattern where components call this with useEffect
  // For now, we'll provide a simpler implementation that works with useEffect
  if (typeof window === "undefined") {
    return;
  }

  // Return a function that can be used in useEffect
  // Components will need to call this in their useEffect
  const eventNameArray = Array.isArray(eventNames) ? eventNames : [eventNames];
  const cleanupFunctions = [];

  eventNameArray.forEach((eventName) => {
    const cleanup = listenToDataRefresh(eventName, callback);
    cleanupFunctions.push(cleanup);
  });

  // Return cleanup function
  return () => {
    cleanupFunctions.forEach((cleanup) => cleanup());
  };
};
