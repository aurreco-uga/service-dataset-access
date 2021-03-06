package org.veupathdb.service.access.controller;

import java.util.List;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;

import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.access.generated.model.ApprovalStatus;
import org.veupathdb.service.access.generated.model.EndUserCreateRequest;
import org.veupathdb.service.access.generated.model.EndUserCreateResponseImpl;
import org.veupathdb.service.access.generated.model.EndUserPatch;
import org.veupathdb.service.access.generated.resources.DatasetEndUsers;
import org.veupathdb.service.access.service.user.EndUserService;
import org.veupathdb.service.access.service.user.EndUserValidate;

import static org.veupathdb.service.access.service.provider.ProviderService.userIsManager;
import static org.veupathdb.service.access.service.staff.StaffService.userIsOwner;

@Authenticated
public class EndUserController implements DatasetEndUsers
{
  private final Request request;

  public EndUserController(@Context Request request) {
    this.request = request;
  }

  @Override
  public GetDatasetEndUsersResponse getDatasetEndUsers(
    final String datasetId,
    final int limit,
    final int offset,
    final ApprovalStatus approval
  ) {
    Util.requireDatasetId(datasetId);

    if (!userIsManager(request, datasetId) && !userIsOwner(request))
      throw new ForbiddenException();

    return GetDatasetEndUsersResponse.respond200WithApplicationJson(
      EndUserService.listEndUsers(datasetId, limit, offset, approval));
  }

  @Override
  public PostDatasetEndUsersResponse postDatasetEndUsers(final EndUserCreateRequest entity) {
    final var curUser = Util.requireUser(request);

    final String recordId;
    if (userIsManager(curUser.getUserId(), entity.getDatasetId()) || userIsOwner(curUser.getUserId())) {
      if (EndUserService.endUserExists(entity.getUserId(), entity.getDatasetId()))
        throw new BadRequestException("An end user already exists for the given dataset with the given id");

      EndUserValidate.validateMgrPost(entity);
      recordId = EndUserService.endUserMgrCreate(entity);
    } else if (curUser.getUserId() == entity.getUserId()) {
      if (EndUserService.endUserExists(entity.getUserId(), entity.getDatasetId()))
        throw new BadRequestException("An end user already exists for the given dataset with the given id");

      EndUserValidate.validateSelfCreate(entity);
      recordId = EndUserService.userSelfCreate(entity);
    } else
      throw new ForbiddenException();

    final var out = new EndUserCreateResponseImpl();
    out.setEndUserId(recordId);

    return PostDatasetEndUsersResponse.respond200WithApplicationJson(out);
  }

  @Override
  public GetDatasetEndUsersByEndUserIdResponse getDatasetEndUsersByEndUserId(
    final String endUserId
  ) {
    final var curUser = Util.requireUser(request);
    final var endUser = EndUserService.getEndUser(endUserId);

    if (endUser.getUser().getUserId() == curUser.getUserId()
      || userIsManager(curUser.getUserId(), endUser.getDatasetId())
      || userIsOwner(curUser.getUserId())
    ) {
      return GetDatasetEndUsersByEndUserIdResponse.respond200WithApplicationJson(
        endUser);
    }

    throw new ForbiddenException();
  }

  @Override
  public PatchDatasetEndUsersByEndUserIdResponse patchDatasetEndUsersByEndUserId(
    final String endUserId,
    final List < EndUserPatch > entity
  ) {
    final var curUser = Util.requireUser(request);
    final var endUser = EndUserService.getRawEndUser(endUserId);

    if (endUser.getUserId() == curUser.getUserId()) {
      EndUserService.selfPatch(endUser, entity);
    } else if (userIsManager(curUser.getUserId(), endUser.getDatasetId()) || userIsOwner(curUser.getUserId())) {
      EndUserService.modPatch(endUser, entity);
    } else {
      throw new ForbiddenException();
    }

    return PatchDatasetEndUsersByEndUserIdResponse.respond204();
  }
}
