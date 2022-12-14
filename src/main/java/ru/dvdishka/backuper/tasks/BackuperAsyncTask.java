package ru.dvdishka.backuper.tasks;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import ru.dvdishka.backuper.common.CommonVariables;
import ru.dvdishka.backuper.common.ConfigVariables;

public class BackuperAsyncTask implements Runnable {

    public void run() {

        try {

            File backupDir = new File("plugins/Backuper/Backups/" +
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            File backupsDir = new File("plugins/Backuper/Backups");
            backupDir.mkdir();

            for (World world : Bukkit.getWorlds()) {

                File worldDir = world.getWorldFolder();

                if (worldDir.listFiles() != null) {

                    try {

                        this.copyFilesInDir(backupDir.toPath().resolve(world.getName()).toFile(), worldDir);

                    } catch (Exception e) {

                        CommonVariables.logger.warning("Something went wrong when trying to copy files!");
                        CommonVariables.logger.warning(e.getMessage());
                    }
                }
            }


            for (World world : Bukkit.getWorlds()) {

                if (!world.getWorldFolder().setWritable(true)) {

                    CommonVariables.logger.warning("Can not set " + world.getWorldFolder().getPath() + " writable!");
                }
            }

            CommonVariables.logger.info("Backup process has been finished!");

            if (ConfigVariables.backupsNumber != 0) {

                ArrayList<LocalDateTime> backups = new ArrayList<>();

                for (File file : backupsDir.listFiles()) {

                    try {

                        backups.add(LocalDateTime.parse(file.getName()));

                    } catch (Exception ignored) {}
                }

                for (int firstBackupsIndex = 0; firstBackupsIndex < backups.size(); firstBackupsIndex++) {

                    for (int secondBackupsIndex = 0; secondBackupsIndex < backups.size(); secondBackupsIndex++) {

                        if (backups.get(firstBackupsIndex).isBefore(backups.get(secondBackupsIndex))) {

                            LocalDateTime saveDate = backups.get(firstBackupsIndex);

                            backups.set(firstBackupsIndex, backups.get(secondBackupsIndex));
                            backups.set(secondBackupsIndex, saveDate);
                        }
                    }
                }

                int backupsToDelete = backups.size() - ConfigVariables.backupsNumber;

                for (LocalDateTime fileName : backups) {

                    if (backupsToDelete <= 0) {

                        break;
                    }

                    try {

                        for (File backup : backupsDir.listFiles()) {

                            String backupFileName = backup.getName();

                            while (backupFileName.length() < fileName.toString().length()) {

                                backupFileName = backupFileName.concat("0");
                            }

                            if (backupFileName.equals(fileName.toString())) {

                                deleteDir(backup);
                            }
                        }
                    } catch (Exception e) {

                        CommonVariables.logger.warning(e.getMessage());
                    }

                    backupsToDelete--;
                }
            }

            if (ConfigVariables.backupsWeight != 0) {

                long backupsFolderWeight = FileUtils.sizeOf(backupsDir);

                if (backupsFolderWeight > ConfigVariables.backupsWeight) {

                    ArrayList<LocalDateTime> backups = new ArrayList<>();

                    for (File file : backupsDir.listFiles()) {

                        try {

                            backups.add(LocalDateTime.parse(file.getName()));

                        } catch (Exception ignored) {}
                    }

                    for (int firstBackupsIndex = 0; firstBackupsIndex < backups.size(); firstBackupsIndex++) {

                        for (int secondBackupsIndex = 0; secondBackupsIndex < backups.size(); secondBackupsIndex++) {

                            if (backups.get(firstBackupsIndex).isBefore(backups.get(secondBackupsIndex))) {

                                LocalDateTime saveDate = backups.get(firstBackupsIndex);

                                backups.set(firstBackupsIndex, backups.get(secondBackupsIndex));
                                backups.set(secondBackupsIndex, saveDate);
                            }
                        }
                    }

                    long bytesToDelete = backupsFolderWeight - ConfigVariables.backupsWeight;

                    for (LocalDateTime fileName : backups) {

                        if (bytesToDelete <= 0) {

                            break;
                        }

                        for (File backup : backupsDir.listFiles()) {

                            String backupFileName = backup.getName();

                            while (backupFileName.length() < fileName.toString().length()) {

                                backupFileName = backupFileName.concat("0");
                            }

                            if (backupFileName.equals(fileName.toString())) {

                                bytesToDelete -= FileUtils.sizeOf(backup);

                                deleteDir(backup);
                            }
                        }
                    }
                }
            }

            if (ConfigVariables.afterBackup.equals("restart")) {

                Bukkit.getServer().spigot().restart();

            } else if (ConfigVariables.afterBackup.equals("stop")) {

                Bukkit.getServer().shutdown();
            }

        } catch (Exception e) {

            for (World world : Bukkit.getWorlds()) {

                if (!world.getWorldFolder().setWritable(true)) {

                    CommonVariables.logger.warning("Can not set " + world.getWorldFolder().getPath() + " writable!");
                }                     }

            CommonVariables.logger.warning("Copy task has finished with an exception!");
            CommonVariables.logger.warning(e.getMessage());
        }

    }

    public void deleteDir(File dir) {

        if (dir != null) {

            for (File file : dir.listFiles()) {

                if (file.isDirectory()) {

                    deleteDir(file);

                } else {

                    if (!file.delete()) {

                        CommonVariables.logger.warning("Can not delete file " + file.getName());
                    }
                }
            }
            if (!dir.delete()) {

                CommonVariables.logger.warning("Can not delete directory " + dir.getName());
            }
        }
    }

    public void copyFilesInDir(File destDir, File dir) {

        if (dir.listFiles() != null) {

            destDir.mkdir();

            for (File file : dir.listFiles()) {

                if (file.isDirectory()) {

                    copyFilesInDir(destDir.toPath().resolve(file.getName()).toFile(), file);

                } else {

                    try {

                        Files.copy(file.toPath(), destDir.toPath().resolve(file.getName()));

                    } catch (Exception e) {

                        CommonVariables.logger.warning("Something went wrong while trying to copy file!");
                        CommonVariables.logger.warning(e.getMessage());
                    }
                }
            }
        }
    }
}
