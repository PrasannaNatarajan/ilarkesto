package ilarkesto.core.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonObject {

	private Map<String, Object> elements = new LinkedHashMap<String, Object>();
	private int idx = -1;

	public JsonObject() {}

	private JsonObject(String json, int offset) {
		parse(json, offset);
	}

	public JsonObject(String json) {
		this(json, 0);
	}

	public JsonObject(Map<?, ?> map) {
		for (Map.Entry entry : map.entrySet()) {
			String name = entry.getKey().toString();
			Object value = Json.convertValue(entry.getValue());
			put(name, value);
		}
	}

	// --- inspecting ---

	public Object get(String name) {
		return elements.get(name);
	}

	public String getString(String name) {
		return (String) get(name);
	}

	public JsonObject getObject(String name) {
		return (JsonObject) get(name);
	}

	public List getArray(String name) {
		return (List) get(name);
	}

	public List getNumber(String name) {
		return (List) get(name);
	}

	public Integer getInteger(String name) {
		return (Integer) get(name);
	}

	public Long getLong(String name) {
		return (Long) get(name);
	}

	public List<String> getArrayOfStrings(String name) {
		return (List<String>) get(name);
	}

	public List<JsonObject> getArrayOfObjects(String name) {
		return (List<JsonObject>) get(name);
	}

	// --- manipulating ---

	public <V> V put(String name, V value) {
		if (name == null || name.length() == 0) throw new RuntimeException("name required");
		elements.put(name, Json.convertValue(value));
		return value;
	}

	// --- formating ---

	public String toFormatedString(int indentation) {
		StringBuilder sb = new StringBuilder();
		return sb.toString();
	}

	public String toFormatedString() {
		return toFormatedString(0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		for (Map.Entry<String, Object> element : elements.entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
			}
			sb.append('"').append(Json.escapeString(element.getKey())).append("\":");
			sb.append(Json.valueToString(element.getValue()));
		}
		sb.append('}');
		return sb.toString();
	}

	// --- parsing ---

	int parse(String json, int offset) {
		idx = offset;
		parseWhitespace(json, "'{'");
		if (json.charAt(idx) != '{') throw new ParseException("Expecting '{'", json, idx);
		idx++;
		parseWhitespace(json, "elements or '}'");
		boolean first = true;
		while (json.charAt(idx) != '}') {
			if (first) {
				first = false;
			} else {
				if (json.charAt(idx) == ',') {
					idx++;
				} else {
					throw new ParseException("Expecting ','", json, idx);
				}
			}
			parseElement(json);
			parseWhitespace(json, "',' or '}'");
		}
		idx++;
		return idx;
	}

	private void parseWhitespace(String json, String expectation) {
		idx = Json.getFirstNonWhitespaceIndex(json, idx);
		if (idx < 0) throw new ParseException("Expecting " + expectation, json, idx);
	}

	private void parseElement(String json) {
		if (json.charAt(idx) != '"') throw new ParseException("Expecting '\"'", json, idx);
		idx++;
		int nameEndIdx = Json.getFirstQuoting(json, idx);
		if (nameEndIdx < 0) throw new ParseException("Unclosed element name", json, idx);
		String name = json.substring(idx, nameEndIdx);
		idx = nameEndIdx + 1;
		parseWhitespace(json, "':'");
		if (json.charAt(idx) != ':') throw new ParseException("Expecting ':'", json, idx);
		idx++;
		parseWhitespace(json, "element value");
		Object value = parseValue(json);
		put(name, value);
	}

	private Object parseValue(String json) {
		if (json.startsWith("null", idx)) {
			idx += 4;
			return null;
		} else if (json.startsWith("true", idx)) {
			idx += 4;
			return true;
		} else if (json.startsWith("false", idx)) {
			idx += 5;
			return false;
		} else if (json.charAt(idx) == '"') {
			idx++;
			int valueEndIdx = Json.getFirstQuoting(json, idx);
			if (valueEndIdx < 0) throw new ParseException("Unclosed element string value", json, idx);
			String value = json.substring(idx, valueEndIdx);
			idx = valueEndIdx + 1;
			return value;
		} else if (json.charAt(idx) == '{') {
			JsonObject value = new JsonObject(json, idx);
			idx = value.idx;
			return value;
		} else if (json.charAt(idx) == '[') {
			List list = new ArrayList();
			idx++;
			while (true) {
				parseWhitespace(json, "array");
				if (json.charAt(idx) == ']') break;
				Object value = parseValue(json);
				list.add(value);
				parseWhitespace(json, "array");
				if (json.charAt(idx) == ']') break;
				if (json.charAt(idx) != ',') throw new ParseException("Expectint array separator ','", json, idx);
				idx++;
			}
			idx++;
			return list;
		} else {
			int len = json.length();
			int endIdx = idx;
			while (endIdx < len) {
				endIdx++;
				char endCh = json.charAt(endIdx);
				if (endCh == ',' || endCh == '}' || endCh == ']' || Json.isWhitespace(endCh)) {
					break;
				}
			}
			String number = json.substring(idx, endIdx);
			idx = endIdx;
			return Json.parseNumber(number);
		}
	}

}