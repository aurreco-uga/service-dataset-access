package org.veupathdb.service.access.repo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.veupathdb.service.access.model.PartialProviderRow;
import org.veupathdb.service.access.model.ProviderRow;

public final class ProviderRepo
{
  public interface Delete
  {
    static void byId(int providerId) throws Exception {
      final var sql = SQL.Delete.Providers.ById;
      try (
        var con = Util.getAcctDbConnection();
        var ps = Util.prepareStatement(con, sql)
      ) {
        ps.setInt(1, providerId);
        Util.executeLogged(ps, sql);
      }
    }
  }

  public interface Insert
  {
    static int newProvider(PartialProviderRow row) throws Exception {
      final var sql = SQL.Insert.Providers;
      final var ret = new String[]{DB.Column.Provider.ProviderId};
      try (
        final var cn = Util.getAcctDbConnection();
        final var ps = Util.prepareStatement(cn, sql, ret)
      ) {
        ps.setLong(1, row.getUserId());
        ps.setBoolean(2, row.isManager());
        ps.setString(3, row.getDatasetId());

        try (final var rs = Util.executeQueryLogged(ps, sql)) {
          rs.next();
          return rs.getInt(1);
        }
      }
    }
  }

  public interface Select
  {
    static List < ProviderRow > byDataset(
      final String datasetId,
      final int limit,
      final int offset
    ) throws Exception {
      final var sql = SQL.Select.Providers.ByDataset;
      try (
        var con = Util.getAcctDbConnection();
        var ps = Util.prepareStatement(con, sql)
      ) {
        ps.setString(1, datasetId);
        ps.setInt(2, offset);
        ps.setInt(3, limit);

        try (var rs = Util.executeQueryLogged(ps, sql)) {
          if (!rs.next())
            return Collections.emptyList();

          final var out = new ArrayList < ProviderRow >(10);

          do {
            final var row = new ProviderRow();
            row.setProviderId(rs.getInt(DB.Column.Provider.ProviderId));
            row.setUserId(rs.getInt(DB.Column.Provider.UserId));
            row.setDatasetId(datasetId);
            row.setManager(rs.getBoolean(DB.Column.Provider.IsManager));
            UserQuery.parseUser(row, rs);

            out.add(row);
          } while (rs.next());

          return out;
        }
      }
    }

    static Optional < ProviderRow > byId(final int providerId)
    throws Exception {
      final var sql = SQL.Select.Providers.ById;
      try (
        var con = Util.getAcctDbConnection();
        var ps = Util.prepareStatement(con, sql)
      ) {
        ps.setInt(1, providerId);

        try (var rs = Util.executeQueryLogged(ps, sql)) {
          if (!rs.next())
            return Optional.empty();

          final var out = new ProviderRow();
          out.setProviderId(providerId);
          out.setUserId(rs.getInt(DB.Column.Provider.UserId));
          out.setDatasetId(rs.getString(DB.Column.Provider.DatasetId));
          out.setManager(rs.getBoolean(DB.Column.Provider.IsManager));
          UserQuery.parseUser(out, rs);

          return Optional.of(out);
        }
      }
    }

    static Optional < ProviderRow > byUserAndDataset(
      final long userId,
      final String datasetId
    )
    throws Exception {
      final var sql = SQL.Select.Providers.ByUserDataset;
      try (
        var con = Util.getAcctDbConnection();
        var ps = Util.prepareStatement(con, sql)
      ) {
        ps.setLong(1, userId);
        ps.setString(2, datasetId);

        try (var rs = Util.executeQueryLogged(ps, sql)) {
          if (!rs.next())
            return Optional.empty();

          final var out = new ProviderRow();
          out.setProviderId(rs.getInt(DB.Column.Provider.ProviderId));
          out.setUserId(userId);
          out.setDatasetId(datasetId);
          out.setManager(rs.getBoolean(DB.Column.Provider.IsManager));
          UserQuery.parseUser(out, rs);

          return Optional.of(out);
        }
      }
    }

    static List < ProviderRow > byUserId(long userId) throws Exception {
      final var sql = SQL.Select.Providers.ByUserId;
      try (
        var con = Util.getAcctDbConnection();
        var ps = Util.prepareStatement(con, sql)
      ) {
        ps.setLong(1, userId);

        try (var rs = Util.executeQueryLogged(ps, sql)) {
          final var out = new ArrayList < ProviderRow >();

          while (rs.next()) {
            final var row = new ProviderRow();
            row.setProviderId(rs.getInt(DB.Column.Provider.ProviderId));
            row.setUserId(userId);
            row.setDatasetId(rs.getString(DB.Column.Provider.DatasetId));
            row.setManager(rs.getBoolean(DB.Column.Provider.IsManager));
            UserQuery.parseUser(row, rs);
            out.add(row);
          }

          return out;
        }
      }
    }

    static int countByDataset(String datasetId) throws Exception {
      final var sql = SQL.Select.Providers.CountByDataset;
      try (
        var con = Util.getAcctDbConnection();
        var ps = Util.prepareStatement(con, sql)
      ) {
        ps.setString(1, datasetId);
        try (var rs = Util.executeQueryLogged(ps, sql)) {
          rs.next();
          return rs.getInt(1);
        }
      }
    }
  } // End::Select

  public interface Update
  {
    static void isManagerById(ProviderRow row) throws Exception {
      final var sql = SQL.Update.Providers.ById;
      try (
        var con = Util.getAcctDbConnection();
        var ps = Util.prepareStatement(con, sql)
      ) {
        ps.setBoolean(1, row.isManager());
        ps.setInt(2, row.getProviderId());
        Util.executeLogged(ps, sql);
      }
    }
  }
}
