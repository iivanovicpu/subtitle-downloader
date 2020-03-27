package hr.ii.subtitledownloader;

public enum Languages {
    CRAOTIAN("hrv","hr"), BOSNIAN("bos","bs"), SERBIAN("scc","sr"), ENGLISH("eng","en");

    Languages(String query, String suffix) {
        this.query = query;
        this.suffix = suffix;
    }

    String query;
    String suffix;

    public String getQuery() {
        return query;
    }

    public String getSuffix() {
        return suffix;
    }
}
