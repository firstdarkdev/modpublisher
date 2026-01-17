package com.hypherionmc.modpublisher.util.modtale.meta;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ModtaleMetadata {

    private String versionNumber;
    private List<String> gameVersions;
    private String changelog;
    private String channel;

    public static ModtaleMetadata builder() {
        return new ModtaleMetadata();
    }

    public ModtaleMetadata setVersionNumber(String version) {
        this.versionNumber = version;
        return this;
    }

    public ModtaleMetadata addGameVersion(String gameVersion) {
        if (this.gameVersions == null)
            this.gameVersions = new ArrayList<>();

        this.gameVersions.add(gameVersion);
        return this;
    }

    public ModtaleMetadata setGameVersions(List<String> gameVersions) {
        if (this.gameVersions == null) {
            this.gameVersions = new ArrayList<>();
        } else {
            this.gameVersions.clear();
        }

        this.gameVersions.addAll(gameVersions);
        return this;
    }

    public ModtaleMetadata setChangelog(String changelog) {
        this.changelog = changelog;
        return this;
    }

    public ModtaleMetadata setChannel(String channel) {
        this.channel = channel.toUpperCase();
        return this;
    }

}
