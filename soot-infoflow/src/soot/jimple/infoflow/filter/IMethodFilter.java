package soot.jimple.infoflow.filter;

public interface IMethodFilter {

	/**
	 * Return if the given method should be skipped
	 * 
	 * @param signature
	 * @return
	 */
	public boolean skipMethod(String signature);
}
