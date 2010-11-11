package peersim.config;

/**
 * {@link IResolver} knows how to resolve an attribute value. The method it
 * exposes are all quite similar, taking a prefix (namespace), an attribute key,
 * and an {@link Attribute} annotation.
 * 
 * Methods throw {@link MissingParameterException} if the attribute cannot be
 * resolved.
 */
public interface IResolver {
	public Integer getInt(String prefix, String key);

	public Long getLong(String prefix, String key);

	public Float getFloat(String prefix, String key);

	public Double getDouble(String prefix, String key);

	public Boolean getBoolean(String prefix, String key);

	public String getString(String prefix, String key);
	
	public Object getObject(String prefix, String key);
}