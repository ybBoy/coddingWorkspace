package model;

public class Movie {
    private String id;
    private String name;
    private String director;
    private int year;
    private String genre;
    private String status;
    private String comment;
    private int rating;
    private String posterUrl;
    private long createdAt;
    private String tags;
    private int priority;
    private String watchDate;
    private int rewatchCount;

    public Movie() {
        this.createdAt = System.currentTimeMillis();
        this.priority = 0;
        this.rewatchCount = 0;
    }

    public Movie(String id, String name, String director, int year, String genre,
                 String status, String comment, int rating, String posterUrl, long createdAt) {
        this.id = id;
        this.name = name;
        this.director = director;
        this.year = year;
        this.genre = genre;
        this.status = status;
        this.comment = comment;
        this.rating = rating;
        this.posterUrl = posterUrl;
        this.createdAt = createdAt;
        this.priority = 0;
        this.rewatchCount = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getWatchDate() {
        return watchDate;
    }

    public void setWatchDate(String watchDate) {
        this.watchDate = watchDate;
    }

    public int getRewatchCount() {
        return rewatchCount;
    }

    public void setRewatchCount(int rewatchCount) {
        this.rewatchCount = rewatchCount;
    }
}
