package org.sql2o.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by lars on 11.04.14.
 */
public class DefaultSqlParameterParsingStrategy implements SqlParameterParsingStrategy {

    @SuppressWarnings("ConstantConditions")
    public String parseSql(String statement, Map<String, List<Integer>> paramMap) {
        int length=statement.length();
        StringBuilder parsedQuery=new StringBuilder(length);
        boolean inSingleQuote=false;
        boolean inDoubleQuote=false;
        int index=1;

        char c=' ';
        for(int i=0;i<length;i++) {
            char previousChar=c;
            c = statement.charAt(i);
            if(inSingleQuote) {
                if(c=='\'') {
                    inSingleQuote=false;
                }
            } else if(inDoubleQuote) {
                if(c=='"') {
                    inDoubleQuote=false;
                }
            } else {
                if(c=='\'') {
                    inSingleQuote=true;
                } else if(c=='"') {
                    inDoubleQuote=true;
                } else if(previousChar!=':' && c==':' && i+1<length &&
                        Character.isJavaIdentifierStart(statement.charAt(i+1))) {
                    int j=i+2;
                    while(j<length && Character.isJavaIdentifierPart(statement.charAt(j))) {
                        j++;
                    }
                    String name=statement.substring(i+1,j);
                    c='?'; // replace the parameter with a question mark
                    i+=name.length(); // skip past the end if the parameter

                    List<Integer> indexList=paramMap.get(name);
                    if(indexList==null) {
                        indexList=new ArrayList<Integer>(3);
                        paramMap.put(name, indexList);
                    }
                    indexList.add(index);

                    index++;
                }
            }
            parsedQuery.append(c);
        }

        return parsedQuery.toString();
    }
}
