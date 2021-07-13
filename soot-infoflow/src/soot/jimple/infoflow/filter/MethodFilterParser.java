package soot.jimple.infoflow.filter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodFilterParser implements IMethodFilter {

	private final String fileName;
	// Signatures of methods that are assumed to be safe. This means, that within
	// the method no taint on a parameter reaches a sink.
	List<String> methodsToSkip = new ArrayList<>();

	// Signatures of methods that contain a taint flow from a parameter to a sink
	List<String> insecureMethods = new ArrayList<>();

	private final String regex = "^(.+),(.+)$";

	protected MethodFilterParser(String fileName) {
		this.fileName = fileName;
//		this.fileName = "/home/koss/repositories/vulnerabilityclonedetection/candidate-search/out/com.roamingsquirrel.android.calculator.apk.txt";
	}

	public static MethodFilterParser fromFile(String fileName) {
		MethodFilterParser filter = new MethodFilterParser(fileName);
		try {
			filter.parse();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return filter;
	}

	public void parse() throws IOException {

		BufferedReader rdr = readFile();
		if (rdr == null)
			throw new RuntimeException("Could not read filter file");

		String line = null;
		Pattern p = Pattern.compile(regex);

		while ((line = rdr.readLine()) != null) {
			Matcher m = p.matcher(line);
			if (m.find()) {
				String sig = m.group(1);
				if (!sig.startsWith("<"))
					sig = "<" + sig;
				if (!sig.endsWith(">"))
					sig = sig + ">";
				String type = m.group(2);

				if (type.equals("NoSQLi") || type.equals("NoSink")) {
					methodsToSkip.add(sig);
				}
				if (type.equals("SQLi")) {
					insecureMethods.add(sig);
				}
			}
		}

		try {
			if (rdr != null)
				rdr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BufferedReader readFile() {
		FileReader fr = null;
		BufferedReader br = null;
		try {
			fr = new FileReader(fileName);
			br = new BufferedReader(fr);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}

		return br;
	}

	/**
	 * Return if the given method should be skipped
	 * 
	 * @param signature
	 * @return
	 */
	public boolean skipMethod(String signature) {
		if (methodsToSkip.contains(signature))
			return true;
		return false;
	}

	@Override
	public List<String> getInsecureMethods() {
		return insecureMethods;
	}

}
