package it.unitn.disi.graph.codecs;

import it.unitn.disi.utils.MiscUtils;
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
	
	// ----------------------------------------------------------------------
	// Encoders.
	// ----------------------------------------------------------------------
	
	public static GraphEncoder uncheckedCreateEncoder(OutputStream stream,
			String encoderClass) {
		try {
			return GraphCodecHelper.createEncoder(stream, encoderClass);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
	
	public static GraphEncoder createEncoder(OutputStream stream,
			String encoderClass) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, IOException {
		return (GraphEncoder) instantiate(encoderClass, OutputStream.class,
				stream);
	}
	
	// ----------------------------------------------------------------------
	// Decoders.
	// ----------------------------------------------------------------------
	
	public static ResettableGraphDecoder uncheckedCreateDecoder(InputStream stream,
			String decoderClass) {
		try {
			return GraphCodecHelper.createDecoder(stream, decoderClass);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object instantiate(String codec, Class parameterType,
			Object parameter) throws ClassNotFoundException,
			NoSuchMethodException, InstantiationException,
			IllegalAccessException, InvocationTargetException, IOException {

		Class klass = Class.forName(codec);
		Constructor constructor = klass.getConstructor(parameterType);
		return constructor.newInstance(parameter);
	}
}
