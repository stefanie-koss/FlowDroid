package soot.jimple.infoflow.methodSummary.data.summary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSink;
import soot.jimple.infoflow.methodSummary.data.sourceSink.FlowSource;
import soot.util.HashMultiMap;
import soot.util.MultiMap;

public class Generalization {
	private MultiMap<String, MethodFlow> methodFlows = new HashMultiMap<>(); // Implementing class to flows (maybe
																				// MethodSummaries instead of flows
																				// later
	Set<FlowEquivalenceClass> equivalenceClasses = new HashSet<>();
	private final String signature;

	/**
	 * TODO: class for source/sink equivalences, Idea: Build classes of fields that
	 * can not be differentiated from interface point of view
	 * 
	 * @author koss
	 *
	 * @param <E>
	 */
	abstract class SourceSinkEquivalenceClass<E> {
		private Set<E> equivalenceClass = new HashSet<>();

		public SourceSinkEquivalenceClass() {
		};

		public boolean add(E element) {
			return equivalenceClass.add(element);
		}

		public boolean contains(E element) {
			return equivalenceClass.contains(element);
		}
	}

	/**
	 * TODO: class for source equivalences
	 * 
	 * @author koss
	 *
	 */
	class SourceEquivalenceClass extends SourceSinkEquivalenceClass<FlowSource> {

	}

	/**
	 * TODO: class for sink equivalences
	 * 
	 * @author koss
	 *
	 */
	class SinkEquivalenceClass extends SourceSinkEquivalenceClass<FlowSink> {

	}

	/**
	 * Represents flows that are equivalent for generalization sorted by
	 * implementing classes.
	 * 
	 * @author koss
	 *
	 */
	class FlowEquivalenceClass {
		private static final String DUMMY_BASE_TYPE = "genericField";

		private MultiMap<String, MethodFlow> flows = new HashMultiMap<>();
		private final FlowSource genericSource;
		private final FlowSink genericSink;
		private final boolean isAlias;
		private final boolean typeChecking;
		private final boolean cutSubFields;

		/**
		 * Creates a generic flow for this.
		 * 
		 * @param signature
		 * @return
		 */
		public MethodFlow toFlow(String signature) {
			List<String> implementors = new ArrayList<>(flows.keySet());
			return new MethodFlow(signature, genericSource, genericSink, isAlias, typeChecking, cutSubFields,
					implementors); // TODO: restore outcommented code

		}

		public FlowEquivalenceClass(String implementor, MethodFlow flow) {
			SourceSinkType sourceType = flow.source().getType();
			String sourceBaseType = (sourceType.equals(SourceSinkType.Field)) ? DUMMY_BASE_TYPE
					: flow.source().getBaseType();
			int sourceParameterIdx = flow.source().getParameterIndex();
			SourceSinkType sinkType = flow.sink().getType();
			String sinkBaseType = (sinkType.equals(SourceSinkType.Field)) ? DUMMY_BASE_TYPE : flow.sink().getBaseType();
			int sinkParameterIdx = flow.sink().getParameterIndex();
			boolean taintSubFields = flow.sink().taintSubFields();
			this.isAlias = flow.isAlias();
			this.typeChecking = flow.getTypeChecking();
			this.cutSubFields = flow.getCutSubFields();
			this.genericSource = new FlowSource(sourceType, sourceParameterIdx, sourceBaseType);
			this.genericSink = new FlowSink(sinkType, sinkParameterIdx, sinkBaseType, taintSubFields);
			this.flows.put(implementor, flow);
		}

		public boolean add(String implementor, MethodFlow flow) {
			if (!isEquivalent(flow))
				return false;
			return flows.put(implementor, flow);
		}

		public boolean contains(String implementor, MethodFlow flow) {
			return flows.contains(implementor, flow);
		}

		/**
		 * Checks if the given source is equivalent to the genericSource of this.
		 * 
		 * @param source
		 * @return
		 */
		public boolean isSourceEquivalent(FlowSource source) {
			return (genericSource.getType().equals(source.getType())
					&& genericSource.getParameterIndex() == source.getParameterIndex());
		}

		/**
		 * Checks if the given sink is equivalent to the genericSink of this.
		 * 
		 * @param sink
		 * @return
		 */
		public boolean isSinkEquivalent(FlowSink sink) {
			return (genericSink.getType().equals(sink.getType())
					&& genericSink.getParameterIndex() == sink.getParameterIndex()
					&& genericSink.taintSubFields() == sink.taintSubFields());
		}

		/**
		 * Checks if the given flow is equivalent to the flows represented by this.
		 * 
		 * @param flow
		 * @return
		 */
		public boolean isEquivalent(MethodFlow flow) {
			// check for baseType would be redundant, would need exception for fields
			return (isSourceEquivalent(flow.source()) && isSinkEquivalent(flow.sink()) && isAlias == flow.isAlias()
					&& typeChecking == flow.getTypeChecking() && cutSubFields == flow.getCutSubFields());
		}

	}

	public Generalization(String signature) {
		this.signature = signature;
	}

	public String getSignature() {
		return this.signature;
	}

	public Set<String> getImplementors() {
		return this.methodFlows.keySet();
	}

	public Set<MethodFlow> getMethodFlowsForImplementor(String className) {
		return methodFlows.get(className);
	}

	public boolean addMethodFlowsForClass(String className, Set<MethodFlow> flows) {
		for (MethodFlow flow : flows) {
			if (flow.source().hasGap() || flow.sink().hasGap())
				return false;
		}

		return methodFlows.putAll(className, flows);
	}

	/**
	 * Builds equivalenceClasses from methodFlows
	 */
	public void generalizeMethods() {
		for (String implementor : getImplementors()) {
			Set<MethodFlow> methodFlows = getMethodFlowsForImplementor(implementor);
			for (MethodFlow flow : methodFlows) {
				boolean needsNewClass = true;
				for (FlowEquivalenceClass eq : equivalenceClasses) {
					if (eq.isEquivalent(flow)) {
						eq.add(implementor, flow);
						needsNewClass = false;
						continue;
					}
				}
				if (needsNewClass) {
					FlowEquivalenceClass newClass = new FlowEquivalenceClass(implementor, flow);
					equivalenceClasses.add(newClass);
				}
			}
		}
	}

	public MethodSummaries getGeneralizationFromEquivalenceClasses() {
		Set<MethodFlow> flows = new HashSet<>();
		for (FlowEquivalenceClass fc : this.equivalenceClasses) {
			flows.add(fc.toFlow(getSignature()));
		}
		return new MethodSummaries(flows);
	}
}
