package org.sql2o;

/**
 * Created by zsoltjanos on 01/08/15.
 */
public class UserInserterFactory {

    public static UserInserter buildUserInserter(boolean useBind){
        if (useBind){
            return new BindUserInserter();
        } else {
            return new PlainUserInserter();
        }
    }

    private static class BindUserInserter implements UserInserter {

        @Override
        public void insertUser(Query insertQuery, int idx) {
            User user = new User();
            user.name = "a name " + idx;
            user.setEmail(String.format("test%s@email.com", idx));
            user.text = "some text";
            insertQuery.bind(user).addToBatch();
        }
    }

    private static class PlainUserInserter implements UserInserter {

        @Override
        public void insertUser(Query insertQuery, int idx) {
            insertQuery.addParameter("name", "a name " + idx)
                    .addParameter("email", String.format("test%s@email.com", idx))
                    .addParameter("text", "some text").addToBatch();
        }
    }
}
