package io.insigit.jgit;

import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.transport.PackParser;
import org.eclipse.jgit.transport.PackedObjectInfo;

import java.io.IOException;
import java.io.InputStream;

public class RpcPackParser extends PackParser {
  public RpcPackParser(RpcObjDatabase db, InputStream src) {
    super(db,src);
  }

  @Override
  protected void onStoreStream(byte[] raw, int pos, int len) throws IOException {

  }

  @Override
  protected void onObjectHeader(Source src, byte[] raw, int pos, int len) throws IOException {

  }

  @Override
  protected void onObjectData(Source src, byte[] raw, int pos, int len) throws IOException {

  }

  @Override
  protected void onInflatedObjectData(PackedObjectInfo obj, int typeCode, byte[] data) throws IOException {

  }

  @Override
  protected void onPackHeader(long objCnt) throws IOException {

  }

  @Override
  protected void onPackFooter(byte[] hash) throws IOException {

  }

  @Override
  protected boolean onAppendBase(int typeCode, byte[] data, PackedObjectInfo info) throws IOException {
    return false;
  }

  @Override
  protected void onEndThinPack() throws IOException {

  }

  @Override
  protected ObjectTypeAndSize seekDatabase(PackedObjectInfo obj, ObjectTypeAndSize info) throws IOException {
    return null;
  }

  @Override
  protected ObjectTypeAndSize seekDatabase(UnresolvedDelta delta, ObjectTypeAndSize info) throws IOException {
    return null;
  }

  @Override
  protected int readDatabase(byte[] dst, int pos, int cnt) throws IOException {
    return 0;
  }

  @Override
  protected boolean checkCRC(int oldCRC) {
    return false;
  }

  @Override
  protected void onBeginWholeObject(long streamPosition, int type, long inflatedSize) throws IOException {

  }

  @Override
  protected void onEndWholeObject(PackedObjectInfo info) throws IOException {

  }

  @Override
  protected void onBeginOfsDelta(long deltaStreamPosition, long baseStreamPosition, long inflatedSize) throws IOException {

  }

  @Override
  protected void onBeginRefDelta(long deltaStreamPosition, AnyObjectId baseId, long inflatedSize) throws IOException {

  }
}
