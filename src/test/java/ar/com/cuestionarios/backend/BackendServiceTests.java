package ar.com.cuestionarios.backend;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static ar.com.cuestionarios.backend.ApiModels.*;

class BackendServiceTests {
    BackendRepository repository;
    BackendService service;
    UserAccount user=new UserAccount();
    Bank ownedBank(){Bank b=new Bank();b.ownerId=user.id;b.status="PRIVADO";return b;}
    Quiz ownedQuiz(){Quiz q=new Quiz();q.ownerId=user.id;return q;}
    Attempt ownedAttempt(){Attempt a=new Attempt();a.ownerId=user.id;return a;}
    @BeforeEach void setup() { repository=mock(BackendRepository.class); CurrentUser current=mock(CurrentUser.class);user.name="Ana";when(current.complete()).thenReturn(user);service=new BackendService(repository,current); }
    AttemptQuestion question(Attempt a) {
        AttemptQuestion q=new AttemptQuestion(); q.attemptId=a.id; q.statement="Pregunta";
        q.explanation="Explicación"; q.options.add(new Option("Correcta",true)); q.options.add(new Option("Incorrecta",false)); return q;
    }
    @Test void ocultaCorreccionHastaFinalizarYCuentaOmisionesComoIncorrectas() {
        Attempt a=ownedAttempt(); a.quizName="Práctica";
        AttemptQuestion answered=question(a); answered.selectedOption=0;
        AttemptQuestion omitted=question(a);
        when(repository.get(Attempt.class,a.id)).thenReturn(a);
        when(repository.lockAttempt(a.id)).thenReturn(a);
        when(repository.snapshots(a.id)).thenReturn(List.of(answered,omitted));
        AttemptView open=service.getAttempt(a.id);
        assertThat(open.score()).isNull();
        assertThat(open.questions()).allSatisfy(q->{assertThat(q.correctOption()).isNull(); assertThat(q.explanation()).isNull();});
        AttemptView finished=service.finish(a.id);
        assertThat(finished.score()).isEqualTo(1);
        assertThat(finished.percentage()).isEqualTo(50);
        assertThat(finished.questions()).allSatisfy(q->{assertThat(q.correctOption()).isZero(); assertThat(q.explanation()).isEqualTo("Explicación");});
        assertThat(service.finish(a.id)).isEqualTo(finished);
        assertThatThrownBy(()->service.answer(a.id,answered.id,new AnswerInput(1))).isInstanceOf(BackendException.class);
    }
    @Test void permiteRevisarRespuestasYRechazaOpcionesAjenas() {
        Attempt a=ownedAttempt(); AttemptQuestion q=question(a);
        when(repository.lockAttempt(a.id)).thenReturn(a);
        when(repository.get(AttemptQuestion.class,q.id)).thenReturn(q);
        when(repository.snapshots(a.id)).thenReturn(List.of(q));
        service.answer(a.id,q.id,new AnswerInput(0));
        service.answer(a.id,q.id,new AnswerInput(1));
        assertThat(q.selectedOption).isEqualTo(1);
        assertThatThrownBy(()->service.answer(a.id,q.id,new AnswerInput(2))).isInstanceOf(BackendException.class);
        q.attemptId=UUID.randomUUID();
        assertThatThrownBy(()->service.answer(a.id,q.id,new AnswerInput(0))).isInstanceOf(BackendException.class);
    }
    @Test void seleccionaSinRepeticionYConservaCopiasIndependientes() {
        Bank bank=ownedBank(); Quiz quiz=ownedQuiz(); quiz.bankId=bank.id; quiz.name="Práctica"; quiz.questionCount=2;
        Question first=new Question(); first.bankId=bank.id; first.statement="Original"; first.options.add(new Option("Sí",true)); first.options.add(new Option("No",false));
        Question second=new Question(); second.bankId=bank.id; second.statement="Otra"; second.options.add(new Option("A",true)); second.options.add(new Option("B",false));
        when(repository.get(Quiz.class,quiz.id)).thenReturn(quiz);
        when(repository.get(Bank.class,bank.id)).thenReturn(bank);
        when(repository.questions(bank.id,true)).thenReturn(List.of(first,second));
        List<AttemptQuestion> copies=new ArrayList<>();
        doAnswer(call->{Object value=call.getArgument(0); if(value instanceof AttemptQuestion q) copies.add(q); return null;}).when(repository).save(any());
        when(repository.snapshots(any())).thenAnswer(call->copies);
        AttemptView view=service.start(quiz.id);
        assertThat(view.total()).isEqualTo(2);
        assertThat(copies.stream().map(q->q.sourceQuestionId)).doesNotHaveDuplicates();
        first.statement="Editada"; first.options.get(0).text="Cambiada";
        AttemptQuestion copy=copies.stream().filter(q->q.sourceQuestionId.equals(first.id)).findFirst().orElseThrow();
        assertThat(copy.statement).isEqualTo("Original"); assertThat(copy.options.get(0).text).isEqualTo("Sí");
        quiz.questionCount=3;
        assertThatThrownBy(()->service.start(quiz.id)).isInstanceOf(BackendException.class).hasMessageContaining("disponibles: 2");
        bank.active=false;
        assertThatThrownBy(()->service.start(quiz.id)).isInstanceOf(BackendException.class);
    }
    @Test void exigeUnaSolaRespuestaCorrecta() {
        Bank b=ownedBank(); when(repository.get(Bank.class,b.id)).thenReturn(b);
        assertThatThrownBy(()->service.createQuestion(b.id,new QuestionInput("Pregunta",null,List.of(new OptionInput("A",true),new OptionInput("B",true)))))
            .isInstanceOf(BackendException.class);
    }
}
