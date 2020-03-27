package hr.ii.subtitledownloader;

import com.github.wtekiela.opensub4j.impl.OpenSubtitlesClientImpl;
import com.github.wtekiela.opensub4j.response.Response;
import com.github.wtekiela.opensub4j.response.SubtitleFile;
import com.github.wtekiela.opensub4j.response.SubtitleInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.XmlRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubtitleFile.class);
    public static final String USER = "";   //"iivanovic";
    public static final String PASS = "";   //""d1783rt";
    public static final String LANG = "en";
    // this user agent is only for testing and development purposes, see:
    //    https://trac.opensubtitles.org/projects/opensubtitles/wiki/DevReadFirst
    public static final String USER_AGENT = "TemporaryUserAgent";
    private final OpenSubtitlesClientImpl client;
//    private static String path;

    public Downloader() throws MalformedURLException {
        URL url = new URL("https", "api.opensubtitles.org", 443, "/xml-rpc");
        client = new OpenSubtitlesClientImpl(url);
    }

    public static void main(String[] args) throws IOException {
        File yourFile = new File("D:\\Downloads\\test\\subs\\1");
        if (!yourFile.exists()) {
            Files.createDirectories(yourFile.toPath());
        }

        LOGGER.info("Finished !!!");
    }

    public void start(RunModes runMode, String directoryPath, String name, String downloadDir) {
        switch (runMode) {
            case DIRECTORY:
                directoryDownload(directoryPath, downloadDir);
                break;
            case NAME:
                downloadByName(name, downloadDir);
                break;
            case SERVICE:
                LOGGER.error("Service downloader not implemented yet!");
                break;
            case NOT_DEFINED:
                LOGGER.error("Run mode not defined");
                break;
        }
        LOGGER.info("Finished !!!");
    }

    private void directoryDownload(String path, String downloadDir) {
        File file = new File(path);
        List<String> files = getFilesList(file);
        try {
            processFiles(files, path, downloadDir);
        } catch (XmlRpcException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private void downloadByName(String name, String downloadDir) {
        if (login()) {
            for (Languages language : Languages.values()) {
                LOGGER.info("Searching for language: ({}-{})", language.query, language.suffix);
                searchByTitleAndLanguage(name, language.getQuery()).forEach(s -> {
                    logSubtitleInfo(s);
                    download(s, createSubtitleFileName(name, language.getSuffix(), s.getFileName()), downloadDir);
                });
            }
        }
        logout();
    }

    private static List<String> getFilesList(File path) {
        List<String> files = null;
        String[] filesArr = path.list(getFilenameFilter());
        if (null != filesArr && filesArr.length > 0) {
            files = Arrays.asList(filesArr);
        }
        return files;
    }

    private static FilenameFilter getFilenameFilter() {
        return (dir, name) -> name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".asf") || name.endsWith(".mkv");
    }

    private void processFiles(List<String> movieFiles, String path, String downloadDir) throws XmlRpcException {
        if (movieFiles == null || movieFiles.isEmpty()) {
            LOGGER.info("There are no files on path: {}", path);
            return;
        }
        if (login()) {
            LOGGER.info("ServerInfo: {}", client.serverInfo());

            // TODO: 29.1.2020. -> languages from arguments or properties
            movieFiles.forEach(m -> {
                for (Languages language : Languages.values()) {
                    LOGGER.info("Searching for language: ({}-{})", language.query, language.suffix);
                    searchByTitleAndLanguage(m, language.getQuery()).forEach(s -> {
                        logSubtitleInfo(s);
                        download(s, createSubtitleFileName(m, language.getSuffix(), s.getFileName()), null == downloadDir ? path : downloadDir);
                    });
                }
            });
        }
        logout();
    }
    // preraditi da bude Consumer i pozivati kao lambda expression

    public static String createSubtitleFileName(String title, String language, String originalSubtitleFileName) {
        int i = title.lastIndexOf(".");
        String subtitleExtension = getFileExtension(originalSubtitleFileName);
        if (i != -1 && i != 0) {
            return title.substring(0, i) + "_" + language + subtitleExtension;
        }
        return title + "_" + language + subtitleExtension;
    }

    public static String getFileExtension(String filename) {
        int idx = filename.lastIndexOf(".");
        String extension = idx != -1 && idx != 0 ? filename.substring(idx) : ".srt";
        LOGGER.info("Extension for: {} is {}", filename, extension);
        return extension;
    }

    public List<SubtitleInfo> searchByTitleAndLanguage(String movieTitle, String language) {
        List<SubtitleInfo> subtitleInfoList = new ArrayList<>();
        String season = parseSeason(movieTitle);
        String episode = parseEpisode(movieTitle);

        try {
            List<SubtitleInfo> subtitleInfoListResponse = client.searchSubtitles(language, movieTitle, season, episode);
            if (null != subtitleInfoListResponse) {
                subtitleInfoListResponse.stream().limit(1).forEach(subtitleInfoList::add);
            }
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
        return subtitleInfoList;
    }

    private void logSubtitleInfo(SubtitleInfo subtitleInfo) {
        LOGGER.info("Found subtitle: {} {} {} {} {} {} {} {} {} ",
                subtitleInfo.getLanguage(),
                subtitleInfo.getSubtitleFileId(),
                subtitleInfo.getOsLink(),
                subtitleInfo.getZipDownloadLink(),
                subtitleInfo.getDownloadsNo(),
                subtitleInfo.getFileName(),
                subtitleInfo.getDownloadLink(),
                subtitleInfo.getFormat(),
                subtitleInfo.getEncoding());
    }

    private String parseSeason(String fileName) {
        Pattern p = Pattern.compile("(.*?)[.\\s][sS](\\d{2}).*");
        Matcher m = p.matcher(fileName);
        if (m.matches()) {
            return m.group(2);
        }
        return null;
    }

    private String parseEpisode(String fileName) {
        Pattern p = Pattern.compile("(.*?)[.\\s][eE](\\d{2}).*");
        Matcher m = p.matcher(fileName);
        if (m.matches()) {
            return m.group(2);
        }
        return null;
    }

    public void download(SubtitleInfo subtitleInfo, String fileName, String path) {
        LOGGER.info("Download");
        List<SubtitleFile> subtitleFileListResponse;
        try {
            createDirIfNotExists(path);
            try {
                subtitleFileListResponse = client.downloadSubtitles(subtitleInfo.getSubtitleFileId());
                if (null == subtitleFileListResponse) {
                    return;
                }
                subtitleFileListResponse.forEach(f -> {
                    Path fullPath = Paths.get(path + "/" + fileName);
                    try {
                        Files.write(fullPath, f.getContentAsString(StringUtils.isNotEmpty(subtitleInfo.getEncoding()) ? subtitleInfo.getEncoding() : "UTF-8").getBytes());
                        LOGGER.info("Subtitle file downloaded as: {}", fullPath);
                    } catch (IOException e) {
                        LOGGER.error("Error saving file: {}", fullPath);
                        e.printStackTrace();
                    }
                });
            } catch (RuntimeException e) {
                LOGGER.info("Error downloading subtitle: {}", subtitleInfo);
                e.printStackTrace();
            }
        } catch (XmlRpcException | IOException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }

    }

    private void createDirIfNotExists(String path) throws IOException {
        File yourFile = new File(path);
        if (!yourFile.exists()) {
            Files.createDirectories(yourFile.toPath());
        }

    }

    public boolean login() {
        LOGGER.info("Login");
        if (client.isLoggedIn()) {
            return true;
        }

        try {
            Response loginResponse = client.login(USER, PASS, LANG, USER_AGENT);
            LOGGER.info("Login status: {}", loginResponse.getStatus());
            LOGGER.info("Login status: {}", loginResponse.getStatus());
            LOGGER.info("Logged In: {}", client.isLoggedIn());
        } catch (XmlRpcException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
        return client.isLoggedIn();
    }

    public void logout() {
        try {
            client.logout();
        } catch (XmlRpcException e) {
            LOGGER.error(e.getMessage());
            e.printStackTrace();
        }
    }
}
