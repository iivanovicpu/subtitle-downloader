package hr.ii.subtitledownloader;

import com.github.wtekiela.opensub4j.response.SubtitleFile;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubtitleFile.class);

    public static CommandLineParser PARSER = new DefaultParser();
    public static final Option DIRECTORY = new Option("d", "dir", true, "Full Path to directory which contains movies or tv shows");
    public static final Option NAME = new Option("n", "name", true, "Movie or tv show name");
    public static final Option SERVICE = new Option("s", "service", true, "Running in background and periodically check directory for new files and try to download");
    public static final Option DOWNLOAD_DIR = new Option("t", "download", true, "Optional, full path to download directory. If skipped, working directory used");
    public static final Options options = new Options();

    private static RunModes runMode = RunModes.NOT_DEFINED;
    private static String directoryPath;
    private static String name;
    private static String downloadDir;

    static {
        options.addOption(DIRECTORY);
        options.addOption(NAME);
        options.addOption(SERVICE);
        options.addOption(DOWNLOAD_DIR);
    }


    public static void main(String[] args) {
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine commandLine = PARSER.parse(options, args);
            if (RunModes.NOT_DEFINED.equals(runMode) && commandLine.hasOption(DIRECTORY.getOpt())) {
                directoryPath = commandLine.getOptionValue(DIRECTORY.getOpt());
                downloadDir = commandLine.getOptionValue(DOWNLOAD_DIR.getOpt());
                if (directoryPath != null) {
                    runMode = RunModes.DIRECTORY;
                }
            }
            if (RunModes.NOT_DEFINED.equals(runMode) && commandLine.hasOption(NAME.getOpt())) {
                name = commandLine.getOptionValue(NAME.getOpt());
                downloadDir = commandLine.getOptionValue(DOWNLOAD_DIR.getOpt());
                if (name != null) {
                    runMode = RunModes.NAME;
                }
            }
            if (RunModes.NOT_DEFINED.equals(runMode) && commandLine.hasOption(SERVICE.getOpt())) {
                directoryPath = commandLine.getOptionValue(SERVICE.getOpt());
                if (directoryPath != null) {
                    runMode = RunModes.DIRECTORY;
                }
                runMode = RunModes.SERVICE;
            }
            if (RunModes.NOT_DEFINED.equals(runMode)) {
                formatter.printHelp("joopensubs", options);
                LOGGER.info("Missing required run parameters");
                return;
            }
            LOGGER.info("Subtitle downloader, mode: {}, path: {}, name: {}, download to: {}", runMode, directoryPath, name != null ? name : "-", getDownloadDir());
            download();

        } catch (ParseException e) {
            formatter.printHelp("joopensubs", options);
            LOGGER.error(e.getMessage());
        }
    }

    private static void download() {
        try {
            Downloader downloader = new Downloader();
            downloader.start(runMode, directoryPath, name, getDownloadDir());
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getDownloadDir() {
        if (null != downloadDir)
            return downloadDir;
        if (RunModes.DIRECTORY.equals(runMode))
            return directoryPath;
        return System.getProperty("user.dir");
    }
}
