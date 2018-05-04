package io.insight.jgit.internal;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefRename;
import org.eclipse.jgit.lib.RefUpdate;

import java.io.IOException;

final class DfsRefRename extends RefRename {
  DfsRefRename(RefUpdate src, RefUpdate dst) {
    super(src, dst);
  }

  /** {@inheritDoc} */
  @Override
  protected RefUpdate.Result doRename() throws IOException {
    // TODO Correctly handle renaming foo/bar to foo.
    // TODO Batch these together into one log update.

    destination.setExpectedOldObjectId(ObjectId.zeroId());
    destination.setNewObjectId(source.getRef().getObjectId());
    switch (destination.update()) {
      case NEW:
        source.delete();
        return RefUpdate.Result.RENAMED;

      default:
        return destination.getResult();
    }
  }
}
