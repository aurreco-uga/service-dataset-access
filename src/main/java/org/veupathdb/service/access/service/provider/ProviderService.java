package org.veupathdb.service.access.service.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.Request;

import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.accountdb.UserProfile;
import org.veupathdb.lib.container.jaxrs.errors.UnprocessableEntityException;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.service.access.generated.model.*;
import org.veupathdb.service.access.model.PartialProviderRow;
import org.veupathdb.service.access.model.ProviderRow;
import org.veupathdb.service.access.repo.AccountRepo;
import org.veupathdb.service.access.repo.DatasetRepo;
import org.veupathdb.service.access.util.Keys;

public class ProviderService
{
  static ProviderService instance = new ProviderService();

  ProviderService() {}

  private static final Logger log = LogProvider.logger(ProviderService.class);

  /**
   * Creates a new Provider record from the given request body.
   *
   * @return the ID of the newly created record.
   */
  public int createNewProvider(DatasetProviderCreateRequest body) {
    log.trace("ProviderService#createNewProvider(DatasetProviderCreateRequest)");

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
  public DatasetProviderList getDatasetProviderList(
    final String datasetId,
    final int limit,
    final int offset
  ) {
    log.trace("ProviderService#getDatasetProviderList(String, int, int)");
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
  public ProviderRow mustGetProviderById(final int providerId) {
    log.trace("ProviderService#requireProviderById(int)");

    try {
      return ProviderRepo.Select.byId(providerId)
        .orElseThrow(NotFoundException::new);
    } catch (WebApplicationException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public List < ProviderRow > getProviderByUserId(final long userId) {
    log.trace("ProviderService#getProviderByUserId(long)");

    try {
      return ProviderRepo.Select.byUserId(userId);
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
  public List < ProviderRow > mustGetProviderByUserId(long userId) {
    log.trace("ProviderService#mustGetProviderByUserId(userId)");

    final var out = lookupProviderByUserId(userId);

    if (out.isEmpty())
      throw new ForbiddenException();

    return out;
  }

  public boolean isUserManager(final Request req, final String datasetId) {
    log.trace("ProviderService#userIsManager(req, datasetId)");

    return isUserManager(UserProvider.lookupUser(req)
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
  public boolean isUserManager(final long userId, final String datasetId) {
    log.trace("ProviderService#userIsManager(userId, datasetId)");

    final var ds = datasetId.toUpperCase();
    return lookupProviderByUserId(userId)
      .stream()
      .filter(row -> ds.equals(row.getDatasetId().toUpperCase()))
      .anyMatch(PartialProviderRow::isManager);
  }

  public void deleteProviderRecord(final int providerId) {
    log.trace("ProviderService#deleteProvider(providerId)");

    try {
      ProviderRepo.Delete.byId(providerId);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  public void updateProviderRecord(final ProviderRow row) {
    log.trace("ProviderService#updateProvider(row)");

    try {
      ProviderRepo.Update.isManagerById(row);
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Validate Create Request Body
   * <p>
   * Verifies the following:
   * <ul>
   *   <li>Dataset ID value is set</li>
   *   <li>Dataset ID value is valid (points to a real dataset)</li>
   *   <li>User ID is valid</li>
   *   <li>No provider currently exists for this user/dataset already.</li>
   * </ul>
   *
   * @param body Provider create request body.
   *
   * @throws ForbiddenException if a provider already exists for the given user
   * & dataset id values.
   * @throws UnprocessableEntityException if the dataset id value is not set or
   * invalid, if the user id is not valid.
   * @throws InternalServerErrorException if a database error occurs while
   * attempting to validate this request.
   */
  public void validateCreateRequest(final DatasetProviderCreateRequest body) {
    log.trace("ProviderService#validateCreate(body)");

    final var out = new HashMap<String, List<String>>();

    try {
      if (body.getDatasetId() == null || body.getDatasetId().isBlank())
        out.put(Keys.Json.KEY_DATASET_ID, new ArrayList <>(){{
          add("Dataset ID is required");
        }});
      else if (!DatasetRepo.Select.datasetExists(body.getDatasetId()))
        out.put(Keys.Json.KEY_DATASET_ID, new ArrayList <>(){{
          add("Dataset ID is invalid");
        }});

      if (!AccountRepo.Select.userExists(body.getUserId()))
        out.put(Keys.Json.KEY_USER_ID, new ArrayList <>(){{
          add("Specified user does not exist.");
        }});

      if (ProviderRepo.Select.byUserAndDataset(body.getUserId(), body.getDatasetId()).isPresent())
        throw new ForbiddenException("A provider record already exists for this user and dataset.");
    } catch(WebApplicationException e) {
      throw e;
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }

    if (!out.isEmpty())
      throw new UnprocessableEntityException(out);
  }

  @SuppressWarnings("unchecked")
  public void validatePatchRequest(final List < DatasetProviderPatch > items) {
    log.trace("ProviderService#validatePatch(items)");

    // If there is nothing in the patch, it's a bad request.
    if (items == null || items.isEmpty())
      throw new BadRequestException();

    // not allowed to do more than one thing
    if (items.size() > 1)
      throw new ForbiddenException();

    // WARNING: This cast mess is due to a bug in the JaxRS generator, the type
    // it actually passes up is not the declared type, but a list of linked hash
    // maps instead.
    final var item = ((List<LinkedHashMap<String, Object>>)((Object) items)).get(0);

    // only allow replace ops
    if (!"replace".equals(item.get(Keys.Json.KEY_OP)))
      throw new ForbiddenException();

    // only allow modifying the isManager property
    if (!("/" + Keys.Json.KEY_IS_MANAGER).equals(item.get(Keys.Json.KEY_PATH)))
      throw new ForbiddenException();
  }

  private DatasetProviderList listToProviders(
    final List < ProviderRow > rows,
    final int offset,
    final int total
  ) {
    log.trace("ProviderService#list2Providers(rows, offset, total)");

    var out = new DatasetProviderListImpl();

    out.setRows(rows.size());
    out.setOffset(offset);
    out.setTotal(total);
    out.setData(rows.stream()
      .map(this::rowToProvider)
      .collect(Collectors.toList()));

    return out;
  }

  private DatasetProvider rowToProvider(final ProviderRow row) {
    log.trace("ProviderService#rowToProvider(ProviderRow)");

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




  public static ProviderService getInstance() {
    return instance;
  }

  public static int createProvider(final DatasetProviderCreateRequest body) {
    return getInstance().createNewProvider(body);
  }

  public static DatasetProviderList getProviderList(
    final String datasetId,
    final int limit,
    final int offset
  ) {
    return getInstance().getDatasetProviderList(datasetId, limit, offset);
  }

  public static ProviderRow requireProviderById(final int providerId) {
    return getInstance().mustGetProviderById(providerId);
  }

  public static List < ProviderRow > lookupProviderByUserId(final long userId) {
    return getInstance().getProviderByUserId(userId);
  }

  public static List < ProviderRow > requireProviderByUserId(final long userId) {
    return getInstance().mustGetProviderByUserId(userId);
  }

  public static boolean userIsManager(final Request req, final String datasetId) {
    return getInstance().isUserManager(req, datasetId);
  }

  public static boolean userIsManager(final long userId, final String datasetId) {
    return getInstance().isUserManager(userId, datasetId);
  }

  public static void deleteProvider(final int providerId) {
    getInstance().deleteProviderRecord(providerId);
  }

  public static void updateProvider(final ProviderRow row) {
    getInstance().updateProviderRecord(row);
  }

  public static void validateCreate(final DatasetProviderCreateRequest body) {
    getInstance().validateCreateRequest(body);
  }

  public static void validatePatch(final List < DatasetProviderPatch > items) {
    getInstance().validatePatchRequest(items);
  }

  private static DatasetProviderList list2Providers(
    final List < ProviderRow > rows,
    final int offset,
    final int total
  ) {
    return getInstance().listToProviders(rows, offset, total);
  }
}
