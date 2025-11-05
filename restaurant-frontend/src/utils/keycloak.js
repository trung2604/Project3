import { IDP } from '../constants';

/**
 * Build Keycloak authorization URL for login
 * @param {string} redirectUri - The redirect URI after authentication
 * @param {boolean} forceLogin - Force login prompt (don't use existing session)
 * @returns {string} The Keycloak authorization URL
 */
export function buildKeycloakAuthUrl(redirectUri, forceLogin = false) {
    const params = new URLSearchParams({
        client_id: IDP.CLIENT_ID,
        response_type: 'code',
        redirect_uri: redirectUri,
        scope: 'openid profile email'
    });

    // Add prompt=login to force Keycloak to show login form even if session exists
    if (forceLogin) {
        params.append('prompt', 'login');
    }

    return `${IDP.URL}/realms/${IDP.REALM}/protocol/openid-connect/auth?${params.toString()}`;
}

/**
 * Build Keycloak registration URL
 * @param {string} redirectUri - The redirect URI after registration
 * @returns {string} The Keycloak registration URL
 */
export function buildKeycloakRegisterUrl(redirectUri) {
    const params = new URLSearchParams({
        client_id: IDP.CLIENT_ID,
        response_type: 'code',
        redirect_uri: redirectUri,
        scope: 'openid profile email'
    });
    return `${IDP.URL}/realms/${IDP.REALM}/protocol/openid-connect/registrations?${params.toString()}`;
}

/**
 * Build Keycloak logout URL
 * @param {string} redirectUri - The redirect URI after logout
 * @param {string} idToken - Optional ID token for logout (not required for JWT stateless)
 * @returns {string} The Keycloak logout URL
 */
export function buildKeycloakLogoutUrl(redirectUri, idToken = null) {
    const params = new URLSearchParams({
        client_id: IDP.CLIENT_ID,
        redirect_uri: redirectUri
    });

    // Add id_token_hint if available (optional, for better logout)
    if (idToken) {
        params.append('id_token_hint', idToken);
    }

    return `${IDP.URL}/realms/${IDP.REALM}/protocol/openid-connect/logout?${params.toString()}`;
}

