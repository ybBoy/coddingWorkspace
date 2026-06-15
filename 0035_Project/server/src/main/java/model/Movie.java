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

    public Movie() {
        this.createdAt = System.currentTimeMillis();
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
}
