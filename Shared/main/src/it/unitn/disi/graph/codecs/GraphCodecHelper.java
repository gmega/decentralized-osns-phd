package it.unitn.disi.graph.codecs;

import it.unitn.disi.utils.ResettableFileInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * {@link GraphCodecHelper} contains utility methods for helping with
 * reflectively creating codec objects.
 * 
 * @author giuliano
 */
public class GraphCodecHelper {

	public static GraphEncoder createEncoder(OutputStream stream,
			String encoderClass) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, IOException {
		return (GraphEncoder) instantiate(encoderClass, OutputStream.class,
				stream);
	}

	public static ResettableGraphDecoder createDecoder(String file,
			String decoderClass) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, IOException {
		return createDecoder(new ResettableFileInputStream(new File(file)),
				decoderClass);
	}

	public static ResettableGraphDecoder createDecoder(InputStream stream,
			String decoderClass) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, IOException {

		return (ResettableGraphDecoder) instantiate(decoderClass,
				InputStream.class, stream);
	}

	@SuppressWarnings("unchecked")
	private static Object instantiate(String codec, Class parameterType,
			Object parameter) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, IOException {

		Class klass = Class.forName(codec);
		Constructor constructor = klass.getConstructor(parameterType);
		return constructor.newInstance(parameter);
	}
}
