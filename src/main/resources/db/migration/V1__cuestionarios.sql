
create table banks (id uuid primary key, name varchar(200) not null, active boolean not null);
create table questions (id uuid primary key, bank_id uuid not null references banks(id), statement text not null, explanation text, active boolean not null);
create table question_options (question_id uuid not null references questions(id), position integer not null, text varchar(1000) not null, correct boolean not null, primary key(question_id,position));
create index questions_bank_idx on questions(bank_id);
create table quizzes (id uuid primary key, bank_id uuid not null references banks(id), name varchar(200) not null, question_count integer not null check(question_count>0), active boolean not null);
create table attempts (id uuid primary key, quiz_id uuid not null references quizzes(id), quiz_name varchar(200) not null, started_at timestamptz not null, finished_at timestamptz, score integer, check ((finished_at is null and score is null) or (finished_at is not null and score is not null)));
create table attempt_questions (id uuid primary key, attempt_id uuid not null references attempts(id), source_question_id uuid not null, position integer not null, statement text not null, explanation text, selected_option integer, unique(attempt_id,position));
create index attempt_questions_attempt_idx on attempt_questions(attempt_id);
create table attempt_options (attempt_question_id uuid not null references attempt_questions(id), position integer not null, text varchar(1000) not null, correct boolean not null, primary key(attempt_question_id,position));
