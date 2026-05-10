package sd2526.trab.impl.db;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import sd2526.trab.api.java.Result;
import sd2526.trab.api.java.Result.ErrorCode;
import sd2526.trab.impl.db.Hibernate.HibernateSession;


public class DB {
	
	public static <T> Result<List<T>> select(String query, Class<T> clazz) {
		return Hibernate.getInstance().inSession( (session) -> session.select(query, clazz));
	}	
		
	public static <T> Result<T> getOne(Object id, Class<T> clazz) {
		return Hibernate.getInstance().inSession( (session) -> session.getOne(id, clazz));
	}

	public static <T> Result<T> deleteOne(T obj) {
		return Hibernate.getInstance().transaction( hibernate -> hibernate.deleteOne( obj ));
	}
	
	public static <T> Result<T> updateOne(T obj) {
		return Hibernate.getInstance().transaction( hibernate -> hibernate.updateOne( obj ) );
	}
	
	public static <T> Result<T> persistOne( T obj) {
		return Hibernate.getInstance().transaction( hibernate -> hibernate.persistOne( obj ) );
	}
	
	public static <T> Result<T> transaction( Function<HibernateSession, Result<T>> f) {
		return reTry( () -> Hibernate.getInstance().transaction(f));
	}

	private static <T> Result<T> reTry( Supplier<Result<T>> f) {
		Result<T> res;
		if( (res = f.get()) != null ) return res;
		if( (res = f.get()) != null ) return res;
		if( (res = f.get()) != null ) return res;
		return Result.error( ErrorCode.INTERNAL_ERROR );
	}
}
