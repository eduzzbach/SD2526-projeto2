package sd2526.trab.impl.db;


import static sd2526.trab.api.java.Result.error;
import static sd2526.trab.api.java.Result.ok;
import static sd2526.trab.api.java.Result.ErrorCode.CONFLICT;
import static sd2526.trab.api.java.Result.ErrorCode.INTERNAL_ERROR;
import static sd2526.trab.api.java.Result.ErrorCode.NOT_FOUND;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;

import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;



/**
 * A helper class to perform POJO (Plain Old Java Objects) persistence, using
 * Hibernate and a backing relational database.
 * 
 * @param <Session>
 */
public class Hibernate {

	private static final String HIBERNATE_CFG_FILE = "hibernate.cfg.xml";
	private SessionFactory sessionFactory;
	private static Hibernate instance;

	private Hibernate() {
		try {
			sessionFactory = new Configuration().configure(new File(HIBERNATE_CFG_FILE)).buildSessionFactory();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the Hibernate instance, initializing if necessary. Requires a
	 * configuration file (hibernate.cfg.xml)
	 * 
	 * @return
	 */
	synchronized public static Hibernate getInstance() {
		if (instance == null)
			instance = new Hibernate();
		return instance;
	}
	
	<T> Result<T> execute(Function<HibernateSession, Result<T>> func) {
		try (var session = sessionFactory.openSession()) {
			var res = func.apply( new HibernateSession(session) );
			session.flush();
			return res;
		} catch (Exception e) {
			return Result.error(ErrorCode.INTERNAL_ERROR);
		}
	}

	public <T> Result<T> inSession(Function<HibernateSession, Result<T>> func) {
		try (var session = sessionFactory.openSession()) {
			return func.apply( new HibernateSession(session) );
		}
		catch (ConstraintViolationException __) {
			return Result.error(ErrorCode.CONFLICT);
		}  
		catch (Exception e) {
			e.printStackTrace();			
			return error( INTERNAL_ERROR );
		}
	}
	
	
	public <T> Result<T> transaction(Function<HibernateSession, Result<T>> func) {
		Transaction tx = null;
		try (var session = sessionFactory.openSession()) {
			tx = session.beginTransaction();
			var res = func.apply( new HibernateSession(session) );
			session.flush();
			tx.commit();
			return res;
		}
		catch (ConstraintViolationException __) {
			return Result.error(ErrorCode.CONFLICT);
		}  
		catch (Exception e) {
			e.printStackTrace();
			
			if( tx != null ) {
				tx.rollback();
				if( tx.wasFailure() )
					return null;
			}

			return error( INTERNAL_ERROR );
		}
	}
	
	public static class HibernateSession {
		final Session session;
		HibernateSession( Session s ) {
			this.session = s;
		}
		
		public <T> Result<T> persistOne( T obj ) {
			try { 
				session.persist(obj);
				return ok( obj );
			} catch( ConstraintViolationException __) {
				return error( CONFLICT );
			}
		}
		
		public <T> Result<T> getOne(Object id, Class<T> clazz) {
			var res = session.find(clazz, id );				
			return res != null ? ok( res ) : error( NOT_FOUND );			
		}
		
		public <T> Result<T> updateOne( T obj ) {
			var res = session.merge( obj );
			return res != null ? ok( res ) : error( NOT_FOUND );			
		}
		
		public <T> Result<T> deleteOne( T obj ) {
			session.remove(obj);
			return Result.ok( obj );
		}
		
		public Result<Void> deleteMany( Collection<?> objects ) {
			for( var obj : objects)
				session.remove(obj);
			return ok();
		}
			
		public <T> Result<List<T>> select(String sqlStatement, Class<T> clazz) {
			try {
				var query = session.createNativeQuery(sqlStatement, clazz);
				return ok(query.list());
			} catch (Exception e) {
				return error(ErrorCode.INTERNAL_ERROR);
			}
		}
	}
}