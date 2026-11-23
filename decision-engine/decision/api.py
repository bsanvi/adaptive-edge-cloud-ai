import hmac
import os
import threading
import time

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from decision.models import DecisionRequest, DecisionResponse
from decision.engine import make_decision


app = FastAPI(
    title="Intelligent Edge-Cloud Decision Engine",
    description="Rule-based and explainable workload placement Decision Engine.",
    version="0.1.0",
)

_windows = {}
_lock = threading.Lock()


def add_security_headers(response):
    response.headers["Cache-Control"] = "no-store"
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["Referrer-Policy"] = "no-referrer"
    return response


def allowed(client):
    limit = int(os.getenv("SECURITY_RATE_LIMIT_PER_MINUTE", "300"))
    now = time.monotonic()

    with _lock:
        started, count = _windows.get(client, (now, 0))

        if now - started >= 60:
            started = now
            count = 0

        count += 1
        _windows[client] = (started, count)

        return count <= limit


@app.middleware("http")
async def security_middleware(request: Request, call_next):
    client = request.client.host if request.client else "unknown"

    if not allowed(client):
        return add_security_headers(
            JSONResponse(
                status_code=429,
                headers={"Retry-After": "60"},
                content={"detail": "Too many requests"},
            )
        )

    length = request.headers.get("content-length")

    if length:
        try:
            if int(length) > 1_048_576:
                return add_security_headers(
                    JSONResponse(
                        status_code=413,
                        content={"detail": "Request body too large"},
                    )
                )
        except ValueError:
            return add_security_headers(
                JSONResponse(
                    status_code=400,
                    content={"detail": "Invalid Content-Length"},
                )
            )

    if request.url.path != "/health":
        expected = os.getenv("INTERNAL_API_KEY", "")

        if expected:
            if len(expected) < 32:
                return add_security_headers(
                    JSONResponse(
                        status_code=503,
                        content={"detail": "Decision API security misconfigured"},
                    )
                )

            supplied = request.headers.get("X-Internal-Api-Key", "")

            if not hmac.compare_digest(supplied, expected):
                return add_security_headers(
                    JSONResponse(
                        status_code=401,
                        content={"detail": "Missing or invalid internal API key"},
                    )
                )

    response = await call_next(request)
    return add_security_headers(response)


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "component": "decision-engine",
    }


@app.post("/decision/evaluate", response_model=DecisionResponse)
def evaluate_decision(request: DecisionRequest):
    return make_decision(request)
