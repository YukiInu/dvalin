package de.taimos.dvalin.interconnect.model;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;

/**
 * Helper class to deserialize maps with DateTime keys
 */
public class DateTimeDeserializerWithTZ extends StdScalarDeserializer<DateTime> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;


	/**
	 * Default constructor
	 */
	public DateTimeDeserializerWithTZ() {
		super(DateTime.class);
	}

	@Override
	public DateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		JsonToken t = jp.getCurrentToken();
		if (t == JsonToken.VALUE_NUMBER_INT) {
			return new DateTime(jp.getLongValue(), DateTimeZone.forTimeZone(ctxt.getTimeZone()));
		}
		if (t == JsonToken.VALUE_STRING) {
			String str = jp.getText().trim();
			if (str.length() == 0) { // [JACKSON-360]
				return null;
			}
			// catch serialized time zones
			if ((str.charAt(str.length() - 6) == '+') || (str.charAt(str.length() - 1) == 'Z') || (str.charAt(str.length() - 6) == '-')) {
				return new DateTime(str);
			}
			return new DateTime(str, DateTimeZone.forTimeZone(ctxt.getTimeZone()));
		}
		throw ctxt.mappingException(this.handledType());
	}

	@Override
	public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException, JsonProcessingException {
		return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
	}
}
