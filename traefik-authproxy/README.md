<p align="center"><img src="https://raw.githubusercontent.com/Labs64/.github/refs/heads/master/assets/labs64-io-ecosystem.png"></p>

## Traefik Auth (M2M) Middleware

This repository contains a custom Traefik ForwardAuth middleware. The middleware is designed to verify M2M (Machine-to-Machine) JWT tokens issued by Keycloak and enforce path-based role-based access control (RBAC) for microservices deployed on Kubernetes.

It receives incoming requests from Traefik, validates the JWT token, extracts user roles, and checks them against a configurable path/role mapping. If the request is authorized, it allows Traefik to forward the request to the backend service. Otherwise, it returns a `401 Unauthorized` or `403 Forbidden` response.

## Features

- JWT Verification: Validates tokens issued by Keycloak using public keys from the `.well-known` endpoint.
- Role-Based Access Control (RBAC): Enforces access based on roles assigned to the user/client.
- Configurable Role Mapping: Allows administrators to define a mapping of URL paths to required roles.
- FastAPI Backend: A lightweight and performant backend for handling authentication logic.

## Prerequisites

- A running Kubernetes cluster.
- Traefik installed as an Ingress Controller in your cluster.
- A configured Keycloak instance.
- Docker for building the middleware container image.

## Configuration

The middleware is configured using environment variables.

- `KEYCLOAK_URL`: The base URL of your Keycloak instance (e.g., http://keycloak.default.svc.cluster.local:8080).
- `KEYCLOAK_REALM`: The name of the realm in Keycloak (e.g., labs64io).
- `KEYCLOAK_AUDIENCE`: The audience claim to verify in the JWT (e.g., labs64io_client).
- `KEYCLOAK_DISCOVERY_URL`: The URL to the Keycloak discovery endpoint (e.g., http://keycloak.default.svc.cluster.local:8080/realms/labs64io/.well-known/openid-configuration).
- `ROLE_MAPPING_FILE`: YAML file defining the path-to-role mapping. This can be passed as a ConfigMap in a production environment.

## Usage

- Once deployed, Traefik will intercept any request to *whoami.example.com* and forward it to the auth-middleware for authentication.
- For a request to be successful, it must include a valid JWT in the Authorization header with the format `Bearer <token>`. The roles contained in the JWT must match the required roles for the requested path as defined in your role mapping.
- The role mapping is a key part of the middleware's logic. You would define a dictionary that maps a path prefix to a list of required roles.

### For example:

- A request to `/api/admin` would require the `admin` role.
- A request to `/api/users` would require either the `user` or `admin` role.

## License

This project is licensed under the MIT License.
