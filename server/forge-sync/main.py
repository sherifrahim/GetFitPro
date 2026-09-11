"""
forge-sync — snapshot backup/restore server for the Forge Android app.

Deliberately tiny. The app already produces a full-fidelity backup document (see
app/.../data/backup/BackupModels.kt: format "forge.backup"); this service stores versions of that
document and hands the latest one back. That is the whole sync model — one user, whole snapshots,
last-writer-wins, no per-record merge — chosen over op-based sync because nothing today needs
multi-device merging and the backup path is already verified end to end on the client.

Runs on a 1 GB Always-Free VM shared with another service and Postgres, so:
  * one uvicorn worker, synchronous psycopg, no ORM, no background tasks;
  * bodies are capped (MAX_SNAPSHOT_BYTES) and stored as text, not jsonb, so the bytes that come
    back are exactly the bytes that went in (the client's sha256 dedupe relies on that);
  * old versions are pruned on every write (KEEP_VERSIONS), so storage is bounded.

Auth is a single bearer token (FORGE_SYNC_TOKEN) — this is a personal server, not a multi-tenant
product. Compared in constant time.
"""

import hashlib
import json
import os
import secrets

import psycopg
from fastapi import Depends, FastAPI, Header, HTTPException, Request, Response
from psycopg.rows import dict_row

DATABASE_URL = os.environ["DATABASE_URL"]
TOKEN = os.environ["FORGE_SYNC_TOKEN"]
MAX_SNAPSHOT_BYTES = int(os.environ.get("MAX_SNAPSHOT_BYTES", str(8 * 1024 * 1024)))
KEEP_VERSIONS = int(os.environ.get("KEEP_VERSIONS", "20"))

# No interactive docs: they'd advertise the API surface on a public host for no benefit.
app = FastAPI(title="forge-sync", docs_url=None, redoc_url=None, openapi_url=None)


def db():
    return psycopg.connect(DATABASE_URL, row_factory=dict_row)


def require_token(authorization: str = Header(default="")):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="missing bearer token")
    if not secrets.compare_digest(authorization[7:].strip(), TOKEN):
        raise HTTPException(status_code=401, detail="invalid token")


@app.get("/v1/health")
def health():
    """Unauthenticated liveness check, used by the app's Settings 'Test connection'."""
    return {"ok": True, "service": "forge-sync"}


@app.put("/v1/snapshot", dependencies=[Depends(require_token)])
async def put_snapshot(request: Request, x_device_id: str = Header(default="unknown")):
    body = await request.body()
    if len(body) > MAX_SNAPSHOT_BYTES:
        raise HTTPException(status_code=413, detail=f"snapshot larger than {MAX_SNAPSHOT_BYTES} bytes")
    try:
        doc = json.loads(body)
    except ValueError:
        raise HTTPException(status_code=400, detail="body is not JSON")
    if not isinstance(doc, dict) or doc.get("format") != "forge.backup":
        raise HTTPException(status_code=400, detail="not a forge.backup document")

    text = body.decode("utf-8")
    sha = hashlib.sha256(body).hexdigest()

    with db() as conn:
        latest = conn.execute(
            "select id, created_at, sha256 from snapshots order by id desc limit 1"
        ).fetchone()
        # Same bytes as the current latest: nothing to store. Lets the client upload freely on every
        # change without growing the table when nothing actually changed.
        if latest and latest["sha256"] == sha:
            return {
                "version": latest["id"],
                "created_at": latest["created_at"].isoformat(),
                "sha256": sha,
                "bytes": len(body),
                "unchanged": True,
            }
        row = conn.execute(
            "insert into snapshots (device_id, sha256, bytes, body) values (%s, %s, %s, %s) "
            "returning id, created_at",
            (x_device_id[:64], sha, len(body), text),
        ).fetchone()
        conn.execute(
            "delete from snapshots where id not in "
            "(select id from snapshots order by id desc limit %s)",
            (KEEP_VERSIONS,),
        )
    return {
        "version": row["id"],
        "created_at": row["created_at"].isoformat(),
        "sha256": sha,
        "bytes": len(body),
        "unchanged": False,
    }


@app.get("/v1/snapshot/latest", dependencies=[Depends(require_token)])
def get_latest():
    with db() as conn:
        row = conn.execute(
            "select id, created_at, sha256, bytes, body from snapshots order by id desc limit 1"
        ).fetchone()
    if not row:
        raise HTTPException(status_code=404, detail="no snapshot stored yet")
    return Response(
        content=row["body"],
        media_type="application/json",
        headers={
            "X-Snapshot-Version": str(row["id"]),
            "X-Snapshot-Created": row["created_at"].isoformat(),
            "X-Snapshot-Sha256": row["sha256"],
        },
    )


@app.get("/v1/snapshots", dependencies=[Depends(require_token)])
def list_snapshots():
    with db() as conn:
        rows = conn.execute(
            "select id, device_id, created_at, sha256, bytes from snapshots order by id desc"
        ).fetchall()
    return {
        "snapshots": [
            {
                "version": r["id"],
                "device_id": r["device_id"],
                "created_at": r["created_at"].isoformat(),
                "sha256": r["sha256"],
                "bytes": r["bytes"],
            }
            for r in rows
        ]
    }
