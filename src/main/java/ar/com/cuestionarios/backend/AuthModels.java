package ar.com.cuestionarios.backend;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.UUID;
import java.time.Instant;
public final class AuthModels {
 private AuthModels(){}
 public record AcademicInput(@NotBlank @Size(max=100) String university,@NotBlank @Size(max=100) String faculty,@NotBlank @Size(max=100) String career,@Size(max=1000) String note){}
 public record RegisterInput(@NotBlank @Size(max=200) String name,@NotBlank @Size(max=200) String lastName,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=72) String password,@NotNull @Valid AcademicInput academic){}
 public record ProfileInput(@NotBlank @Size(max=200) String name,@NotBlank @Size(max=200) String lastName,@NotNull @Valid AcademicInput academic){}
 public record LoginInput(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=72) String password){}
 public record UserView(UUID id,String name,String lastName,String email,String university,String faculty,String career,String academicNote,boolean profileComplete,boolean googleLinked,Instant createdAt){}
}
