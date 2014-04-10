package org.sql2o.tools;

import java.util.*;

/**
 * Created by lars on 10.04.14.
 */
public class DefaultNamedParameterHandlerFactory implements NamedParameterHandlerFactory {
    public NamedParameterHandler newParameterHandler() {
        return new NamedParameterHandler() {

            private final Map paramMap = new HashMap();

            public int[] getParameterIndices(String name) {
                return (int[])paramMap.get(name);
            }

            public boolean containsParameter(String name) {
                return paramMap.containsKey(name);
            }

            public String parseStatement(String statement) {
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

                            List indexList=(List)paramMap.get(name);
                            if(indexList==null) {
                                indexList=new LinkedList();
                                paramMap.put(name, indexList);
                            }
                            indexList.add(Integer.valueOf(index));

                            index++;
                        }
                    }
                    parsedQuery.append(c);
                }

                // replace the lists of Integer objects with arrays of ints
                for(Iterator itr=paramMap.entrySet().iterator(); itr.hasNext();) {
                    Map.Entry entry=(Map.Entry)itr.next();
                    List list=(List)entry.getValue();
                    int[] indexes=new int[list.size()];
                    int i=0;
                    for(Iterator itr2=list.iterator(); itr2.hasNext();) {
                        Integer x=(Integer)itr2.next();
                        indexes[i++]=x.intValue();
                    }
                    entry.setValue(indexes);
                }

                return parsedQuery.toString();
            }
        };
    }
}
