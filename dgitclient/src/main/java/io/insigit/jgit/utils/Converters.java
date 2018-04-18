package io.insigit.jgit.utils;

import io.insight.jgit.Storage;
import org.eclipse.jgit.lib.*;

import java.util.Collections;
import java.util.Map;

public class Converters {
  public static Ref getRef(io.insight.jgit.Ref ref) {
    return getRef(Collections.emptyMap(), ref);
  }

  public static Ref getRef(Map<String, io.insight.jgit.Ref> allRefs, io.insight.jgit.Ref ref) {
    if (ref == null)
      return null;
    if (ref.getSymbolic()) {
      io.insight.jgit.Ref target = allRefs.get(ref.getTarget());
      if (target == null) {
        ObjectIdRef.Unpeeled t = new ObjectIdRef.Unpeeled(Ref.Storage.NEW, ref.getTarget(), null);
        return new SymbolicRef(ref.getName(), t);
      } else {
        return new SymbolicRef(ref.getName(), getRef(allRefs, target));
      }
    } else {
      Ref.Storage storage = Ref.Storage.valueOf(ref.getStorage().name());
      if (ref.hasObjectId()) {
        return new ObjectIdRef.PeeledNonTag(storage, ref.getName(), ObjectId.fromString(ref.getObjectId().getId()));
      }else {
        return new ObjectIdRef.Unpeeled(storage, ref.getName(), null);
      }
    }
  }

  public static io.insight.jgit.Ref toProtoRef(Ref ref) {
    if (ref == null)
      return null;
    io.insight.jgit.Ref.Builder builder = io.insight.jgit.Ref.newBuilder()
        .setName(ref.getName())
        .setStorage(Storage.valueOf(ref.getStorage().name()));

    if (ref.isSymbolic()) {
      return builder.setSymbolic(true)
          .setTarget(ref.getTarget().getName())
          .clearObjectId()
          .build();
    } else {
      builder.setSymbolic(false).clearTarget();
      if (ref.getObjectId() == null) {
        builder.clearObjectId();
      } else {
        builder.setObjectId(io.insight.jgit.ObjectId.newBuilder()
            .setId(ref.getObjectId().getName()));
      }
      return builder.build();
    }
  }

  public static ObjectId fromObjId(io.insight.jgit.ObjectId objId) {
    return ObjectId.fromString(objId.getId());
  }

  public static io.insight.jgit.ObjectId toObjId(AnyObjectId id) {
    return io.insight.jgit.ObjectId.newBuilder().setId(id.name()).build();
  }
}
