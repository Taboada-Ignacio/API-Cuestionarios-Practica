package ar.com.cuestionarios.backend;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;
import static org.springframework.http.HttpStatus.*;
import static ar.com.cuestionarios.backend.ApiModels.*;

@RestController
@RequestMapping("/api/v1")
public class BackendController {
    private final BackendService service;
    BackendController(BackendService service) { this.service=service; }
    @GetMapping("/bancos/{id}/disponibilidad")
    BankAvailabilityView availability(@PathVariable UUID id) { return service.availability(id); }
    @PostMapping("/bancos/{id}/intentos") @ResponseStatus(CREATED)
    AttemptView startBankAttempt(@PathVariable UUID id,@Valid @RequestBody StartAttemptInput input) { return service.startBankAttempt(id,input); }
    @PutMapping("/bancos/{id}/estado") BankView status(@PathVariable UUID id,@Valid @RequestBody StatusInput input){return service.status(id,input);}
    @PostMapping("/bancos/carga") @ResponseStatus(CREATED)
    BankView loadBank(@Valid @RequestBody BankLoadInput input) { return service.loadBank(input); }
    @PostMapping("/bancos") @ResponseStatus(CREATED)
    BankView createBank(@Valid @RequestBody BankInput input) { return service.createBank(input); }
    @GetMapping("/bancos")
    PageView<BankView> banks(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="mine") String scope,
        @RequestParam(required=false) UUID carreraId,@RequestParam(required=false) UUID materiaId,@RequestParam(required=false) Integer anio,
        @RequestParam(required=false) String tipoEvaluacion,@RequestParam(required=false) Integer numeroEvaluacion,
        @RequestParam(required=false) String etiqueta,@RequestParam(required=false) List<String> etiquetas) {
        if(!scope.equals("mine")&&!scope.equals("practice"))throw new BackendException(BAD_REQUEST,"scope debe ser mine o practice");
        List<String> tags=new java.util.ArrayList<>();if(etiquetas!=null)tags.addAll(etiquetas);if(etiqueta!=null&&!etiqueta.isBlank())tags.add(etiqueta);
        return service.banks(page,size,scope.equals("practice"),new BankFilters(carreraId,materiaId,anio,tipoEvaluacion,numeroEvaluacion,tags));
    }
    @GetMapping("/bancos/{id}") BankView bank(@PathVariable UUID id) { return service.getBank(id); }
    @PutMapping("/bancos/{id}") BankView updateBank(@PathVariable UUID id,@Valid @RequestBody BankInput input) { return service.updateBank(id,input); }
    @DeleteMapping("/bancos/{id}") @ResponseStatus(NO_CONTENT) void deactivateBank(@PathVariable UUID id) { service.deactivateBank(id); }
    @GetMapping("/carreras") List<CareerView> careers(@RequestParam(required=false) Boolean activa){return service.careers(activa);}
    @GetMapping("/carreras/{id}") CareerView career(@PathVariable UUID id){return service.getCareer(id);}
    @PostMapping("/carreras") @ResponseStatus(CREATED) CareerView createCareer(@Valid @RequestBody CareerInput input){return service.createCareer(input);}
    @PutMapping("/carreras/{id}") CareerView updateCareer(@PathVariable UUID id,@Valid @RequestBody CareerInput input){return service.updateCareer(id,input);}
    @GetMapping("/materias") List<SubjectView> subjects(@RequestParam(required=false) UUID carreraId,@RequestParam(required=false) Integer anio,@RequestParam(required=false) Boolean activa){return service.subjects(carreraId,anio,activa);}
    @GetMapping("/carreras/{id}/materias") List<SubjectView> careerSubjects(@PathVariable UUID id,@RequestParam(required=false) Integer anio,@RequestParam(required=false) Boolean activa){return service.subjects(id,anio,activa);}
    @GetMapping("/materias/{id}") SubjectView subject(@PathVariable UUID id){return service.getSubject(id);}
    @PostMapping("/materias") @ResponseStatus(CREATED) SubjectView createSubject(@Valid @RequestBody SubjectInput input){return service.createSubject(input);}
    @PutMapping("/materias/{id}") SubjectView updateSubject(@PathVariable UUID id,@Valid @RequestBody SubjectInput input){return service.updateSubject(id,input);}
    @GetMapping("/etiquetas") List<TagView> tags(@RequestParam(required=false) String search){return service.tags(search);}
    @PostMapping("/etiquetas") @ResponseStatus(CREATED) TagView createTag(@Valid @RequestBody TagInput input){return service.createTag(input);}
    @PostMapping("/bancos/{id}/preguntas") @ResponseStatus(CREATED)
    QuestionView createQuestion(@PathVariable UUID id,@Valid @RequestBody QuestionInput input) { return service.createQuestion(id,input); }
    @GetMapping("/bancos/{id}/preguntas")
    PageView<QuestionView> questions(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.questions(id,page,size); }
    @GetMapping("/preguntas/{id}") QuestionView question(@PathVariable UUID id) { return service.getQuestion(id); }
    @PutMapping("/preguntas/{id}") QuestionView updateQuestion(@PathVariable UUID id,@Valid @RequestBody QuestionInput input) { return service.updateQuestion(id,input); }
    @DeleteMapping("/preguntas/{id}") @ResponseStatus(NO_CONTENT) void deactivateQuestion(@PathVariable UUID id) { service.deactivateQuestion(id); }
    @PostMapping("/cuestionarios") @ResponseStatus(CREATED) QuizView createQuiz(@Valid @RequestBody QuizInput input) { return service.createQuiz(input); }
    @GetMapping("/cuestionarios")
    PageView<QuizView> quizzes(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.quizzes(page,size); }
    @GetMapping("/cuestionarios/{id}") QuizView quiz(@PathVariable UUID id) { return service.getQuiz(id); }
    @PutMapping("/cuestionarios/{id}") QuizView updateQuiz(@PathVariable UUID id,@Valid @RequestBody QuizInput input) { return service.updateQuiz(id,input); }
    @DeleteMapping("/cuestionarios/{id}") @ResponseStatus(NO_CONTENT) void deactivateQuiz(@PathVariable UUID id) { service.deactivateQuiz(id); }
    @PostMapping("/cuestionarios/{id}/intentos") @ResponseStatus(CREATED) AttemptView start(@PathVariable UUID id) { return service.start(id); }
    @GetMapping("/intentos")
    PageView<AttemptView> attempts(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.attempts(page,size); }
    @GetMapping("/intentos/{id}") AttemptView attempt(@PathVariable UUID id) { return service.getAttempt(id); }
    @PutMapping("/intentos/{id}/respuestas/{questionId}")
    AttemptView answer(@PathVariable UUID id,@PathVariable UUID questionId,@Valid @RequestBody AnswerInput input) { return service.answer(id,questionId,input); }
    @PostMapping("/intentos/{id}/finalizar") AttemptView finish(@PathVariable UUID id) { return service.finish(id); }
}
