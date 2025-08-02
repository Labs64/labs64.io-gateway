import os
import logging
from typing import Dict, Any, List, Tuple

import yaml
import requests
from fastapi import FastAPI, Request, HTTPException, status
from jose import jwt
from jose.exceptions import JWTError, ExpiredSignatureError

# --- Caches ---
DISCOVERY_CACHE: Dict[str, Any] = {}
JWKS_CACHE: Dict[str, Any] = {}

# --- Configuration ---
KEYCLOAK_URL = os.getenv("KEYCLOAK_URL", "http://keycloak.tools.svc.cluster.local")
KEYCLOAK_REALM = os.getenv("KEYCLOAK_REALM", "default")
KEYCLOAK_DISCOVERY_URL = os.getenv(
    "KEYCLOAK_DISCOVERY_URL",
    f"{KEYCLOAK_URL}/realms/{KEYCLOAK_REALM}/.well-known/openid-configuration"
)

KEYCLOAK_AUDIENCE = os.getenv("KEYCLOAK_AUDIENCE", "account")
ROLE_MAPPING_FILE = os.getenv("ROLE_MAPPING_FILE", "role_mapping.yaml")

LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO").upper()
LOG_FORMAT = "%(asctime)s - %(levelname)s - %(message)s"

# --- Logging ---
numeric_level = getattr(logging, LOG_LEVEL, logging.INFO)
logging.basicConfig(level=numeric_level, format=LOG_FORMAT)
app_logger = logging.getLogger("traefik_authproxy")
app_logger.setLevel(numeric_level)

# --- App Initialization ---
app = FastAPI(
    title="Traefik Auth (M2M) Middleware",
    description="ForwardAuth service to verify Keycloak JWTs and enforce RBAC based on URI-to-role mapping",
    version="1.0.0"
)

# --- Load Role Mapping and Public Paths ---
def load_role_mapping(file_path: str) -> Tuple[Dict[str, List[str]], List[str]]:
    try:
        with open(file_path, "r") as f:
            raw_mapping = yaml.safe_load(f)

        if not isinstance(raw_mapping, dict):
            raise ValueError("Role mapping file must contain a dictionary")

        protected_paths = {}
        public_paths = []

        for path, roles in raw_mapping.items():
            if roles in (None, [], ["public"]):
                public_paths.append(path)
                app_logger.debug(f"load_role_mapping::Detected public path: {path}")
            else:
                protected_paths[path] = roles

        app_logger.info(
            f"Role mapping loaded: {len(protected_paths)} protected paths, {len(public_paths)} public paths"
        )
        return protected_paths, public_paths

    except Exception as e:
        app_logger.warning(f"load_role_mapping::Skipping path check – failed to load mapping: {e}")
        return {}, []

PROTECTED_PATHS, PUBLIC_PATHS = load_role_mapping(ROLE_MAPPING_FILE)

# --- JWKS Loader with Discovery ---
def get_jwks() -> Dict[str, Any]:
    if JWKS_CACHE:
        app_logger.debug("get_jwks::Using cached JWKS")
        return JWKS_CACHE

    try:
        if "jwks_uri" not in DISCOVERY_CACHE:
            app_logger.info(f"get_jwks::Fetching discovery doc from {KEYCLOAK_DISCOVERY_URL}")
            resp = requests.get(KEYCLOAK_DISCOVERY_URL)
            resp.raise_for_status()
            jwks_uri = resp.json().get("jwks_uri")
            if not jwks_uri:
                raise ValueError("Discovery document missing 'jwks_uri'")
            DISCOVERY_CACHE["jwks_uri"] = jwks_uri

        jwks_uri = DISCOVERY_CACHE["jwks_uri"]
        app_logger.info(f"get_jwks::Fetching JWKS from {jwks_uri}")
        resp = requests.get(jwks_uri)
        resp.raise_for_status()
        JWKS_CACHE.update(resp.json())
        return JWKS_CACHE

    except (requests.RequestException, ValueError) as e:
        app_logger.error(f"get_jwks::Error: {e}")
        raise HTTPException(status_code=500, detail="Failed to retrieve JWKS")

# --- JWT Token Verifier ---
def verify_token(token: str) -> Dict[str, Any]:
    try:
        kid = jwt.get_unverified_header(token).get("kid")
        if not kid:
            raise HTTPException(status_code=401, detail="Missing 'kid' in token header")

        payload = jwt.decode(
            token,
            get_jwks(),
            algorithms=["RS256"],
            audience=KEYCLOAK_AUDIENCE
        )
        app_logger.debug(f"verify_token::Decoded payload: {payload}")
        return payload

    except ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except JWTError as e:
        raise HTTPException(status_code=401, detail=f"Invalid token: {e}")
    except Exception as e:
        app_logger.error("verify_token::Unexpected error", exc_info=True)
        raise HTTPException(status_code=500, detail="Token verification failed")

# --- Role Extractor ---
def extract_token_roles(payload: Dict[str, Any]) -> List[str]:
    roles = set()

    realm_roles = payload.get("realm_access", {}).get("roles", [])
    if isinstance(realm_roles, list):
        roles.update(realm_roles)

    client_roles = payload.get("resource_access", {}).get(KEYCLOAK_AUDIENCE, {}).get("roles", [])
    if isinstance(client_roles, list):
        roles.update(client_roles)

    return list(roles)

# --- Path Role Matcher ---
def get_required_roles(path: str) -> List[str]:
    longest_match = ""
    required_roles = []

    for prefix, roles in PROTECTED_PATHS.items():
        if path.startswith(prefix) and len(prefix) > len(longest_match):
            longest_match = prefix
            required_roles = roles

    return required_roles

def is_public_path(path: str) -> bool:
    return any(path.startswith(pub) for pub in PUBLIC_PATHS)

# --- Authentication Endpoint ---
@app.get("/auth")
@app.post("/auth")
async def authenticate(request: Request):
    forwarded_uri = request.headers.get("X-Forwarded-Uri", "/")
    app_logger.debug(f"Received request on forwarded URI: {forwarded_uri}")

    if is_public_path(forwarded_uri):
        app_logger.info(f"Public access granted to: {forwarded_uri}")
        return {"message": "Public access granted", "user_id": None, "roles": []}

    auth_header = request.headers.get("Authorization")
    if not auth_header or not auth_header.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or malformed Authorization header")

    token = auth_header.split(" ")[1]
    payload = verify_token(token)
    user_roles = extract_token_roles(payload)

    if not user_roles:
        raise HTTPException(status_code=403, detail="Token contains no roles")

    required_roles = get_required_roles(forwarded_uri)
    if not required_roles:
        raise HTTPException(status_code=403, detail=f"No access control configured for: {forwarded_uri}")

    if not set(user_roles).intersection(required_roles):
        raise HTTPException(status_code=403, detail=f"Insufficient roles. Required: {required_roles}")

    app_logger.info(f"Access granted to user {payload.get('sub')} for path {forwarded_uri}")
    return {
        "message": "Authentication successful",
        "user_id": payload.get("sub"),
        "roles": user_roles
    }