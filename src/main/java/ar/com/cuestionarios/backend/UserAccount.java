package ar.com.cuestionarios.backend;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="app_users")
class UserAccount {
 @Id UUID id=UUID.randomUUID();
 @Column(nullable=false,length=200) String name;
 @Column(name="last_name",nullable=false,length=200) String lastName="";
 @Column(nullable=false,unique=true,length=254) String email;
 @Column(name="password_hash",length=100) String passwordHash;
 @Column(name="google_subject",unique=true,length=255) String googleSubject;
 @Column(length=100) String university;
 @Column(length=100) String faculty;
 @Column(length=100) String career;
 @Column(name="academic_note",length=1000) String academicNote;
 boolean active=true;
 @Column(name="created_at",nullable=false) Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) Instant updatedAt=Instant.now();
 protected UserAccount() {}
}
