package ar.com.cuestionarios.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import static org.springframework.http.HttpStatus.*;
import static ar.com.cuestionarios.backend.ApiModels.*;

@Service
@Transactional
public class BackendService {
    private final BackendRepository repository;
    private final CurrentUser current;
    BackendService(BackendRepository repository,CurrentUser current) { this.repository=repository;this.current=current; }
    private UUID user(){return current.complete().id;}
    private void own(UUID owner){if(!user().equals(owner))throw new BackendException(NOT_FOUND,"Recurso no encontrado");}
    private Bank ownedBank(UUID id){Bank b=repository.get(Bank.class,id);own(b.ownerId);return b;}
    private Bank readableBank(UUID id){Bank b=repository.get(Bank.class,id);UUID uid=user();if(!uid.equals(b.ownerId)&&(!b.active||!b.status.equals("PUBLICO")))throw new BackendException(NOT_FOUND,"Recurso no encontrado");return b;}
    private Bank practiceBank(UUID id){Bank b=readableBank(id);active(b.active);if(b.status.equals("BORRADOR"))throw new BackendException(CONFLICT,"Publicá el borrador como privado o público antes de realizar intentos");return b;}
    private Attempt ownedAttempt(UUID id){Attempt a=repository.get(Attempt.class,id);own(a.ownerId);return a;}
    private Quiz ownedQuiz(UUID id){Quiz q=repository.get(Quiz.class,id);own(q.ownerId);return q;}
    public BankView status(UUID id,StatusInput input){Bank b=ownedBank(id);active(b.active);if(!Set.of("BORRADOR","PRIVADO","PUBLICO").contains(input.status()))throw new BackendException(BAD_REQUEST,"Estado inválido");if(!input.status().equals("BORRADOR")&&repository.activeQuestionCount(id)==0)throw new BackendException(CONFLICT,"Agregá una pregunta antes de publicar");b.status=input.status();return view(b);}

    private void active(boolean active) { if(!active) throw new BackendException(CONFLICT,"El recurso está desactivado"); }
    private void pagination(int page,int size) {
        if(page<0 || page>1000000 || size<1 || size>100)
            throw new BackendException(BAD_REQUEST,"page debe ser 0..1000000 y size 1..100");
    }
    private BankView view(Bank b) { return new BankView(b.id,b.name,b.active,b.ownerId,b.status); }
    private QuestionView view(Question q) {
        return new QuestionView(q.id,q.bankId,q.statement,q.explanation,
            q.options.stream().map(o->new OptionInput(o.text,o.correct)).toList(),q.active);
    }
    private QuizView view(Quiz q) { return new QuizView(q.id,q.bankId,q.name,q.questionCount,q.active); }

    public BankView loadBank(BankLoadInput input) {
        BankView bank=createBank(new BankInput(input.name()));
        input.questions().forEach(question->createQuestion(bank.id(),question));
        if(input.status()!=null&&!input.status().equals("BORRADOR"))return status(bank.id(),new StatusInput(input.status()));
        return bank;
    }
    public BankView createBank(BankInput input) {
        Bank b=new Bank(); b.ownerId=user(); b.name=input.name().trim(); repository.save(b); return view(b);
    }
    public BankView getBank(UUID id) { return view(readableBank(id)); }
    public PageView<BankView> practiceBanks(int page,int size){pagination(page,size);UUID uid=user();return new PageView<>(repository.scopedBanks(uid,true,page,size).stream().map(this::view).toList(),page,size,repository.scopedBankCount(uid,true));}
    public PageView<BankView> banks(int page,int size) {
        pagination(page,size);
        return new PageView<>(repository.scopedBanks(user(),false,page,size).stream().map(this::view).toList(),page,size,repository.scopedBankCount(user(),false));
    }
    public BankView updateBank(UUID id,BankInput input) {
        Bank b=ownedBank(id); active(b.active); b.name=input.name().trim(); return view(b);
    }
    public void deactivateBank(UUID id) { ownedBank(id).active=false; }

    private void apply(Question q,QuestionInput input) {
        if(input.options().stream().filter(OptionInput::correct).count()!=1)
            throw new BackendException(BAD_REQUEST,"Debe haber exactamente una opción correcta");
        long distinct=input.options().stream().map(o->o.text().trim().toLowerCase(Locale.ROOT)).distinct().count();
        if(distinct!=input.options().size()) throw new BackendException(BAD_REQUEST,"Las opciones deben ser distintas");
        q.statement=input.statement().trim(); q.explanation=input.explanation();
        q.options.clear();
        input.options().forEach(o->q.options.add(new Option(o.text().trim(),o.correct())));
    }
    public QuestionView createQuestion(UUID bankId,QuestionInput input) {
        active(ownedBank(bankId).active);
        Question q=new Question(); q.bankId=bankId; apply(q,input); repository.save(q); return view(q);
    }
    public QuestionView getQuestion(UUID id) { Question q=repository.get(Question.class,id);ownedBank(q.bankId);return view(q); }
    public PageView<QuestionView> questions(UUID bankId,int page,int size) {
        pagination(page,size); ownedBank(bankId);
        return new PageView<>(repository.questionPage(bankId,page,size).stream().map(this::view).toList(),
            page,size,repository.questionCount(bankId));
    }
    public QuestionView updateQuestion(UUID id,QuestionInput input) {
        Question q=repository.get(Question.class,id); active(q.active);
        active(ownedBank(q.bankId).active); apply(q,input); return view(q);
    }
    public void deactivateQuestion(UUID id) { Question q=repository.get(Question.class,id);ownedBank(q.bankId);q.active=false; }

    public QuizView createQuiz(QuizInput input) {
        active(ownedBank(input.bankId()).active);
        Quiz q=new Quiz(); q.ownerId=user();q.bankId=input.bankId(); q.name=input.name().trim(); q.questionCount=input.questionCount();
        repository.save(q); return view(q);
    }
    public QuizView getQuiz(UUID id) { return view(ownedQuiz(id)); }
    public PageView<QuizView> quizzes(int page,int size) {
        pagination(page,size);
        return new PageView<>(repository.scopedPage(Quiz.class,user(),page,size).stream().map(this::view).toList(),page,size,repository.scopedCount(Quiz.class,user()));
    }
    public QuizView updateQuiz(UUID id,QuizInput input) {
        Quiz q=ownedQuiz(id); active(q.active);
        active(ownedBank(input.bankId()).active);
        q.bankId=input.bankId(); q.name=input.name().trim(); q.questionCount=input.questionCount(); return view(q);
    }
    public void deactivateQuiz(UUID id) { ownedQuiz(id).active=false; }

    public BankAvailabilityView availability(UUID id) {
        Bank bank=readableBank(id);
        return new BankAvailabilityView(bank.id,bank.name,bank.active,repository.activeQuestionCount(id),bank.status);
    }
    public AttemptView startBankAttempt(UUID id,StartAttemptInput input) {
        Bank bank=practiceBank(id);
        Quiz quiz=new Quiz();quiz.ownerId=user();quiz.bankId=id;quiz.name=bank.name;quiz.questionCount=input.questionCount();repository.save(quiz);
        AttemptView started=start(quiz.id);
        Attempt attempt=repository.get(Attempt.class,started.id());
        attempt.participantName=(current.complete().name+" "+current.complete().lastName).trim();
        return attempt(attempt);
    }
    public AttemptView start(UUID quizId) {
        Quiz quiz=ownedQuiz(quizId); active(quiz.active);
        practiceBank(quiz.bankId);
        List<Question> pool=new ArrayList<>(repository.questions(quiz.bankId,true));
        if(pool.size()<quiz.questionCount) throw new BackendException(CONFLICT,
            "Preguntas activas disponibles: "+pool.size()+"; solicitadas: "+quiz.questionCount);
        Collections.shuffle(pool);
        Attempt a=new Attempt(); a.ownerId=user();a.participantName=(current.complete().name+" "+current.complete().lastName).trim(); a.quizId=quiz.id; a.quizName=quiz.name; repository.save(a);
        for(int i=0;i<quiz.questionCount;i++) {
            Question source=pool.get(i); AttemptQuestion copy=new AttemptQuestion();
            copy.attemptId=a.id; copy.sourceQuestionId=source.id; copy.position=i;
            copy.statement=source.statement; copy.explanation=source.explanation;
            source.options.forEach(o->copy.options.add(new Option(o.text,o.correct)));
            repository.save(copy);
        }
        return attempt(a);
    }
    public AttemptView getAttempt(UUID id) { return attempt(ownedAttempt(id)); }
    public PageView<AttemptView> attempts(int page,int size) {
        pagination(page,size);
        return new PageView<>(repository.scopedPage(Attempt.class,user(),page,size).stream().map(this::attempt).toList(),page,size,repository.scopedCount(Attempt.class,user()));
    }
    public AttemptView answer(UUID attemptId,UUID questionId,AnswerInput input) {
        Attempt a=repository.lockAttempt(attemptId);own(a.ownerId);
        if(a.finishedAt!=null) throw new BackendException(CONFLICT,"El intento ya está finalizado");
        AttemptQuestion q=repository.get(AttemptQuestion.class,questionId);
        if(!q.attemptId.equals(a.id)) throw new BackendException(NOT_FOUND,"Pregunta no encontrada en este intento");
        if(input.optionIndex()<0 || input.optionIndex()>=q.options.size())
            throw new BackendException(BAD_REQUEST,"Índice de opción inválido");
        q.selectedOption=input.optionIndex(); return attempt(a);
    }
    public AttemptView finish(UUID id) {
        Attempt a=repository.lockAttempt(id);own(a.ownerId);
        if(a.finishedAt==null) {
            a.score=(int)repository.snapshots(a.id).stream().filter(q->q.selectedOption!=null && q.options.get(q.selectedOption).correct).count();
            a.finishedAt=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        }
        return attempt(a);
    }
    private AttemptView attempt(Attempt a) {
        boolean finished=a.finishedAt!=null;
        List<AttemptQuestion> questions=repository.snapshots(a.id);
        List<AttemptQuestionView> views=questions.stream().map(q->new AttemptQuestionView(q.id,q.statement,
            IntStream.range(0,q.options.size()).mapToObj(i->new PublicOption(i,q.options.get(i).text)).toList(),
            q.selectedOption,finished?IntStream.range(0,q.options.size()).filter(i->q.options.get(i).correct).findFirst().orElseThrow():null,
            finished?q.explanation:null)).toList();
        return new AttemptView(a.id,a.quizId,a.quizName,finished?"FINALIZADO":"ABIERTO",a.startedAt,a.finishedAt,
            a.score,questions.size(),finished?100.0*a.score/questions.size():null,views,a.participantName);
    }
}
