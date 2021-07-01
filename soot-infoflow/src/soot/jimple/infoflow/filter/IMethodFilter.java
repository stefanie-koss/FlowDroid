package soot.jimple.infoflow.filter;

import java.util.List;

public interface IMethodFilter {

	/**
	 * Return if the given method should be skipped
	 * 
	 * @param signature
	 * @return
	 */
	public boolean skipMethod(String signature);

	public List<String> getInsecureMethods();
}
