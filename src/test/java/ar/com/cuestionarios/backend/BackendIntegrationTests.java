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
    @Test void clasificacionAcademicaEsOpcionalYValida() {
        BackendService backend=context.getBean(BackendService.class);
        BankView plain=backend.createBank(new BankInput("Sin clasificación"));
        assertThat(plain.subject()).isNull();
        assertThat(plain.tags()).isEmpty();
        assertThatThrownBy(()->backend.createBank(new BankInput("Inválido",null,null,2,List.of())))
            .isInstanceOf(BackendException.class);

        AcademicCatalog catalog=context.getBean(AcademicCatalog.class);
        AcademicCatalog.Entry catalogCareer=catalog.careers.stream().filter(entry->entry.durationYears()!=null).findFirst().orElseThrow();
        AcademicCatalog.Entry catalogFaculty=catalog.faculties.stream().filter(entry->entry.id().equals(catalogCareer.parent())).findFirst().orElseThrow();
        int lastYear=catalogCareer.durationYears();
        BankView catalogClassified=backend.createBank(new BankInput("Con catálogo",null,null,null,List.of(),catalogFaculty.parent(),catalogFaculty.id(),catalogCareer.id(),lastYear,"  inGenIeria   y SocIedad "));
        assertThat(catalogClassified.academic().careerId()).isEqualTo(catalogCareer.id());
        assertThat(catalogClassified.academic().studyYear()).isEqualTo(lastYear);
        assertThat(catalogClassified.academic().subjectName()).isEqualTo("Ingenieria y Sociedad");
        assertThatThrownBy(()->backend.createBank(new BankInput("Año fuera de rango",null,null,null,List.of(),catalogFaculty.parent(),catalogFaculty.id(),catalogCareer.id(),lastYear+1,null)))
            .isInstanceOf(BackendException.class);

        CareerView career=backend.createCareer(new CareerInput("Ingeniería en Sistemas","ISI",true));
        assertThatThrownBy(()->backend.createCareer(new CareerInput("ingeniería EN sistemas",null,true)))
            .isInstanceOf(BackendException.class);
        SubjectView subject=backend.createSubject(new SubjectInput(career.id(),"Sistemas Operativos","SO",3,true));
        assertThat(subject.career().id()).isEqualTo(career.id());
        assertThat(backend.subjects(career.id(),3,true)).extracting(SubjectView::id).containsExactly(subject.id());
        assertThatThrownBy(()->backend.createSubject(new SubjectInput(UUID.randomUUID(),"Inexistente",null,1,true))).isInstanceOf(BackendException.class);
        assertThatThrownBy(()->backend.createSubject(new SubjectInput(career.id(),"sistemas operativos",null,3,true)))
            .isInstanceOf(BackendException.class);

        BankView classified=backend.createBank(new BankInput("Primer parcial",subject.id(),"PARCIAL",1,List.of("Memoria Virtual","memoria virtual","TLB")));
        assertThat(classified.subject().id()).isEqualTo(subject.id());
        assertThat(classified.subject().career().id()).isEqualTo(career.id());
        assertThat(classified.subject().studyYear()).isEqualTo(3);
        assertThat(classified.tags()).extracting(TagView::slug).containsExactly("memoria-virtual","tlb");
        assertThat(backend.createTag(new TagInput("MEMORIA VIRTUAL")).id()).isEqualTo(classified.tags().get(0).id());
        assertThat(backend.updateBank(classified.id(),new BankInput("Parcial renombrado")).tags()).hasSize(2);

        BankView cleared=backend.updateBank(classified.id(),new BankInput("Primer parcial",null,null,null,List.of()));
        assertThat(cleared.subject()).isNull();
        assertThat(cleared.evaluationType()).isNull();
        assertThat(cleared.evaluationNumber()).isNull();
        assertThat(cleared.tags()).isEmpty();
    }
    @Test void filtrosAcademicosSeCombinanYEtiquetasUsanAnd() {
        BackendService backend=context.getBean(BackendService.class);
        CareerView career=backend.createCareer(new CareerInput("Licenciatura en Informática",null,true));
        SubjectView subject=backend.createSubject(new SubjectInput(career.id(),"Arquitectura",null,2,true));
        BankView both=backend.createBank(new BankInput("Parcial memoria",subject.id(),"PARCIAL",1,List.of("memoria","cache")));
        backend.createBank(new BankInput("Solo memoria",subject.id(),"FINAL",null,List.of("memoria")));
        backend.createBank(new BankInput("Otro tema"));

        assertThat(backend.banks(0,20,false,new BankFilters(career.id(),null,null,null,null,List.of())).total()).isEqualTo(2);
        assertThat(backend.banks(0,20,false,new BankFilters(null,null,2,null,null,List.of())).total()).isEqualTo(2);
        assertThat(backend.banks(0,20,false,new BankFilters(null,subject.id(),null,null,null,List.of())).total()).isEqualTo(2);
        assertThat(backend.banks(0,20,false,new BankFilters(null,null,null,"PARCIAL",null,List.of())).total()).isEqualTo(1);
        assertThat(backend.banks(0,20,false,new BankFilters(null,null,null,null,1,List.of())).total()).isEqualTo(1);
        assertThat(backend.banks(0,20,false,new BankFilters(null,null,null,null,null,List.of("memoria"))).total()).isEqualTo(2);
        assertThat(backend.banks(0,20,false,new BankFilters(career.id(),null,2,"PARCIAL",1,List.of("memoria","cache"))).items())
            .extracting(BankView::id).containsExactly(both.id());
        assertThat(backend.banks(0,20,false,new BankFilters(null,subject.id(),null,null,null,List.of("memoria"))).total()).isEqualTo(2);
        assertThat(backend.banks(0,20,false,new BankFilters(null,null,null,null,null,List.of("memoria","cache"))).total()).isEqualTo(1);
    }
}
