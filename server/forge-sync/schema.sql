-- forge-sync schema. Applied once by deploy.sh; idempotent.
create table if not exists snapshots (
    id          bigserial primary key,
    device_id   text        not null,
    created_at  timestamptz not null default now(),
    sha256      text        not null,
    bytes       integer     not null,
    body        text        not null
);
create index if not exists snapshots_id_desc on snapshots (id desc);
