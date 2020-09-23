package org.veupathdb.service.access.service;

import java.sql.*;

import io.vulpine.lib.jcfi.CheckedBiFunction;
import io.vulpine.lib.jcfi.CheckedFunction;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;

public class QueryUtil
{
  @SuppressWarnings("FieldMayBeFinal")
  private static QueryUtil instance = new QueryUtil();

  private final Logger log = LogProvider.logger(QueryUtil.class);

  public Connection getAcctDbConnection() throws Exception {
    log.trace("Util#getAcctDbConnection()");

    return DbManager.accountDatabase().getDataSource().getConnection();
  }

  public Connection getAppDbConnection() throws Exception {
    log.trace("Util#getAppDbConnection()");

    return DbManager.applicationDatabase().getDataSource().getConnection();
  }

  public ResultSet runQueryLogged(final Statement s, final String q) throws Exception {
    log.trace("Util#runQueryLogged(Statement, String)");

    return exec(s, q, PreparedStatement::executeQuery, Statement::executeQuery);
  }

  public boolean runLogged(final Statement s, final String q) throws Exception {
    log.trace("Util#executeLogged(Statement, String)");

    return exec(s, q, PreparedStatement::execute, Statement::execute);
  }

  public PreparedStatement prepareSqlStatement(final Connection c, final String sql)
  throws Exception {
    log.trace("Util#prepareSqlStatement(Connection, String)");

    try {
      return c.prepareStatement(sql);
    } catch (Exception e) {
      throw new Exception("Failed to prepare query:\n" + sql, e);
    }
  }

  public PreparedStatement prepareSqlStatement(
    final Connection con,
    final String sql,
    final String[] returning
  ) throws Exception {
    log.trace("Util#prepareSqlStatement(con, sql, returning)");

    try {
      return con.prepareStatement(sql, returning);
    } catch (Exception e) {
      throw new Exception("Failed to prepare query:\n" + sql, e);
    }
  }

  public < T > T exec(
    final Statement ps,
    final String sql,
    final CheckedFunction< PreparedStatement, T > fn1,
    final CheckedBiFunction< Statement, String, T > fn2
  ) throws Exception {
    log.trace("Util#exec(Statement, String, CheckedFunction, CheckedBiFunction)");

    try {
      return ps instanceof PreparedStatement
        ? fn1.apply((PreparedStatement) ps)
        : fn2.apply(ps, sql);
    } catch (Exception e) {
      throw new Exception("Query failed:\n" + sql, e);
    }
  }



  public static QueryUtil getInstance() {
    return instance;
  }

  public static Connection acctDbConnection() throws Exception {
    return getInstance().getAcctDbConnection();
  }

  public static Connection appDbConnection() throws Exception {
    return getInstance().getAppDbConnection();
  }

  public static ResultSet executeQueryLogged(final Statement s, final String q) throws Exception {
    return getInstance().runQueryLogged(s, q);
  }

  public static boolean executeLogged(final Statement ps, final String sql) throws Exception {
    return getInstance().runLogged(ps, sql);
  }

  public static PreparedStatement prepareStatement(final Connection con, final String sql)
  throws Exception {
    return getInstance().prepareSqlStatement(con, sql);
  }

  public static PreparedStatement prepareStatement(
    final Connection con,
    final String sql,
    final String[] returning
  ) throws Exception {
    return getInstance().prepareSqlStatement(con, sql, returning);
  }
}
