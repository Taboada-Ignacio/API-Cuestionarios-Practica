-- Reinicio de datos de desarrollo autorizado. No reutilizar para una base con datos reales.
delete from attempt_options;
delete from attempt_questions;
delete from attempts;
delete from quizzes;
delete from question_options;
delete from questions;
delete from banks;
create table app_users (
 id uuid primary key, name varchar(200) not null, email varchar(254) not null unique,
 password_hash varchar(100), google_subject varchar(255) unique,
 university varchar(100), faculty varchar(100), career varchar(100),
 academic_note varchar(1000), active boolean not null,
 created_at timestamptz not null, updated_at timestamptz not null
);
alter table banks add column owner_id uuid not null references app_users(id);
alter table banks add column status varchar(20) not null default 'BORRADOR' check(status in ('BORRADOR','PRIVADO','PUBLICO'));
alter table quizzes add column owner_id uuid not null references app_users(id);
alter table attempts add column owner_id uuid not null references app_users(id);
create index banks_owner_idx on banks(owner_id);
create index banks_public_idx on banks(status) where active=true;
create index quizzes_owner_idx on quizzes(owner_id);
create index attempts_owner_idx on attempts(owner_id);
