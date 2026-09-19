package ar.com.cuestionarios.backend;

import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.*;
import static org.springframework.http.HttpStatus.*;

@Repository
class BackendRepository {
    @PersistenceContext EntityManager em;
    <T> T get(Class<T> type,UUID id) {
        T value=em.find(type,id);
        if(value==null) throw new BackendException(NOT_FOUND,"Recurso no encontrado");
        return value;
    }
    Attempt lockAttempt(UUID id) {
        Attempt value=em.find(Attempt.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(value==null) throw new BackendException(NOT_FOUND,"Intento no encontrado");
        return value;
    }
    Optional<UserAccount> userByEmail(String email){return em.createQuery("select u from UserAccount u where u.email=:email",UserAccount.class).setParameter("email",email).getResultStream().findFirst();}
    Optional<UserAccount> userByGoogle(String sub){return em.createQuery("select u from UserAccount u where u.googleSubject=:sub",UserAccount.class).setParameter("sub",sub).getResultStream().findFirst();}
    void flush(){em.flush();}
    void health(){em.createNativeQuery("select 1",Integer.class).getSingleResult();}
    void save(Object value) { em.persist(value); }
    Optional<Career> careerByName(String name){return em.createQuery("select c from Career c where lower(c.name)=lower(:name)",Career.class).setParameter("name",name).getResultStream().findFirst();}
    Optional<Subject> subjectByName(UUID careerId,String name){return em.createQuery("select s from Subject s where s.careerId=:careerId and lower(s.name)=lower(:name)",Subject.class).setParameter("careerId",careerId).setParameter("name",name).getResultStream().findFirst();}
    Optional<Tag> tagBySlug(String slug){return em.createQuery("select t from Tag t where t.slug=:slug",Tag.class).setParameter("slug",slug).getResultStream().findFirst();}
    List<Career> careers(Boolean active){String jpql="select c from Career c"+(active==null?"":" where c.active=:active")+" order by c.name";var q=em.createQuery(jpql,Career.class);if(active!=null)q.setParameter("active",active);return q.getResultList();}
    List<Subject> subjects(UUID careerId,Integer year,Boolean active){StringBuilder jpql=new StringBuilder("select s from Subject s where 1=1");if(careerId!=null)jpql.append(" and s.careerId=:careerId");if(year!=null)jpql.append(" and s.studyYear=:year");if(active!=null)jpql.append(" and s.active=:active");jpql.append(" order by s.studyYear,s.name");var q=em.createQuery(jpql.toString(),Subject.class);if(careerId!=null)q.setParameter("careerId",careerId);if(year!=null)q.setParameter("year",year);if(active!=null)q.setParameter("active",active);return q.getResultList();}
    List<Tag> tags(String search){String value=search==null?"":search.trim().toLowerCase(Locale.ROOT);return em.createQuery("select t from Tag t where :search='' or lower(t.name) like :pattern or t.slug like :pattern order by t.name",Tag.class).setParameter("search",value).setParameter("pattern","%"+value+"%").setMaxResults(30).getResultList();}
    private String banksWhere(boolean practice){return practice?"b.active=true and b.status<>'BORRADOR' and (b.ownerId=:uid or b.status='PUBLICO')":"b.ownerId=:uid";}
    List<Bank> scopedBanks(UUID uid,boolean practice,int page,int size){return em.createQuery("select b from Bank b where "+banksWhere(practice)+" order by b.id",Bank.class).setParameter("uid",uid).setFirstResult(page*size).setMaxResults(size).getResultList();}
    long scopedBankCount(UUID uid,boolean practice){return em.createQuery("select count(b) from Bank b where "+banksWhere(practice),Long.class).setParameter("uid",uid).getSingleResult();}
    private String filteredWhere(boolean practice,ApiModels.BankFilters f){StringBuilder where=new StringBuilder(banksWhere(practice));if(f.careerId()!=null)where.append(" and b.subjectId in (select s.id from Subject s where s.careerId=:careerId)");if(f.subjectId()!=null)where.append(" and b.subjectId=:subjectId");if(f.year()!=null)where.append(" and b.subjectId in (select s.id from Subject s where s.studyYear=:year)");if(f.evaluationType()!=null)where.append(" and b.evaluationType=:evaluationType");if(f.evaluationNumber()!=null)where.append(" and b.evaluationNumber=:evaluationNumber");if(f.tags()!=null&&!f.tags().isEmpty())where.append(" and (select count(distinct t.slug) from Bank tagged join tagged.tags t where tagged.id=b.id and t.slug in :tagSlugs)=:tagCount");return where.toString();}
    private void filterParams(Query q,UUID uid,ApiModels.BankFilters f){q.setParameter("uid",uid);if(f.careerId()!=null)q.setParameter("careerId",f.careerId());if(f.subjectId()!=null)q.setParameter("subjectId",f.subjectId());if(f.year()!=null)q.setParameter("year",f.year());if(f.evaluationType()!=null)q.setParameter("evaluationType",EvaluationType.valueOf(f.evaluationType()));if(f.evaluationNumber()!=null)q.setParameter("evaluationNumber",f.evaluationNumber());if(f.tags()!=null&&!f.tags().isEmpty()){q.setParameter("tagSlugs",f.tags());q.setParameter("tagCount",(long)f.tags().size());}}
    List<Bank> filteredBanks(UUID uid,boolean practice,int page,int size,ApiModels.BankFilters filters){var q=em.createQuery("select b from Bank b where "+filteredWhere(practice,filters)+" order by b.id",Bank.class);filterParams(q,uid,filters);return q.setFirstResult(page*size).setMaxResults(size).getResultList();}
    long filteredBankCount(UUID uid,boolean practice,ApiModels.BankFilters filters){var q=em.createQuery("select count(b) from Bank b where "+filteredWhere(practice,filters),Long.class);filterParams(q,uid,filters);return q.getSingleResult();}
    <T> List<T> scopedPage(Class<T> type,UUID uid,int page,int size){return em.createQuery("select x from "+type.getSimpleName()+" x where x.ownerId=:uid order by x.id",type).setParameter("uid",uid).setFirstResult(page*size).setMaxResults(size).getResultList();}
    long scopedCount(Class<?> type,UUID uid){return em.createQuery("select count(x) from "+type.getSimpleName()+" x where x.ownerId=:uid",Long.class).setParameter("uid",uid).getSingleResult();}
    <T> List<T> page(Class<T> type,int page,int size) {
        return em.createQuery("select x from "+type.getSimpleName()+" x order by x.id",type)
            .setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList();
    }
    long count(Class<?> type) {
        return em.createQuery("select count(x) from "+type.getSimpleName()+" x",Long.class).getSingleResult();
    }
    List<Question> questions(UUID bankId,boolean onlyActive) {
        return em.createQuery("select q from Question q where q.bankId=:id"+(onlyActive?" and q.active=true":"")+" order by q.id",Question.class)
            .setParameter("id",bankId).getResultList();
    }
    List<Question> questionPage(UUID bankId,int page,int size) {
        return em.createQuery("select q from Question q where q.bankId=:id and q.active=true order by q.id",Question.class)
            .setParameter("id",bankId).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList();
    }
    long questionCount(UUID bankId) {
        return em.createQuery("select count(q) from Question q where q.bankId=:id and q.active=true",Long.class)
            .setParameter("id",bankId).getSingleResult();
    }
    long activeQuestionCount(UUID bankId) {
        return em.createQuery("select count(q) from Question q where q.bankId=:id and q.active=true",Long.class)
            .setParameter("id",bankId).getSingleResult();
    }
    List<AttemptQuestion> snapshots(UUID attemptId) {
        return em.createQuery("select q from AttemptQuestion q where q.attemptId=:id order by q.position",AttemptQuestion.class)
            .setParameter("id",attemptId).getResultList();
    }
}
