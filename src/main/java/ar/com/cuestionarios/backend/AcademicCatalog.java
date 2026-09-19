package ar.com.cuestionarios.backend;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import static org.springframework.http.HttpStatus.*;
import static ar.com.cuestionarios.backend.AuthModels.*;
@Service
class AcademicCatalog {
 record Entry(String id,String parent,String name,Integer durationYears){}
 final List<Entry> universities=new ArrayList<>(),faculties=new ArrayList<>(),careers=new ArrayList<>();
 AcademicCatalog() throws IOException {
  try(var in=AcademicCatalog.class.getResourceAsStream("/academic-catalog.tsv")){
   if(in==null)throw new IOException("Falta el catálogo académico");
   new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)).lines().skip(1).forEach(line->{
    var p=line.split("\t",-1);if(p.length!=5)throw new IllegalArgumentException("Catálogo inválido");
    var list=switch(p[0]){case "U"->universities;case "F"->faculties;case "C"->careers;default->throw new IllegalArgumentException("Tipo inválido");};
    list.add(new Entry(p[1],p[2],p[3],p[4].equals("-")?null:Integer.valueOf(p[4])));
   });
  }
 }
 List<Entry> search(List<Entry> list,String parent,String q){
  String term=Objects.toString(q,"").toLowerCase(Locale.ROOT);
  return list.stream().filter(e->parent==null||e.parent().equals(parent)).filter(e->e.name().toLowerCase(Locale.ROOT).contains(term)).sorted(Comparator.comparing(Entry::name)).toList();
 }
 void validate(AcademicInput i){
  if(i.university().equals("NONE")){if(!i.faculty().equals("NONE")||!i.career().equals("NONE"))invalid();return;}
  if(i.university().equals("MISSING")){if(!i.faculty().equals("MISSING")||!i.career().equals("MISSING")||i.note()==null||i.note().isBlank())invalid();return;}
  if(universities.stream().noneMatch(e->e.id().equals(i.university())))invalid();
  if(i.faculty().equals("NONE")){if(!i.career().equals("NONE"))invalid();return;}
  if(i.faculty().equals("MISSING")){if(!i.career().equals("MISSING")||i.note()==null||i.note().isBlank())invalid();return;}
  if(faculties.stream().noneMatch(e->e.id().equals(i.faculty())&&e.parent().equals(i.university())))invalid();
  if(i.career().equals("NONE"))return;
  if(i.career().equals("MISSING")){if(i.note()==null||i.note().isBlank())invalid();return;}
  if(careers.stream().noneMatch(e->e.id().equals(i.career())&&e.parent().equals(i.faculty())))invalid();
 }
 Entry validateSelection(String university,String faculty,String career,Integer year){
  boolean empty=Stream.of(university,faculty,career).allMatch(value->value==null||value.isBlank());
  if(empty){if(year!=null)invalidSelection();return null;}
  if(Stream.of(university,faculty,career).anyMatch(value->value==null||value.isBlank()))invalidSelection();
  if(universities.stream().noneMatch(e->e.id().equals(university)))invalidSelection();
  if(faculties.stream().noneMatch(e->e.id().equals(faculty)&&e.parent().equals(university)))invalidSelection();
  Entry selected=careers.stream().filter(e->e.id().equals(career)&&e.parent().equals(faculty)).findFirst().orElseThrow(()->new BackendException(BAD_REQUEST,"La carrera no pertenece a la facultad seleccionada."));
  if(year!=null&&(year<1||selected.durationYears()==null||year>selected.durationYears()))throw new BackendException(BAD_REQUEST,"El año debe estar dentro de la duración publicada para la carrera.");
  return selected;
 }
 void invalidSelection(){throw new BackendException(BAD_REQUEST,"Universidad, facultad y carrera deben seleccionarse juntas y corresponder entre sí.");}
 void invalid(){throw new BackendException(BAD_REQUEST,"La universidad, facultad y carrera deben corresponder entre sí. Si no aparecen, detallá los datos faltantes.");}
}
