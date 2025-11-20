import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { Auth0Provider } from '@auth0/auth0-react'
import '@fortawesome/fontawesome-free/css/all.min.css'
import App from './App.jsx'
import './index.css'

const domain = import.meta.env.VITE_AUTH0_DOMAIN
const clientId = import.meta.env.VITE_AUTH0_CLIENT_ID
const redirectUri = import.meta.env.VITE_AUTH0_CALLBACK_URL

// Validate required Auth0 configuration variables
if (!domain) {
    throw new Error("Missing required Auth0 configuration: VITE_AUTH0_DOMAIN. Please set this environment variable.")
}
if (!clientId) {
    throw new Error("Missing required Auth0 configuration: VITE_AUTH0_CLIENT_ID. Please set this environment variable.")
}
if (!redirectUri) {
    throw new Error("Missing required Auth0 configuration: VITE_AUTH0_CALLBACK_URL. Please set this environment variable.")
}

ReactDOM.createRoot(document.getElementById('root')).render(
    // *** StrictMode removed to stop double rendering ***
    <Auth0Provider
        domain={domain}
        clientId={clientId}
        authorizationParams={{
            redirect_uri: redirectUri,
            scope: "openid profile email"
        }}
        cacheLocation="localstorage"
        useRefreshTokens={true}
    >
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </Auth0Provider>
)