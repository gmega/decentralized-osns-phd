package it.unitn.disi.utils.logging;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Outputs {
	public StructuredLog [] logs();
}
