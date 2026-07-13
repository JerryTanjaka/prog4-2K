create table if not exists image_submission
(
    id                   uuid      not null primary key,
    email                varchar   not null,
    created_at           timestamp not null,
    content_type         varchar,
    original_bucket_key  varchar,
    bw_bucket_key        varchar
);
