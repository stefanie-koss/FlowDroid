package soot.jimple.infoflow.filter;

import java.util.ArrayList;
import java.util.List;

public class EmptyMethodFilter implements IMethodFilter {

	@Override
	public boolean skipMethod(String signature) {
		return false;
	}

	@Override
	public List<String> getInsecureMethods() {
		return new ArrayList<String>();
	}
}
