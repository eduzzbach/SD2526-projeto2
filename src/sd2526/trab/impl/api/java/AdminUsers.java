package sd2526.trab.impl.api.java;

import java.util.Collection;
import java.util.Set;

import sd2526.trab.api.java.Result;

public interface AdminUsers {
	
	/*
	 * Returns unknown users...
	 */
	Result<Set<String>> checkUsers( Collection<String> names);	
		
}
