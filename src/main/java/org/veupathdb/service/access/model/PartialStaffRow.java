package org.veupathdb.service.access.model;

public class PartialStaffRow extends UserRow
{
  private boolean isOwner;

  public boolean isOwner() {
    return isOwner;
  }

  public void setOwner(boolean owner) {
    isOwner = owner;
  }
}
