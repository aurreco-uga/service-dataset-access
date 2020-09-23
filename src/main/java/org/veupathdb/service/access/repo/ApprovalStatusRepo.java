package org.veupathdb.service.access.repo;

import org.veupathdb.service.access.model.ApprovalStatus;
import org.veupathdb.service.access.model.ApprovalStatusCache;
import org.veupathdb.service.access.service.QueryUtil;

public final class ApprovalStatusRepo
{
  public interface Select
  {
    static void populateApprovalStatusCache() throws Exception {
      try (
        final var cn = QueryUtil.acctDbConnection();
        final var st = cn.createStatement();
        final var rs = QueryUtil.executeQueryLogged(st, SQL.Select.Enums.ApprovalStatus)
      ) {
        final var cache = ApprovalStatusCache.getInstance();

        while (rs.next()) {
          cache.put(rs.getShort(1), ApprovalStatus.valueOf(rs.getString(2)
            .toUpperCase()));
        }
      }
    }
  }
}
