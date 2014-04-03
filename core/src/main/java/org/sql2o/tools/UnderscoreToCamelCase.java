package org.sql2o.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Takes a string formatted like: 'my_string_variable' and returns it as: 'myStringVariable'
 * 
 * @author ryancarlson
 */
public class UnderscoreToCamelCase {

    private final static Map<String, String> cache = new HashMap<String, String>();

	public static String convert(String underscore) {
		if(underscore == null || underscore.trim().length() == 0) return underscore;

        if (FeatureDetector.isCacheUnderscoreToCamelcaseEnabled() && cache.containsKey(underscore))
            return cache.get(underscore);

		String[] stringTokens = underscore.split("_");
		
		StringBuilder camelCase = new StringBuilder();
		
		for(int i = 0; i < stringTokens.length; i++) {
			if(i == 0) {
				/*the first word, which precedes the first underscore will be all lowercase*/
				camelCase.append(stringTokens[i].toLowerCase());
			}
			else {
				/*all subsequent words will have the first character in upper case, and the rest of the word in lower case*/
				camelCase.append(Character.toUpperCase(stringTokens[i].charAt(0))).append(stringTokens[i].substring(1,stringTokens[i].length()).toLowerCase());
			}
		}

        String res = camelCase.toString();
        if (FeatureDetector.isCacheUnderscoreToCamelcaseEnabled()) cache.put(underscore, res);
		
		return res;
	}
}
