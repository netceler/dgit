package io.insight.jgit.services;

public interface KVAdapter {

    KVConfigService configService();

    KVRefService refService();

    KVObjectService objService();
}