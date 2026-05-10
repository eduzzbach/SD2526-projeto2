package sd2526.trab.api.java;

import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 
 * Represents the result of an operation, either wrapping a result of the given type,
 * or an error.
 * 
 * @author smd
 *
 * @param <T> type of the result value associated with success
 */
public interface Result<T> {

	/**
	 * 
	 * @author smd
	 *
	 * Service errors:
	 * OK - no error, implies a non-null result of type T, except for for Void operations
	 * CONFLICT - something is being created but already exists
	 * NOT_FOUND - an access occurred to something that does not exist
	 * INTERNAL_ERROR - something unexpected happened
	 */
	enum ErrorCode{ OK, CONFLICT, NOT_FOUND, BAD_REQUEST, FORBIDDEN, INTERNAL_ERROR, NOT_IMPLEMENTED, TIMEOUT};
	
	/**
	 * Tests if the result is an error.
	 */
	boolean isOK();
	
	/**
	 * obtains the payload value of this result
	 * @return the value of this result.
	 */
	T value();

	/**
	 *
	 * obtains the error code of this result
	 * @return the error code
	 * 
	 */
	ErrorCode error();
	
	/**
	 * Convenience method for returning non error results of the given type
	 * @param Class of value of the result
	 * @return the value of the result
	 */
	static <T> Result<T> ok( T result ) {
		return new OkResult<>(result);
	}

	/**
	 * Convenience method for returning non error results without a value
	 * @return non-error result
	 */
	static <T> Result<T> ok() {
		return new OkResult<>(null);	
	}
	
	/**
	 * Convenience method used to return an error 
	 * @return
	 */
	static <T> Result<T> error(ErrorCode error) {
		return new ErrorResult<>(error);		
	}
	
	static <T> Result<T> error(Result<?> other) {
		return new ErrorResult<>(other.error());		
	}
	
	
	default <Q> Result<Q> mapValue( Function<T, Q> b) {
		if( this.isOK())
			return ok(b.apply(this.value()));
		else
			return error(this.error());
	}

	default <Q> Result<Q> mapResult( Function<T, Result<Q>> b) {
		if( ! this.isOK())
			return error(this);
			
		return b.apply(this.value());
	}
	
	default Result<T> orElse( Supplier<Result<T>> b) {
		if( this.isOK() )
			return this;
			
		else return b.get();
	}
	
	default <Q> Result<Q> thenWith( Function<T, Result<Q>> b) {
		if( ! this.isOK())
			return error(this);
			
		return b.apply(this.value());
	}
	
	
	default <Q> Result<Q> then( Supplier<Result<Q>> b) {
		if( ! this.isOK())
			return error( this.error() );

		return b.get();
	}
	
	default Result<T> then( Runnable r) {
		if( ! this.isOK()) 
			return error(this.error());		
		r.run();
		return this;
	}
	
	default Result<Void> mapToVoid() {
		if( this.isOK())
			return ok();
		else
			return error( this );
	}
		
	default Result<T> async( Consumer<T> c) {
		if( ! this.isOK()) 
			return error(this.error());		
		Executors.newSingleThreadExecutor().execute( () -> c.accept(this.value() ) );
		return this;
	}
}

/*
 * 
 */
class OkResult<T> implements Result<T> {

	final T result;
	
	OkResult(T result) {
		this.result = result;
	}
	
	@Override
	public boolean isOK() {
		return true;
	}

	@Override
	public T value() {
		return result;
	}

	@Override
	public ErrorCode error() {
		return ErrorCode.OK;
	}
	
	public String toString() {
		return String.format("(OK, %s)", value());
	}
}

class ErrorResult<T> implements Result<T> {

	final ErrorCode error;
	
	ErrorResult(ErrorCode error) {
		this.error = error;
	}
	
	@Override
	public boolean isOK() {
		return false;
	}

	@Override
	public T value() {
		throw new RuntimeException("Attempting to extract the value of an Error: " + error());
	}

	@Override
	public ErrorCode error() {
		return error;
	}
	
	public String toString() {
		return String.format("(%s)", error());
	}
}
