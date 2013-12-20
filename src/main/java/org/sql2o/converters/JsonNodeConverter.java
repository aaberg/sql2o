package org.sql2o.converters;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.postgresql.util.PGobject;

public class JsonNodeConverter implements Converter<JsonNode> {

	public JsonNode convert(Object val) throws ConverterException {
		if(val == null) return null;

		if(JsonNode.class.isAssignableFrom(val.getClass())) {
			return (JsonNode)val;
		}

		if(PGobject.class.equals(val.getClass())) {
			String jsonString = ((PGobject)val).getValue();
			ObjectMapper mapper = new ObjectMapper();
			JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead

			try {
				JsonParser parser = factory.createJsonParser(jsonString);
				return mapper.readTree(parser);
			}
			catch(Exception e) {
				throw new ConverterException("Error parsing JSON", e);
			}
		}
		
		if(String.class.equals(val.getClass())) {
			String jsonString = (String)val;
			ObjectMapper mapper = new ObjectMapper();
			JsonFactory factory = mapper.getJsonFactory(); // since 2.1 use mapper.getFactory() instead

			try {
				JsonParser parser = factory.createJsonParser(jsonString);
				return mapper.readTree(parser);
			}
			catch(Exception e) {
				throw new ConverterException("Error parsing JSON", e);
			}
		}

		throw new ConverterException("Unable to convert: " + val.getClass().getCanonicalName() + " to JsonNode");
	}
}
