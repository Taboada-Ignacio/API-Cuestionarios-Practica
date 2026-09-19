alter table banks add column academic_university_id varchar(32);
alter table banks add column academic_faculty_id varchar(32);
alter table banks add column academic_career_id varchar(32);
alter table banks add column study_year integer check (study_year >= 1);
alter table banks add constraint banks_academic_selection_complete check (
 (academic_university_id is null and academic_faculty_id is null and academic_career_id is null)
 or (academic_university_id is not null and academic_faculty_id is not null and academic_career_id is not null)
);
alter table banks add constraint banks_study_year_requires_academic_career check (study_year is null or academic_career_id is not null);
create index banks_academic_career_idx on banks(academic_career_id);
create index banks_study_year_idx on banks(study_year);
