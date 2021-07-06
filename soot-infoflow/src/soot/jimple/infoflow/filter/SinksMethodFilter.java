package soot.jimple.infoflow.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;
import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinitionProvider;
import soot.jimple.infoflow.sourcesSinks.definitions.MethodSourceSinkDefinition;
import soot.util.Chain;

/**
 * Filter which treats all methods with a call to a sink method as unsaft
 * methods and all others as safe. NOTE: there must be an existing Soot Scene
 * before instantiating this class. Otherwise the filter will be empty.
 * 
 * @author koss
 *
 */
public class SinksMethodFilter implements IMethodFilter {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private List<String> sinks = new ArrayList<>();
	private List<String> methodsWithSinks = new ArrayList<>();
	private List<String> methodsWithoutSinks = new ArrayList<>();

	public void collectSinks(ISourceSinkDefinitionProvider sourceSinkProvider) {
		for (ISourceSinkDefinition def : sourceSinkProvider.getSinks()) {
			if (def instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
				if (Scene.v().containsMethod(methodDef.getMethod().getSignature())) {
					SootMethod method = Scene.v().getMethod(methodDef.getMethod().getSignature());
					sinks.add(method.getSignature());
				}
			}
		}
	}

	public void collectMethodsWithSinks(ISourceSinkDefinitionProvider sourceSinkProvider) {
		Set<SootMethod> sinkMethods = new HashSet<>();
		for (ISourceSinkDefinition def : sourceSinkProvider.getSinks()) {
			if (def instanceof MethodSourceSinkDefinition) {
				MethodSourceSinkDefinition methodDef = (MethodSourceSinkDefinition) def;
				if (Scene.v().containsMethod(methodDef.getMethod().getSignature())) {
					SootMethod method = Scene.v().getMethod(methodDef.getMethod().getSignature());
					sinkMethods.add(method);
				}
			}
		}
		collectMethodsWithSinks(sinkMethods);
	}

	private void collectMethodsWithSinks(Collection<SootMethod> sinkMethods) {
		// Collect soot methods that are sinks.
		// TODO überarbeiten

		Chain<SootClass> classes = Scene.v().getApplicationClasses();
		for (SootClass c : classes) {
			if (c.getPackageName().startsWith("android") || c.getPackageName().startsWith("java.")
					|| c.getPackageName().startsWith("com.google.android")
					|| c.getPackageName().startsWith("com.google.firebase")) {
				continue;
			}

//						Scene.v().forceResolve(c, SootClass.BODIES);
			List<SootMethod> methods = new ArrayList<>(c.getMethods());
			for (SootMethod m : methods) {
				if (!m.isConcrete()) {
					continue;
				}
				Body methodBody = null;
				try {
					methodBody = m.retrieveActiveBody();
				} catch (Exception e) {
					logger.error("\tFailed to retrieve active body of: " + m.getSignature());
					System.err.println("Continuing computation...");
					continue;
				}
				// Iterate over units to add only methods containing calls to sink methods
				Iterator<Unit> it_units = methodBody.getUnits().snapshotIterator();
				while (it_units.hasNext()) {
					Unit unit = it_units.next();
					Stmt stmt = (Stmt) unit;
					if (stmt.containsInvokeExpr()) {
						InvokeExpr invoke = stmt.getInvokeExpr();
						SootMethod method = invoke.getMethod();
						if (sinkMethods.contains(method)) {
							methodsWithSinks.add(m.getSignature());
							break;
						}
					}
				}

				// method does not contain sink
				methodsWithoutSinks.add(m.getSignature());

			}
		}
	}

	@Override
	public boolean skipMethod(String signature) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<String> getInsecureMethods() {
		// return methodsWithSinks;
		return sinks;
	}

	public List<String> getMethodsWithoutSinks() {
		return methodsWithoutSinks;
	}

}
