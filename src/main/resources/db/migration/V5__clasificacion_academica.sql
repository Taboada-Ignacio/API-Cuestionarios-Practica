create table careers (
 id uuid primary key,
 name varchar(200) not null,
 code varchar(50),
 active boolean not null default true,
 created_at timestamptz not null,
 updated_at timestamptz not null
);
create unique index careers_name_unique_idx on careers (lower(name));

create table subjects (
 id uuid primary key,
 career_id uuid not null references careers(id),
 name varchar(200) not null,
 code varchar(50),
 study_year integer not null check (study_year >= 1),
 active boolean not null default true,
 created_at timestamptz not null,
 updated_at timestamptz not null
);
create unique index subjects_career_name_unique_idx on subjects (career_id,lower(name));
create index subjects_career_idx on subjects(career_id);
create index subjects_study_year_idx on subjects(study_year);

create table tags (
 id uuid primary key,
 name varchar(100) not null,
 slug varchar(120) not null unique,
 created_at timestamptz not null
);

alter table banks add column subject_id uuid references subjects(id);
alter table banks add column evaluation_type varchar(30)
 check (evaluation_type in ('PARCIAL','TRABAJO_PRACTICO','FINAL','RECUPERATORIO','PRACTICA','OTRO'));
alter table banks add column evaluation_number integer check (evaluation_number >= 1);
alter table banks add constraint banks_evaluation_number_requires_type
 check (evaluation_number is null or evaluation_type is not null);
create index banks_subject_idx on banks(subject_id);
create index banks_evaluation_type_idx on banks(evaluation_type);
create index banks_evaluation_number_idx on banks(evaluation_number);

create table bank_tags (
 bank_id uuid not null references banks(id) on delete cascade,
 tag_id uuid not null references tags(id),
 primary key(bank_id,tag_id)
);
create index bank_tags_tag_idx on bank_tags(tag_id);
