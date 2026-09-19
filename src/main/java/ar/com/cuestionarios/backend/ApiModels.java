package ar.com.cuestionarios.backend;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class ApiModels {
    private ApiModels() {}
    public record BankLoadInput(String status, @NotBlank @Size(max=200) String name, @NotNull @Size(min=1,max=1000) List<@NotNull @Valid QuestionInput> questions,
        UUID subjectId,String evaluationType,@Min(1) Integer evaluationNumber,@Size(max=30) List<@NotBlank @Size(max=100) String> tags,
        String academicUniversityId,String academicFacultyId,String academicCareerId,@Min(1) Integer studyYear,@Size(max=200) String academicSubjectName) {
        public BankLoadInput(String name,List<QuestionInput> questions){this("BORRADOR",name,questions,null,null,null,List.of(),null,null,null,null,null);}
    }
    public record BankInput(@NotBlank @Size(max=200) String name,UUID subjectId,String evaluationType,@Min(1) Integer evaluationNumber,
        @Size(max=30) List<@NotBlank @Size(max=100) String> tags,String academicUniversityId,String academicFacultyId,String academicCareerId,@Min(1) Integer studyYear,@Size(max=200) String academicSubjectName) {
        public BankInput(String name){this(name,null,null,null,null,null,null,null,null,null);}
        public BankInput(String name,UUID subjectId,String evaluationType,Integer evaluationNumber,List<String> tags){this(name,subjectId,evaluationType,evaluationNumber,tags,null,null,null,null,null);}
    }
    public record CareerInput(@NotBlank @Size(max=200) String name,@Size(max=50) String code,Boolean active) {}
    public record CareerView(UUID id,String name,String code,boolean active,Instant createdAt,Instant updatedAt) {}
    public record SubjectInput(@NotNull UUID careerId,@NotBlank @Size(max=200) String name,@Size(max=50) String code,@Min(1) int studyYear,Boolean active) {}
    public record SubjectView(UUID id,String name,String code,int studyYear,boolean active,CareerView career,Instant createdAt,Instant updatedAt) {}
    public record TagInput(@NotBlank @Size(max=100) String name) {}
    public record TagView(UUID id,String name,String slug,Instant createdAt) {}
    public record BankFilters(UUID careerId,UUID subjectId,Integer year,String evaluationType,Integer evaluationNumber,List<String> tags) {}
    public record OptionInput(@NotBlank @Size(max=1000) String text, boolean correct) {}
    public record QuestionInput(@NotBlank @Size(max=20000) String statement,
        @Size(max=20000) String explanation,
        @NotNull @Size(min=2,max=10) List<@NotNull @Valid OptionInput> options) {}
    public record QuizInput(@NotNull UUID bankId, @NotBlank @Size(max=200) String name,
        @Min(1) @Max(1000) int questionCount) {}
    public record BankAvailabilityView(UUID id,String name,boolean active,long questionCount,String status) {}
    public record StartAttemptInput(@Min(1) @Max(1000) int questionCount, @Size(max=200) String participantName) {}
    public record AnswerInput(@NotNull @Min(0) Integer optionIndex) {}
    public record AcademicSelectionView(String universityId,String facultyId,String careerId,Integer studyYear,String subjectName) {}
    public record BankView(UUID id,String name,boolean active,UUID ownerId,String status,SubjectView subject,String evaluationType,Integer evaluationNumber,List<TagView> tags,AcademicSelectionView academic) {}
    public record StatusInput(@NotBlank String status) {}
    public record QuestionView(UUID id,UUID bankId,String statement,String explanation,
        List<OptionInput> options,boolean active) {}
    public record QuizView(UUID id,UUID bankId,String name,int questionCount,boolean active) {}
    public record PageView<T>(List<T> items,int page,int size,long total) {}
    public record PublicOption(int index,String text) {}
    public record AttemptQuestionView(UUID id,String statement,List<PublicOption> options,
        Integer selectedOption,Integer correctOption,String explanation) {}
    public record AttemptView(UUID id,UUID quizId,String quizName,String status,Instant startedAt,
        Instant finishedAt,Integer score,int total,Double percentage,List<AttemptQuestionView> questions,String participantName) {}
}
