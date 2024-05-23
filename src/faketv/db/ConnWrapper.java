package faketv.db;

import java.sql.Connection;

    public class ConnWrapper  {
        Connection con;
        long timeout;
        ConnWrapper(Connection con) {
            this.con = con;
            timeout = System.currentTimeMillis();
        }
    }