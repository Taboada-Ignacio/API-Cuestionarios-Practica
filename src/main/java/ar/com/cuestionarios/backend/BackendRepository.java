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
    private String banksWhere(boolean practice){return practice?"b.active=true and b.status<>'BORRADOR' and (b.ownerId=:uid or b.status='PUBLICO')":"b.ownerId=:uid";}
    List<Bank> scopedBanks(UUID uid,boolean practice,int page,int size){return em.createQuery("select b from Bank b where "+banksWhere(practice)+" order by b.id",Bank.class).setParameter("uid",uid).setFirstResult(page*size).setMaxResults(size).getResultList();}
    long scopedBankCount(UUID uid,boolean practice){return em.createQuery("select count(b) from Bank b where "+banksWhere(practice),Long.class).setParameter("uid",uid).getSingleResult();}
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
        return em.createQuery("select q from Question q where q.bankId=:id order by q.id",Question.class)
            .setParameter("id",bankId).setFirstResult(Math.multiplyExact(page,size)).setMaxResults(size).getResultList();
    }
    long questionCount(UUID bankId) {
        return em.createQuery("select count(q) from Question q where q.bankId=:id",Long.class)
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
