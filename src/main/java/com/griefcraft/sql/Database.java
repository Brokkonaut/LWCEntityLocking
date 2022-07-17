/*
 * Copyright 2011 Tyler Blair. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package com.griefcraft.sql;

import com.griefcraft.lwc.LWC;
import com.griefcraft.scripting.ModuleException;
import com.griefcraft.util.Statistics;
import com.griefcraft.util.config.Configuration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

public abstract class Database {

    public enum Type {
        MySQL, //
        SQLite, //
        NONE; //

        /**
         * Match the given string to a database type
         *
         * @param str
         * @return
         */
        public static Type matchType(String str) {
            for (Type type : values()) {
                if (type.toString().equalsIgnoreCase(str)) {
                    return type;
                }
            }

            return null;
        }

    }

    public static interface SQLRunnable {
        public void run() throws SQLException;
    }

    public static interface SQLCallable<T> {
        public T call() throws SQLException;
    }

    /**
     * The database engine being used for this connection
     */
    public Type currentType;

    /**
     * Store cached prepared statements.
     * <p/>
     * Since SQLite JDBC doesn't cache them.. we do it ourselves :S
     */
    private Map<String, PreparedStatement> statementCache = new HashMap<>();

    /**
     * The connection to the database
     */
    protected Connection connection = null;

    /**
     * The default database engine being used. This is set via config
     *
     * @default SQLite
     */
    public static Type DefaultType = Type.NONE;

    /**
     * If we are connected to sqlite
     */
    private boolean connected = false;

    /**
     * If the database has been loaded
     */
    protected boolean loaded = false;

    /**
     * The database prefix (only if we're using MySQL.)
     */
    protected String prefix = "";

    /**
     * If the high level statement cache should be used. If this is false, already cached statements are ignored
     */
    private boolean useStatementCache = true;

    public Database() {
        currentType = DefaultType;

        prefix = LWC.getInstance().getConfiguration().getString("database.prefix", "");
        if (prefix == null) {
            prefix = "";
        }
    }

    public Database(Type currentType) {
        this();
        this.currentType = currentType;
    }

    /**
     * Ping the database to keep the connection alive
     */
    public void pingDatabase() {
        runAndIgnoreException(() -> {
            Statement stmt = connection.createStatement();
            stmt.executeQuery("SELECT 1;");
            stmt.close();
        });
    }

    /**
     * @return the table prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Print an exception to stdout
     *
     * @param exception
     */
    protected void printException(Exception exception) {
        throw new ModuleException(exception);
    }

    /**
     * Connect to MySQL
     *
     * @return if the connection was succesful
     */
    public boolean connect() throws SQLException {
        if (connection != null) {
            return true;
        }

        if (currentType == null || currentType == Type.NONE) {
            log("Invalid database engine");
            return false;
        }

        // Load the driver class
        try {
            if (currentType == Type.MySQL) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else {
                Class.forName("org.sqlite.JDBC");
            }
        } catch (ClassNotFoundException e) {
            LWC.getInstance().getPlugin().getLogger().log(Level.SEVERE, "Could not load the database driver!", e);
        }

        // Create the properties to pass to the driver
        Properties properties = new Properties();

        // if we're using mysql, append the database info
        if (currentType == Type.MySQL) {
            LWC lwc = LWC.getInstance();
            // properties.put("autoReconnect", "true");
            properties.put("user", lwc.getConfiguration().getString("database.username"));
            properties.put("password", lwc.getConfiguration().getString("database.password"));
        }

        statementCache.clear();
        try {
            // Connect to the database
            connection = DriverManager.getConnection("jdbc:" + currentType.toString().toLowerCase() + ":" + getDatabasePath(), properties);
            connection.setAutoCommit(false);
            connected = true;
            return true;
        } catch (SQLException e) {
            log("Failed to connect to " + currentType + ": " + e.getErrorCode() + " - " + e.getMessage());

            if (e.getCause() != null) {
                log("Connection failure cause: " + e.getCause().getMessage());
            }
            throw e;
        }
    }

    public void dispose() {
        statementCache.clear();

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        connection = null;
    }

    protected <T> T runAndIgnoreException(SQLCallable<T> callable) {
        try {
            return run(callable);
        } catch (SQLException ignored) {
        }
        return null;
    }

    protected <T> T runAndLogException(SQLCallable<T> callable) {
        try {
            return run(callable);
        } catch (SQLException e) {
            LWC.getInstance().getPlugin().getLogger().log(Level.SEVERE, "Database Exception", e);
        }
        return null;
    }

    protected <T> T runAndThrowModuleExceptionIfFailing(SQLCallable<T> callable) {
        try {
            return run(callable);
        } catch (SQLException e) {
            throw new ModuleException(e);
        }
    }

    protected synchronized <T> T run(SQLCallable<T> callable) throws SQLException {
        int fails = 0;
        while (true) {
            try {
                if (connection == null || connection.isClosed()) {
                    connect();
                }
                T rv = callable.call();
                connection.commit();
                return rv;
            } catch (SQLException e) {
                fails += 1;
                if (connection != null) {
                    try {
                        if (!connection.isClosed()) {
                            connection.rollback();
                        }
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
                dispose();
                if (fails >= 3) {
                    throw e;
                }
            }
        }
    }

    protected void runAndIgnoreException(SQLRunnable runnable) {
        try {
            run(runnable);
        } catch (SQLException ignored) {
        }
    }

    protected void runAndLogException(SQLRunnable runnable) {
        try {
            run(runnable);
        } catch (SQLException e) {
            LWC.getInstance().getPlugin().getLogger().log(Level.SEVERE, "Database Exception", e);
        }
    }

    protected void runAndThrowModuleExceptionIfFailing(SQLRunnable runnable) {
        try {
            run(runnable);
        } catch (SQLException e) {
            throw new ModuleException(e);
        }
    }

    protected synchronized void run(SQLRunnable runnable) throws SQLException {
        int fails = 0;
        while (true) {
            try {
                if (connection == null || connection.isClosed()) {
                    connect();
                }
                runnable.run();
                connection.commit();
                return;
            } catch (SQLException e) {
                fails += 1;
                if (connection != null) {
                    try {
                        if (!connection.isClosed()) {
                            connection.rollback();
                        }
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
                dispose();
                if (fails >= 3) {
                    throw e;
                }
            }
        }
    }

    /**
     * @return the connection to the database
     */
    protected Connection getConnection() {
        return connection;
    }

    /**
     * @return the path where the database file should be saved
     */
    protected String getDatabasePath() {
        Configuration lwcConfiguration = LWC.getInstance().getConfiguration();

        if (currentType == Type.MySQL) {
            return "//" + lwcConfiguration.getString("database.host") + "/" + lwcConfiguration.getString("database.database");
        }

        return lwcConfiguration.getString("database.path");
    }

    /**
     * @return the database engine type
     */
    public Type getType() {
        return currentType;
    }

    /**
     * Load the database
     */
    public abstract void load();

    /**
     * Log a string to stdout
     *
     * @param str
     *            The string to log
     */
    protected void log(String str) {
        LWC.getInstance().log(str);
    }

    /**
     * Prepare a statement unless it's already cached (and if so, just return it)
     *
     * @param sql
     * @return
     */
    protected PreparedStatement prepare(String sql) throws SQLException {
        return prepare(sql, false);
    }

    /**
     * Prepare a statement unless it's already cached (and if so, just return it)
     *
     * @param sql
     * @param returnGeneratedKeys
     * @return
     */
    protected PreparedStatement prepare(String sql, boolean returnGeneratedKeys) throws SQLException {
        if (connection == null) {
            return null;
        }

        if (useStatementCache && statementCache.containsKey(sql)) {
            Statistics.addQuery();
            return statementCache.get(sql);
        }

            PreparedStatement preparedStatement;

            if (returnGeneratedKeys) {
                preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            } else {
                preparedStatement = connection.prepareStatement(sql);
            }

            statementCache.put(sql, preparedStatement);
            Statistics.addQuery();

            return preparedStatement;
    }

    /**
     * Add a column to a table
     *
     * @param table
     * @param column
     */
    protected boolean addColumn(String table, String column, String type) {
        return executeUpdateNoException("ALTER TABLE " + table + " ADD " + column + " " + type);
    }

    /**
     * Add a column to a table
     *
     * @param table
     * @param column
     */
    protected boolean dropColumn(String table, String column) {
        return executeUpdateNoException("ALTER TABLE " + table + " DROP COLUMN " + column);
    }

    /**
     * Rename a table
     *
     * @param table
     * @param newName
     */
    protected boolean renameTable(String table, String newName) {
        return executeUpdateNoException("ALTER TABLE " + table + " RENAME TO " + newName);
    }

    /**
     * Drop a table
     *
     * @param table
     */
    protected boolean dropTable(String table) {
        return executeUpdateNoException("DROP TABLE " + table);
    }

    /**
     * Execute an update, ignoring any exceptions
     *
     * @param query
     * @return true if an exception was thrown
     */
    protected boolean executeUpdateNoException(String query) {
        Statement statement = null;
        boolean exception = false;

        try {
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            exception = true;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
            }
        }

        return exception;
    }

    /**
     * @return true if connected to the database
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns true if the high level statement cache should be used. If this is false, already cached statements are ignored
     *
     * @return
     */
    public boolean useStatementCache() {
        return useStatementCache;
    }

    /**
     * Set if the high level statement cache should be used.
     *
     * @param useStatementCache
     * @return
     */
    public void setUseStatementCache(boolean useStatementCache) {
        this.useStatementCache = useStatementCache;
    }

}
