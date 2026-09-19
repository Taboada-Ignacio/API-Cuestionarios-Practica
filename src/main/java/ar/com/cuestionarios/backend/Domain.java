package ar.com.cuestionarios.backend;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="banks")
class Bank {
    @Id UUID id = UUID.randomUUID();
    @Column(nullable=false,length=200) String name;
    @Column(name="owner_id",nullable=false) UUID ownerId;
    @Column(nullable=false,length=20) String status="BORRADOR";
    @Column(name="subject_id") UUID subjectId;
    @Enumerated(EnumType.STRING) @Column(name="evaluation_type",length=30) EvaluationType evaluationType;
    @Column(name="evaluation_number") Integer evaluationNumber;
    @Column(name="academic_university_id",length=32) String academicUniversityId;
    @Column(name="academic_faculty_id",length=32) String academicFacultyId;
    @Column(name="academic_career_id",length=32) String academicCareerId;
    @Column(name="study_year") Integer studyYear;
    @Column(name="academic_subject_name",length=200) String academicSubjectName;
    @ManyToMany(fetch=FetchType.LAZY)
    @JoinTable(name="bank_tags",joinColumns=@JoinColumn(name="bank_id"),inverseJoinColumns=@JoinColumn(name="tag_id"))
    Set<Tag> tags=new LinkedHashSet<>();
    boolean active=true;
    protected Bank() {}
}
enum EvaluationType { PARCIAL,TRABAJO_PRACTICO,FINAL,RECUPERATORIO,PRACTICA,OTRO }
@Entity @Table(name="careers")
class Career {
    @Id UUID id=UUID.randomUUID();
    @Column(nullable=false,length=200) String name;
    @Column(length=50) String code;
    boolean active=true;
    @Column(name="created_at",nullable=false) Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) Instant updatedAt=Instant.now();
    protected Career() {}
}
@Entity @Table(name="subjects")
class Subject {
    @Id UUID id=UUID.randomUUID();
    @Column(name="career_id",nullable=false) UUID careerId;
    @Column(nullable=false,length=200) String name;
    @Column(length=50) String code;
    @Column(name="study_year",nullable=false) int studyYear;
    boolean active=true;
    @Column(name="created_at",nullable=false) Instant createdAt=Instant.now();
    @Column(name="updated_at",nullable=false) Instant updatedAt=Instant.now();
    protected Subject() {}
}
@Entity @Table(name="tags")
class Tag {
    @Id UUID id=UUID.randomUUID();
    @Column(nullable=false,length=100) String name;
    @Column(nullable=false,unique=true,length=120) String slug;
    @Column(name="created_at",nullable=false) Instant createdAt=Instant.now();
    protected Tag() {}
}
@Embeddable
class Option {
    @Column(nullable=false,length=1000) String text;
    @Column(nullable=false) boolean correct;
    protected Option() {}
    Option(String text, boolean correct) { this.text=text; this.correct=correct; }
}
@Entity @Table(name="questions")
class Question {
    @Id UUID id=UUID.randomUUID();
    @Column(name="bank_id",nullable=false) UUID bankId;
    @Column(nullable=false,columnDefinition="text") String statement;
    @Column(columnDefinition="text") String explanation;
    boolean active=true;
    @ElementCollection @CollectionTable(name="question_options",joinColumns=@JoinColumn(name="question_id"))
    @OrderColumn(name="position") List<Option> options=new ArrayList<>();
    protected Question() {}
}
@Entity @Table(name="quizzes")
class Quiz {
    @Id UUID id=UUID.randomUUID();
    @Column(name="bank_id",nullable=false) UUID bankId;
    @Column(nullable=false,length=200) String name;
    @Column(name="question_count",nullable=false) int questionCount;
    boolean active=true;
    @Column(name="owner_id",nullable=false) UUID ownerId;
    protected Quiz() {}
}
@Entity @Table(name="attempts")
class Attempt {
    @Id UUID id=UUID.randomUUID();
    @Column(name="quiz_id",nullable=false) UUID quizId;
    @Column(name="quiz_name",nullable=false,length=200) String quizName;
    @Column(name="participant_name",length=200) String participantName;
    @Column(name="started_at",nullable=false) Instant startedAt=Instant.now();
    @Column(name="finished_at") Instant finishedAt;
    Integer score;
    @Column(name="owner_id",nullable=false) UUID ownerId;
    protected Attempt() {}
}
@Entity @Table(name="attempt_questions")
class AttemptQuestion {
    @Id UUID id=UUID.randomUUID();
    @Column(name="attempt_id",nullable=false) UUID attemptId;
    @Column(name="source_question_id",nullable=false) UUID sourceQuestionId;
    int position;
    @Column(nullable=false,columnDefinition="text") String statement;
    @Column(columnDefinition="text") String explanation;
    @Column(name="selected_option") Integer selectedOption;
    @ElementCollection @CollectionTable(name="attempt_options",joinColumns=@JoinColumn(name="attempt_question_id"))
    @OrderColumn(name="position") List<Option> options=new ArrayList<>();
    protected AttemptQuestion() {}
}
