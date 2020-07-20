package org.veupathdb.service.access.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.Request;

import org.gusdb.fgputil.accountdb.UserProfile;
import org.veupathdb.lib.container.jaxrs.errors.UnprocessableEntityException;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.service.access.generated.model.*;
import org.veupathdb.service.access.model.PartialProviderRow;
import org.veupathdb.service.access.model.ProviderRow;
import org.veupathdb.service.access.repo.AccountRepo;
import org.veupathdb.service.access.repo.ProviderRepo;
import org.veupathdb.service.access.util.Keys;

public class ProviderService
{
  /**
   * Creates a new Provider record from the given request body.
   *
   * @return the ID of the newly created record.
   */
  public static int createProvider(DatasetProviderCreateRequest body) {
    final var row = new PartialProviderRow();
    row.setDatasetId(body.getDatasetId());
    row.setManager(body.getIsManager());
    row.setUserId(body.getUserId());

    try {
      return ProviderRepo.Insert.newProvider(row);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Return a client friendly listing of all providers attached to a given
   * <code>datasetId</code>.
   * <p>
   * <b>NOTE</b>: This method does not verify that the data returned should be
   * available for any particular user.  Verify the user has permission to view
   * this data separately.
   */
  public static DatasetProviderList getProviderList(
    final String datasetId,
    final int limit,
    final int offset
  ) {
    if (datasetId == null || datasetId.isBlank())
      throw new BadRequestException("datasetId query param is required");

    try {
      final var total = ProviderRepo.Select.countByDataset(datasetId);
      final var rows  = ProviderRepo.Select.byDataset(datasetId, limit, offset);
      return list2Providers(rows, offset, total);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Locates a provider with the given <code>providerId</code> or throws a 404
   * exception.
   */
  public static ProviderRow requireProviderById(int providerId) {
    try {
      return ProviderRepo.Select.byId(providerId)
        .orElseThrow(NotFoundException::new);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Locates a provider with the given <code>userId</code> or throws a 404
   * exception.
   */
  public static List < ProviderRow > requireProviderByUserId(
    final long userId
  ) {
    try {
      final var out = ProviderRepo.Select.byUserId(userId);

      if (out.isEmpty())
        throw new NotFoundException();

      return out;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static boolean userIsManager(
    final Request req,
    final String datasetId
  ) {
    return userIsManager(UserProvider.lookupUser(req)
      .map(UserProfile::getUserId)
      .orElseThrow(InternalServerErrorException::new), datasetId);
  }

  /**
   * Looks up the user provider record and returns whether or not the provider
   * with the given <code>userId</code> is marked as a manager.
   * <p>
   * If provider exists with the given <code>userId</code>, a 404 exception
   * will be thrown via {@link #requireProviderByUserId(long)}.
   */
  public static boolean userIsManager(
    final long userId,
    final String datasetId
  ) {
    final var ds = datasetId.toUpperCase();
    return requireProviderByUserId(userId)
      .stream()
      .filter(row -> ds.equals(row.getDatasetId().toUpperCase()))
      .anyMatch(PartialProviderRow::isManager);
  }

  public static void deleteProvider(int providerId) {
    try {
      ProviderRepo.Delete.byId(providerId);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static void updateProvider(ProviderRow row) {
    try {
      ProviderRepo.Update.isManagerById(row);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public static void validateCreate(final DatasetProviderCreateRequest body) {
    final var out = new HashMap<String, List<String>>();

    if (body.getDatasetId() == null || body.getDatasetId().isBlank())
      out.put(Keys.Json.KEY_DATASET_ID, new ArrayList <>(){{
        add("Dataset ID is required");
      }});

    try {
      if (!AccountRepo.Select.userExists(body.getUserId()))
        out.put(Keys.Json.KEY_USER_ID, new ArrayList <>(){{
          add("Specified user does not exist.");
        }});
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }

    if (!out.isEmpty())
      throw new UnprocessableEntityException(out);
  }

  public static void validatePatch(List < DatasetProviderPatch > items) {
    // If there is nothing in the patch, it's a bad request.
    if (items.isEmpty())
      throw new BadRequestException();

    // not allowed to do more than one thing
    if (items.size() > 1)
      throw new ForbiddenException();

    var item = items.get(0);

    // only allow replace ops
    if (!"replace".equals(item.getOp()))
      throw new ForbiddenException();

    // only allow modifying the isManager property
    if (!("/" + Keys.Json.KEY_IS_MANAGER).equals(item.getPath()))
      throw new ForbiddenException();
  }

  private static DatasetProviderList list2Providers(
    final List < ProviderRow > rows,
    final int offset,
    final int total
  ) {
    var out = new DatasetProviderListImpl();

    out.setRows(rows.size());
    out.setOffset(offset);
    out.setTotal(total);
    out.setData(rows.stream()
      .map(ProviderService::row2Provider)
      .collect(Collectors.toList()));

    return out;
  }

  private static DatasetProvider row2Provider(ProviderRow row) {
    var user = new UserDetailsImpl();
    user.setUserId(row.getUserId());
    user.setFirstName(row.getFirstName());
    user.setLastName(row.getLastName());
    user.setOrganization(row.getOrganization());

    var out = new DatasetProviderImpl();
    out.setDatasetId(row.getDatasetId());
    out.setIsManager(row.isManager());
    out.setProviderId(row.getProviderId());
    out.setUser(user);

    return out;
  }
}
