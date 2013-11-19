package it.unitn.disi.utils;

/**
 * Helper class for doing multiple substitutions over strings without having to
 * copy them repeatedly.
 * 
 * @author giuliano
 */
public class SectionReplacer {

	private final StringBuffer fMold;

	private int fDelta;

	public SectionReplacer(String original) {
		fMold = new StringBuffer(original);
	}

	public void replace(int startIndex, int endIndex, String replacement) {
		fMold.replace(startIndex + fDelta, endIndex + fDelta + 1, replacement);
		fDelta += ((startIndex - endIndex - 1) + replacement.length());
	}
	
	public String toString() {
		return fMold.toString();
	}

}
