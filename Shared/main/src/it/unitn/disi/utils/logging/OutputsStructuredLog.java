package it.unitn.disi.utils.logging;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OutputsStructuredLog {
	public String key();
	public String [] fields();
}
