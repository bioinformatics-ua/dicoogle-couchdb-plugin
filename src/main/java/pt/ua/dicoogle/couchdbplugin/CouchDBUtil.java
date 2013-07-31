/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.codehaus.jackson.JsonNode;
import org.dcm4che2.data.Tag;
import org.ektorp.ViewResult;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;

/**
 *
 * @author Louis
 */
public class CouchDBUtil {

   public static List<SearchResult> getListFromViewResult(ViewResult vResult, URI location, float f) {
        List<ViewResult.Row> listResult = vResult.getRows();
        List<SearchResult> result = new ArrayList<SearchResult>();
        for (ViewResult.Row r : listResult) {
            JsonNode valueNode = r.getValueAsNode();
            HashMap<String, Object> map = getMapFromJsonNode(valueNode);
            URI uri;
            try {
                uri = new URI(location.toString() + map.get(Dictionary.getInstance().tagName(Tag.SOPInstanceUID)));
            } catch (URISyntaxException ex) {
                uri = location;
            }
            result.add(new SearchResult(uri, (float) 0.0, map));
        }
        return result;
    }
    
    private static HashMap<String, Object> getMapFromJsonNode(JsonNode node) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Iterator<Map.Entry<String, JsonNode>> fieldsIt = node.getFields();
        while (fieldsIt.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldsIt.next();
            JsonNode fieldNode = entry.getValue();
            String fieldName = entry.getKey();
            if(fieldNode.getFieldNames().hasNext()){
                map.putAll(getMapFromJsonNode(fieldNode));
                continue;
            }
            map.put(fieldName, fieldNode.getValueAsText());
        }
        return map;
    }

    private static String decodeStringToQuery(String strQuery) {
        String query;
        Object obj, lowObj, highObj;
        String str = "", field = "", lowValue = "", highValue = "";
        int length = strQuery.length(), cmp = 0;
        char currentChar;
        boolean isField = true, isInclusiveBetween = false, isExclusiveBetween = false, isNot = false;
        while (cmp < length) {
            currentChar = strQuery.charAt(cmp);
            cmp++;
            switch (currentChar) {
                case ':':
                    if (isField) {
                        isField = false;
                        field = str;
                        str = "";
                    }
                    if (str.equalsIgnoreCase("Numeric")) {
                        str = "";
                    }
                    break;
                case '[':
                    isInclusiveBetween = true;
                    str = "";
                    break;
                case ']':
                    highValue = str;
                    break;
                case '{':
                    isExclusiveBetween = true;
                    str = "";
                    break;
                case '}':
                    highValue = str;
                    break;
                case ' ':
                    if (str.equalsIgnoreCase("NOT")) {
                        isNot = true;
                        str = "";
                        break;
                    }
                    String temp = strQuery.substring(cmp, cmp + 2);
                    if (temp.equalsIgnoreCase("TO")) {
                        lowValue = str;
                        str = "";
                        cmp += 2;
                    }
                    break;
                default:
                    str += currentChar;
                    break;
            }
        }
        if (isInclusiveBetween || isExclusiveBetween) {
            try {
                lowObj = Double.parseDouble(lowValue);
            } catch (NumberFormatException e) {
                lowObj = lowValue;
            }
            try {
                highObj = Double.parseDouble(highValue);
            } catch (NumberFormatException e) {
                highObj = highValue;
            }
            query = madeQueryIsBetween(field, lowObj, highObj, isInclusiveBetween);
            return query;
        }
        try {
            obj = Double.parseDouble(str);
            query = madeQueryIsValue(field, obj, isNot);
        } catch (NumberFormatException e) {
            obj = str;
            query = madeQueryIsValueRegexInsensitive(field, obj, isNot);
        }
        return query;
    }

    public static String parseStringToQuery(String strQuery) {
        String query = null;
        String str = "";
        char currentChar;
        int cmp = 0, length, nbParOpen = 0, nbBrackets = 0;
        boolean and = false, or = false, isBlank = true;
        if (strQuery == null || strQuery.equalsIgnoreCase("")) {
            return madeQueryFindAll();
        }
        length = strQuery.length();
        for (int i = 0; i < length; i++) {
            currentChar = strQuery.charAt(i);
            if (currentChar != ' ' && currentChar != '*' && currentChar != '"' && currentChar != ':') {
                isBlank = false;
            }
        }
        if (isBlank) {
            return madeQueryFindAll();
        }
        while (cmp != length) {
            currentChar = strQuery.charAt(cmp);
            cmp++;
            switch (currentChar) {
                case '{':
                case '[':
                    str += currentChar;
                    nbBrackets++;
                    break;
                case '}':
                case ']':
                    str += currentChar;
                    nbBrackets--;
                    break;
                case '(':
                    if (nbParOpen != 0) {
                        str += currentChar;
                    }
                    nbParOpen++;
                    break;
                case ')':
                    nbParOpen--;
                    if (nbParOpen == 0) {
                        if (!and && !or) {
                            query = parseStringToQuery(str);
                        }
                        if (and) {
                            query = madeQueryAND(query, parseStringToQuery(str));
                            and = false;
                        }
                        if (or) {
                            query = madeQueryOR(query, parseStringToQuery(str));
                            or = false;
                        }
                        str = "";
                    } else {
                        str += currentChar;
                    }
                    break;
                case ' ':
                    if (str.equalsIgnoreCase("NOT")) {
                        str += currentChar;
                        break;
                    }
                    if (nbBrackets != 0) {
                        str += currentChar;
                        break;
                    }
                    if (nbParOpen != 0) {
                        str += currentChar;
                        break;
                    }
                    if (str.equalsIgnoreCase("AND") || str.equalsIgnoreCase("OR")) {
                        if (str.equalsIgnoreCase("AND")) {
                            and = true;
                        } else {
                            or = true;
                        }
                        str = "";
                    } else {
                        String temp = "";
                        if (cmp + 3 < length) {
                            temp = strQuery.substring(cmp, cmp + 3);
                        }
                        if (temp.equalsIgnoreCase("AND") || temp.equalsIgnoreCase("OR ")) {
                            if (!and && !or) {
                                if (!str.equals("")) {
                                    query = decodeStringToQuery(str);
                                }
                            }
                            if (and) {
                                query = madeQueryAND(query, decodeStringToQuery(str));
                                and = false;
                            }
                            if (or) {
                                query = madeQueryOR(query, decodeStringToQuery(str));
                                or = false;
                            }
                            str = "";
                        }
                    }
                    break;
                default:
                    str += currentChar;
                    break;
            }
        }
        if (!str.equals("")) {
            if (!and && !or) {
                query = decodeStringToQuery(str);
            }
            if (and) {
                query = madeQueryAND(query, decodeStringToQuery(str));
            }
            if (or) {
                query = madeQueryOR(query, decodeStringToQuery(str));
            }
        }
        return "function(doc) { if" + query + " emit(null, doc) }";
    }

    private static String madeQueryFindAll() {
        String query = "function(doc) { emit(null, doc) }";
        return query;
    }

    private static String madeQueryIsValue(String field, Object value, boolean isNot) {
        String query;
        if (isNot) {
            query = "(doc." + field + " != " + value + ")";
        } else {
            query = "(doc." + field + " == " + value + ")";
        }
        return query;
    }

    private static String madeQueryIsValueRegexInsensitive(String field, Object value, boolean isNot) {
        /*BasicDBObject query = new BasicDBObject();
         String str = field;
         String strValue = (String) value;
         if (strValue.endsWith(".*")) {
         strValue = strValue.substring(0, strValue.length() - 1);
         } else if (strValue.endsWith("*")) {
         strValue = strValue.substring(0, strValue.length() - 1);
         }
         if (!isNot) {
         query.put(str, new BasicDBObject("$regex", "^" + strValue + ".*").append("$options", "i"));
         } else {
         query.put(str, new BasicDBObject("$ne", strValue));
         }
         return query;*/
        String query;
        if (isNot) {
            query = "(doc." + field + " != '" + value + "')";
        } else {
            query = "(doc." + field + " == '" + value + "')";
        }
        return query;
    }

    private static String madeQueryIsBetween(String field, Object lowValue, Object highValue, boolean isInclusive) {
        String query;
        if (isInclusive) {
            query = "(doc." + field + " >= " + lowValue + " && doc." + field + " <= " + highValue + ")";
        } else {
            query = "(doc." + field + " > " + lowValue + " && doc." + field + " < " + highValue + ")";
        }
        return query;
    }

    private static String madeQueryAND(String q1, String q2) {
        String query = "(" + q1 + " && " + q2 + ")";
        return query;
    }

    private static String madeQueryOR(String q1, String q2) {
        String query = "(" + q1 + " || " + q2 + ")";
        return query;
    }
}
