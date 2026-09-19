package ar.com.cuestionarios.backend;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.IntStream;
import static org.springframework.http.HttpStatus.*;
import static ar.com.cuestionarios.backend.ApiModels.*;

@Service
@Transactional
public class BackendService {
    private final BackendRepository repository;
    private final CurrentUser current;
    private final AcademicCatalog academicCatalog;
    BackendService(BackendRepository repository,CurrentUser current,AcademicCatalog academicCatalog) { this.repository=repository;this.current=current;this.academicCatalog=academicCatalog; }
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
    private CareerView view(Career c){return new CareerView(c.id,c.name,c.code,c.active,c.createdAt,c.updatedAt);}
    private SubjectView view(Subject s){return new SubjectView(s.id,s.name,s.code,s.studyYear,s.active,view(repository.get(Career.class,s.careerId)),s.createdAt,s.updatedAt);}
    private TagView view(Tag t){return new TagView(t.id,t.name,t.slug,t.createdAt);}
    private BankView view(Bank b) { return new BankView(b.id,b.name,b.active,b.ownerId,b.status,b.subjectId==null?null:view(repository.get(Subject.class,b.subjectId)),b.evaluationType==null?null:b.evaluationType.name(),b.evaluationNumber,b.tags.stream().sorted(Comparator.comparing(t->t.name)).map(this::view).toList(),new AcademicSelectionView(b.academicUniversityId,b.academicFacultyId,b.academicCareerId,b.studyYear,b.academicSubjectName)); }
    private QuestionView view(Question q) {
        return new QuestionView(q.id,q.bankId,q.statement,q.explanation,
            q.options.stream().map(o->new OptionInput(o.text,o.correct)).toList(),q.active);
    }
    private QuizView view(Quiz q) { return new QuizView(q.id,q.bankId,q.name,q.questionCount,q.active); }

    public BankView loadBank(BankLoadInput input) {
        BankView bank=createBank(new BankInput(input.name(),input.subjectId(),input.evaluationType(),input.evaluationNumber(),input.tags(),input.academicUniversityId(),input.academicFacultyId(),input.academicCareerId(),input.studyYear(),input.academicSubjectName()));
        input.questions().forEach(question->createQuestion(bank.id(),question));
        if(input.status()!=null&&!input.status().equals("BORRADOR"))return status(bank.id(),new StatusInput(input.status()));
        return bank;
    }
    public BankView createBank(BankInput input) {
        Bank b=new Bank(); b.ownerId=user(); b.name=input.name().trim(); applyClassification(b,input); repository.save(b); return view(b);
    }
    public BankView getBank(UUID id) { return view(readableBank(id)); }
    public PageView<BankView> practiceBanks(int page,int size){pagination(page,size);UUID uid=user();return new PageView<>(repository.scopedBanks(uid,true,page,size).stream().map(this::view).toList(),page,size,repository.scopedBankCount(uid,true));}
    public PageView<BankView> banks(int page,int size) {
        pagination(page,size);
        return new PageView<>(repository.scopedBanks(user(),false,page,size).stream().map(this::view).toList(),page,size,repository.scopedBankCount(user(),false));
    }
    public PageView<BankView> banks(int page,int size,boolean practice,BankFilters filters){pagination(page,size);validateFilters(filters);BankFilters normalized=new BankFilters(filters.careerId(),filters.subjectId(),filters.year(),filters.evaluationType()==null?null:evaluation(filters.evaluationType()).name(),filters.evaluationNumber(),filters.tags()==null?List.of():filters.tags().stream().map(this::slug).distinct().toList());UUID uid=user();return new PageView<>(repository.filteredBanks(uid,practice,page,size,normalized).stream().map(this::view).toList(),page,size,repository.filteredBankCount(uid,practice,normalized));}
    public BankView updateBank(UUID id,BankInput input) {
        Bank b=ownedBank(id);active(b.active);b.name=input.name().trim();if(input.subjectId()!=null||input.evaluationType()!=null||input.evaluationNumber()!=null||input.tags()!=null||input.academicCareerId()!=null)applyClassification(b,input);return view(b);
    }
    public void deactivateBank(UUID id) { ownedBank(id).active=false; }

    private String clean(String value){return value==null?null:value.trim().replaceAll("\\s+"," ");}
    private String academicName(String value){String cleaned=clean(value);if(cleaned==null||cleaned.isBlank())return null;Set<String> lower=Set.of("a","al","con","de","del","el","en","e","la","las","los","o","para","por","sin","u","y");String[] words=cleaned.toLowerCase(Locale.forLanguageTag("es")).split(" ");for(int i=0;i<words.length;i++)if(i==0||!lower.contains(words[i]))words[i]=words[i].substring(0,1).toUpperCase(Locale.forLanguageTag("es"))+words[i].substring(1);return String.join(" ",words);}
    private String slug(String value){String normalized=Normalizer.normalize(clean(value),Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(normalized.isBlank())throw new BackendException(BAD_REQUEST,"La etiqueta debe contener letras o números");return normalized;}
    private EvaluationType evaluation(String value){if(value==null||value.isBlank())return null;try{return EvaluationType.valueOf(value.trim().toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){throw new BackendException(BAD_REQUEST,"Tipo de evaluación inválido");}}
    private Tag tag(String name){String cleaned=clean(name),slug=slug(name);return repository.tagBySlug(slug).orElseGet(()->{Tag tag=new Tag();tag.name=cleaned;tag.slug=slug;repository.save(tag);return tag;});}
    private void applyClassification(Bank bank,BankInput input){EvaluationType evaluation=evaluation(input.evaluationType());if(input.evaluationNumber()!=null&&evaluation==null)throw new BackendException(BAD_REQUEST,"El número de evaluación requiere un tipo de evaluación");if(input.evaluationNumber()!=null&&input.evaluationNumber()<1)throw new BackendException(BAD_REQUEST,"El número de evaluación debe ser mayor o igual a 1");Subject subject=input.subjectId()==null?null:repository.get(Subject.class,input.subjectId());if(subject!=null&&(!subject.active||!repository.get(Career.class,subject.careerId).active))throw new BackendException(CONFLICT,"La materia o su carrera están desactivadas");academicCatalog.validateSelection(input.academicUniversityId(),input.academicFacultyId(),input.academicCareerId(),input.studyYear());bank.subjectId=input.subjectId();bank.evaluationType=evaluation;bank.evaluationNumber=input.evaluationNumber();bank.academicUniversityId=input.academicUniversityId();bank.academicFacultyId=input.academicFacultyId();bank.academicCareerId=input.academicCareerId();bank.studyYear=input.studyYear();bank.academicSubjectName=academicName(input.academicSubjectName());bank.tags.clear();if(input.tags()!=null)input.tags().stream().map(this::tag).forEach(bank.tags::add);}
    private void validateFilters(BankFilters filters){if(filters.year()!=null&&filters.year()<1)throw new BackendException(BAD_REQUEST,"El año debe ser mayor o igual a 1");evaluation(filters.evaluationType());if(filters.evaluationNumber()!=null&&filters.evaluationNumber()<1)throw new BackendException(BAD_REQUEST,"El número debe ser mayor o igual a 1");}

    public CareerView createCareer(CareerInput input){if(repository.careerByName(clean(input.name())).isPresent())throw new BackendException(CONFLICT,"La carrera ya existe");Career c=new Career();c.name=clean(input.name());c.code=clean(input.code());if(input.active()!=null)c.active=input.active();repository.save(c);return view(c);}
    public CareerView getCareer(UUID id){return view(repository.get(Career.class,id));}
    public List<CareerView> careers(Boolean active){return repository.careers(active).stream().map(this::view).toList();}
    public CareerView updateCareer(UUID id,CareerInput input){Career c=repository.get(Career.class,id);repository.careerByName(clean(input.name())).filter(other->!other.id.equals(id)).ifPresent(other->{throw new BackendException(CONFLICT,"La carrera ya existe");});c.name=clean(input.name());c.code=clean(input.code());if(input.active()!=null)c.active=input.active();c.updatedAt=Instant.now();return view(c);}
    public SubjectView createSubject(SubjectInput input){Career career=repository.get(Career.class,input.careerId());active(career.active);if(repository.subjectByName(career.id,clean(input.name())).isPresent())throw new BackendException(CONFLICT,"La materia ya existe en esta carrera");Subject s=new Subject();apply(s,input);repository.save(s);return view(s);}
    private void apply(Subject s,SubjectInput input){if(input.studyYear()<1)throw new BackendException(BAD_REQUEST,"El año de cursado debe ser positivo");Career career=repository.get(Career.class,input.careerId());active(career.active);s.careerId=career.id;s.name=clean(input.name());s.code=clean(input.code());s.studyYear=input.studyYear();if(input.active()!=null)s.active=input.active();s.updatedAt=Instant.now();}
    public SubjectView getSubject(UUID id){return view(repository.get(Subject.class,id));}
    public List<SubjectView> subjects(UUID careerId,Integer year,Boolean active){if(careerId!=null)repository.get(Career.class,careerId);if(year!=null&&year<1)throw new BackendException(BAD_REQUEST,"El año debe ser positivo");return repository.subjects(careerId,year,active).stream().map(this::view).toList();}
    public SubjectView updateSubject(UUID id,SubjectInput input){Subject s=repository.get(Subject.class,id);repository.subjectByName(input.careerId(),clean(input.name())).filter(other->!other.id.equals(id)).ifPresent(other->{throw new BackendException(CONFLICT,"La materia ya existe en esta carrera");});apply(s,input);return view(s);}
    public TagView createTag(TagInput input){return view(tag(input.name()));}
    public List<TagView> tags(String search){return repository.tags(search).stream().map(this::view).toList();}

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
