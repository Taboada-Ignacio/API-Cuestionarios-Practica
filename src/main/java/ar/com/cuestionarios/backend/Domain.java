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
    boolean active=true;
    protected Bank() {}
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
