package ar.com.cuestionarios.backend;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1")
class AcademicController {
 private final AcademicCatalog catalog;private final BackendRepository repository;
 AcademicController(AcademicCatalog c,BackendRepository r){catalog=c;repository=r;}
 @GetMapping("/health") @org.springframework.transaction.annotation.Transactional(readOnly=true) Map<String,String> health(){repository.health();return Map.of("status","UP");}
 @GetMapping("/academia/universidades") List<AcademicCatalog.Entry> universities(@RequestParam(defaultValue="") String q){return catalog.search(catalog.universities,null,q);}
 @GetMapping("/academia/facultades") List<AcademicCatalog.Entry> faculties(@RequestParam String university,@RequestParam(defaultValue="") String q){return catalog.search(catalog.faculties,university,q);}
 @GetMapping("/academia/carreras") List<AcademicCatalog.Entry> careers(@RequestParam String faculty,@RequestParam(defaultValue="") String q){return catalog.search(catalog.careers,faculty,q);}
}
