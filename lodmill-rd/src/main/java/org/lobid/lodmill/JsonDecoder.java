/* Copyright 2014 hbz, Pascal Christoph.
 * Licensed under the Eclipse Public License 1.0 */
package org.lobid.lodmill;

import java.io.IOException;
import java.io.Reader;

import org.culturegraph.mf.exceptions.MetafactureException;
import org.culturegraph.mf.framework.DefaultObjectPipe;
import org.culturegraph.mf.framework.StreamReceiver;
import org.culturegraph.mf.framework.annotations.Description;
import org.culturegraph.mf.framework.annotations.In;
import org.culturegraph.mf.framework.annotations.Out;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
 * Decodes json
 * 
 * @author Pascal Christoph (dr0i)
 * 
 */
@Description("Decodes a json record as literals (as key-value pairs).")
@In(Reader.class)
@Out(StreamReceiver.class)
public final class JsonDecoder extends
		DefaultObjectPipe<Reader, StreamReceiver> {
	private JsonParser jsonParser;
	private boolean arrayOfObjects;

	/**
	 * Sets the JSON parser to await or not await an array of objects.
	 * 
	 * @param arrayOfObjects set "true" if JSON is an array of objects. Default
	 *          ist "false".
	 */
	public void setArrayOfObjects(final String arrayOfObjects) {
		this.arrayOfObjects = Boolean.parseBoolean(arrayOfObjects);
	}

	@Override
	public void process(final Reader reader) {
		try {
			jsonParser = new JsonFactory().createJsonParser(reader);
			System.out.println("Start alles ");
			// Skip START_OBJECT
			JsonToken jsonToken = jsonParser.nextToken();
			while (JsonToken.START_OBJECT != jsonToken) {
				System.out.println("Suche Startobject " + jsonParser.getCurrentName());
				jsonParser.nextToken();
			}
			if (arrayOfObjects) {
				while (jsonParser.nextToken() != JsonToken.START_ARRAY) {
					System.out.println("Suche StartObjectArray "
							+ jsonParser.getCurrentName());
				}
			}

			while (jsonToken != null) {
				if (JsonToken.START_OBJECT == jsonToken) {
					System.out.println("############################  Start Object !");
					getReceiver().startRecord("");
					while (JsonToken.END_OBJECT != jsonParser.nextToken()) {
						// jsonParser.nextToken();
						if (jsonParser.getCurrentName() == null) {
							break;
						}
						String key = jsonParser.getCurrentName();
						System.out.print("Key=" + key);
						jsonParser.nextToken();
						String value = jsonParser.getText();
						System.out.println(" ,value=" + value);
						getReceiver().literal(key, value);
					}
					System.out.println("############################ End Object");
					getReceiver().endRecord();
				}
				jsonToken = jsonParser.nextToken();
			}
		} catch (final IOException e) {
			throw new MetafactureException(e);
		}
	}

}
