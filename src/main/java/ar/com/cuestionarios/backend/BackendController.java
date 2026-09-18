package ar.com.cuestionarios.backend;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
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
    PageView<BankView> banks(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="mine") String scope) { if(!scope.equals("mine")&&!scope.equals("practice"))throw new BackendException(BAD_REQUEST,"scope debe ser mine o practice");return scope.equals("practice")?service.practiceBanks(page,size):service.banks(page,size); }
    @GetMapping("/bancos/{id}") BankView bank(@PathVariable UUID id) { return service.getBank(id); }
    @PutMapping("/bancos/{id}") BankView updateBank(@PathVariable UUID id,@Valid @RequestBody BankInput input) { return service.updateBank(id,input); }
    @DeleteMapping("/bancos/{id}") @ResponseStatus(NO_CONTENT) void deactivateBank(@PathVariable UUID id) { service.deactivateBank(id); }
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
