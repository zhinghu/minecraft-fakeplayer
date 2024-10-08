package io.github.hello09x.fakeplayer.core.repository;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.hello09x.devtools.core.utils.Exceptions;
import io.github.hello09x.fakeplayer.core.Main;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

@Deprecated
@Singleton
public class UsedIdRepository {

    public final static UsedIdRepository instance = new UsedIdRepository();
    private final static Logger log = Main.getInstance().getLogger();

    private final Set<UUID> UUIDS = new HashSet<>();

    @Inject
    public UsedIdRepository() {
        this.load();
    }

    public boolean contains(@NotNull UUID uuid) {
        return UUIDS.contains(uuid);
    }

    public void add(@NotNull UUID uuid) {
        UUIDS.add(uuid);
    }

    public void remove(@NotNull UUID uuid) {
        UUIDS.remove(uuid);
    }

    public boolean exists(@NotNull UUID uuid) {
        return UUIDS.contains(uuid);
    }

    /**
     * 从文件里读取使用过的 UUIDs
     */
    public void load() {
        var file = new File(Main.getInstance().getDataFolder(), "used-uuids.txt");
        if (!file.exists() || !file.isFile()) {
            return;
        }

        try (var in = new FileReader(file)) {
            for (var line : IOUtils.readLines(in)) {
                try {
                    if (line.isBlank()) {
                        continue;
                    }
                    this.UUIDS.add(UUID.fromString(line));
                } catch (Throwable ignored) {
                    log.warning("used-uuids.txt contains illegal UUID: " + line);
                }
            }
        } catch (IOException e) {
            log.warning("Failed to read used-uuids.txt\n" + Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 将使用过的 UUIDs 写入文件
     */
    public void saveAll() {
        var folder = Main.getInstance().getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        var file = new File(folder, "used-uuids.txt");

        try (var out = new FileWriter(file)) {
            IOUtils.writeLines(UUIDS, null, out);
        } catch (IOException e) {
            log.warning("Failed to save used-uuids.txt\n" + Throwables.getStackTraceAsString(e));
        }
    }

    public void onDisable() {
        Exceptions.suppress(Main.getInstance(), this::saveAll);
    }


}
