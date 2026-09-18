package ar.com.cuestionarios.backend;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static ar.com.cuestionarios.backend.ApiModels.*;



class BackendIntegrationTests extends ar.com.cuestionarios.PostgresIntegrationSupport {
    BackendService service;
    @org.junit.jupiter.api.BeforeEach void authenticate(){
        AuthService auth=context.getBean(AuthService.class);
        UserAccount user=auth.register(new AuthModels.RegisterInput("Ana","Pérez","test-"+UUID.randomUUID()+"@example.com","Correct-horse-battery1",new AuthModels.AcademicInput("NONE","NONE","NONE",null)));
        var ctx=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(new LocalPrincipal(user.id),null,java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
        org.springframework.security.core.context.SecurityContextHolder.setContext(ctx);
    }
    @org.junit.jupiter.api.AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    @Test void flujoPersistidoMantieneHistorialYCorreccion() {
        service=context.getBean(BackendService.class);
        BankView bank=service.createBank(new BankInput("Banco"));
        QuestionView original=service.createQuestion(bank.id(),new QuestionInput("Original","Explicación",
            List.of(new OptionInput("Correcta",true),new OptionInput("Incorrecta",false))));
        service.status(bank.id(),new StatusInput("PRIVADO"));
        QuizView quiz=service.createQuiz(new QuizInput(bank.id(),"Cuestionario",1));
        AttemptView attempt=service.start(quiz.id());
        assertThat(attempt.questions().get(0).correctOption()).isNull();
        service.updateQuestion(original.id(),new QuestionInput("Editada","Nueva",List.of(new OptionInput("X",false),new OptionInput("Y",true))));
        service.deactivateQuestion(original.id());
        service.deactivateBank(bank.id());
        AttemptView saved=service.getAttempt(attempt.id());
        assertThat(saved.questions().get(0).statement()).isEqualTo("Original");
        UUID snapshotId=saved.questions().get(0).id();
        service.answer(attempt.id(),snapshotId,new AnswerInput(1));
        service.answer(attempt.id(),snapshotId,new AnswerInput(0));
        AttemptView finished=service.finish(attempt.id());
        assertThat(finished.score()).isEqualTo(1);
        assertThat(finished.percentage()).isEqualTo(100);
        assertThat(finished.questions().get(0).explanation()).isEqualTo("Explicación");
        assertThat(service.finish(attempt.id()).finishedAt()).isEqualTo(finished.finishedAt());
        assertThatThrownBy(()->service.answer(attempt.id(),snapshotId,new AnswerInput(1))).isInstanceOf(BackendException.class);
        assertThatThrownBy(()->service.start(quiz.id())).isInstanceOf(BackendException.class);
    }
    @Test void cargaCompletaEsAtomica() {
        BackendService backend=context.getBean(BackendService.class);
        long before=backend.banks(0,20).total();
        QuestionInput valid=new QuestionInput("Válida",null,List.of(new OptionInput("A",true),new OptionInput("B",false)));
        QuestionInput invalid=new QuestionInput("Inválida",null,List.of(new OptionInput("A",true),new OptionInput("B",true)));
        assertThatThrownBy(()->backend.loadBank(new BankLoadInput("No debe quedar",List.of(valid,invalid))))
            .isInstanceOf(BackendException.class);
        assertThat(backend.banks(0,20).total()).isEqualTo(before);
        BankView saved=backend.loadBank(new BankLoadInput("Carga completa",List.of(valid)));
        assertThat(backend.questions(saved.id(),0,20).total()).isEqualTo(1);
    }
    @Test void intentoDesdeCuestionarioRespetaDisponibilidadYParticipante() {
        BackendService backend=context.getBean(BackendService.class);
        QuestionInput question=new QuestionInput("Nueva práctica","Explicación",List.of(new OptionInput("A",true),new OptionInput("B",false)));
        BankView bank=backend.loadBank(new BankLoadInput("Cuestionario cargado",List.of(question,question)));
        UUID inactive=backend.questions(bank.id(),0,20).items().get(0).id();
        backend.deactivateQuestion(inactive);
        assertThat(backend.availability(bank.id()).questionCount()).isEqualTo(1);
        backend.status(bank.id(),new StatusInput("PRIVADO"));
        long quizzes=backend.quizzes(0,20).total();
        long attempts=backend.attempts(0,20).total();
        assertThatThrownBy(()->backend.startBankAttempt(bank.id(),new StartAttemptInput(2,"Ana"))).isInstanceOf(BackendException.class);
        assertThat(backend.quizzes(0,20).total()).isEqualTo(quizzes);
        assertThat(backend.attempts(0,20).total()).isEqualTo(attempts);
        AttemptView started=backend.startBankAttempt(bank.id(),new StartAttemptInput(1,"  Ana  "));
        assertThat(started.participantName()).isEqualTo("Ana Pérez");
        assertThat(backend.getAttempt(started.id()).participantName()).isEqualTo("Ana Pérez");
        assertThat(started.questions()).hasSize(1);
        assertThat(started.questions().get(0).correctOption()).isNull();
        assertThat(backend.finish(started.id()).score()).isZero();
        backend.deactivateBank(bank.id());
        assertThatThrownBy(()->backend.startBankAttempt(bank.id(),new StartAttemptInput(1,null))).isInstanceOf(BackendException.class);
    }
}
