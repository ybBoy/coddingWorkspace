package service;

import model.Movie;
import store.JsonMovieStore;

import java.util.*;

public class MovieService {
    private final JsonMovieStore store;
    private List<Movie> movies;

    public MovieService(JsonMovieStore store) {
        this.store = store;
        this.movies = store.loadMovies();
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<Movie>(movies);
    }

    public List<Movie> filterMovies(String status, String genre, String search, String sort) {
        List<Movie> result = new ArrayList<Movie>();
        String searchLower = (search != null) ? search.toLowerCase() : null;

        for (Movie m : movies) {
            boolean matchStatus = (status == null || status.isEmpty() || status.equalsIgnoreCase("all")
                    || m.getStatus().equalsIgnoreCase(status));
            boolean matchGenre = (genre == null || genre.isEmpty() || genre.equalsIgnoreCase("all")
                    || m.getGenre().equalsIgnoreCase(genre));
            boolean matchSearch = true;
            if (searchLower != null && !searchLower.isEmpty()) {
                String name = (m.getName() != null) ? m.getName().toLowerCase() : "";
                String director = (m.getDirector() != null) ? m.getDirector().toLowerCase() : "";
                String comment = (m.getComment() != null) ? m.getComment().toLowerCase() : "";
                String tags = (m.getTags() != null) ? m.getTags().toLowerCase() : "";
                matchSearch = name.contains(searchLower) || director.contains(searchLower)
                        || comment.contains(searchLower) || tags.contains(searchLower);
            }
            if (matchStatus && matchGenre && matchSearch) {
                result.add(m);
            }
        }

        sortMovies(result, sort);
        return result;
    }

    private void sortMovies(List<Movie> list, String sort) {
        if (sort == null || sort.isEmpty() || "default".equalsIgnoreCase(sort)) {
            return;
        }
        Comparator<Movie> comparator = null;
        if ("year_desc".equalsIgnoreCase(sort)) {
            comparator = new Comparator<Movie>() {
                @Override
                public int compare(Movie a, Movie b) {
                    return Integer.compare(b.getYear(), a.getYear());
                }
            };
        } else if ("year_asc".equalsIgnoreCase(sort)) {
            comparator = new Comparator<Movie>() {
                @Override
                public int compare(Movie a, Movie b) {
                    return Integer.compare(a.getYear(), b.getYear());
                }
            };
        } else if ("recent".equalsIgnoreCase(sort)) {
            comparator = new Comparator<Movie>() {
                @Override
                public int compare(Movie a, Movie b) {
                    return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                }
            };
        } else if ("rating_desc".equalsIgnoreCase(sort)) {
            comparator = new Comparator<Movie>() {
                @Override
                public int compare(Movie a, Movie b) {
                    return Integer.compare(b.getRating(), a.getRating());
                }
            };
        } else if ("name_asc".equalsIgnoreCase(sort)) {
            comparator = new Comparator<Movie>() {
                @Override
                public int compare(Movie a, Movie b) {
                    String na = (a.getName() != null) ? a.getName() : "";
                    String nb = (b.getName() != null) ? b.getName() : "";
                    return na.compareTo(nb);
                }
            };
        }
        if (comparator != null) {
            Collections.sort(list, comparator);
        }
    }

    public Movie addMovie(Movie movie) {
        movie.setId(store.generateId());
        if (movie.getCreatedAt() == 0) {
            movie.setCreatedAt(System.currentTimeMillis());
        }
        movies.add(movie);
        store.saveMovies(movies);
        return movie;
    }

    public boolean updateStatus(String id, String status) {
        for (Movie m : movies) {
            if (m.getId().equals(id)) {
                m.setStatus(status);
                store.saveMovies(movies);
                return true;
            }
        }
        return false;
    }

    public boolean updateMovie(String id, Movie updated) {
        for (int i = 0; i < movies.size(); i++) {
            Movie m = movies.get(i);
            if (m.getId().equals(id)) {
                if (updated.getName() != null) m.setName(updated.getName());
                if (updated.getDirector() != null) m.setDirector(updated.getDirector());
                if (updated.getYear() > 0) m.setYear(updated.getYear());
                if (updated.getGenre() != null) m.setGenre(updated.getGenre());
                if (updated.getStatus() != null) m.setStatus(updated.getStatus());
                if (updated.getComment() != null) m.setComment(updated.getComment());
                if (updated.getRating() >= 0 && updated.getRating() <= 5) m.setRating(updated.getRating());
                if (updated.getPosterUrl() != null) m.setPosterUrl(updated.getPosterUrl());
                if (updated.getTags() != null) m.setTags(updated.getTags());
                if (updated.getPriority() >= 0) m.setPriority(updated.getPriority());
                if (updated.getWatchDate() != null) m.setWatchDate(updated.getWatchDate());
                if (updated.getRewatchCount() >= 0) m.setRewatchCount(updated.getRewatchCount());
                store.saveMovies(movies);
                return true;
            }
        }
        return false;
    }

    public int batchDelete(List<String> ids) {
        int count = 0;
        for (String id : ids) {
            for (int i = 0; i < movies.size(); i++) {
                if (movies.get(i).getId().equals(id)) {
                    movies.remove(i);
                    count++;
                    break;
                }
            }
        }
        if (count > 0) {
            store.saveMovies(movies);
        }
        return count;
    }

    public int batchUpdateStatus(List<String> ids, String status) {
        int count = 0;
        for (String id : ids) {
            for (Movie m : movies) {
                if (m.getId().equals(id)) {
                    m.setStatus(status);
                    count++;
                    break;
                }
            }
        }
        if (count > 0) {
            store.saveMovies(movies);
        }
        return count;
    }

    public boolean deleteMovie(String id) {
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).getId().equals(id)) {
                movies.remove(i);
                store.saveMovies(movies);
                return true;
            }
        }
        return false;
    }

    public int getTotalCount() {
        return movies.size();
    }

    public int getCountByStatus(String status) {
        int count = 0;
        for (Movie m : movies) {
            if (m.getStatus().equalsIgnoreCase(status)) {
                count++;
            }
        }
        return count;
    }

    public List<String> getAllGenres() {
        Set<String> genres = new LinkedHashSet<String>();
        for (Movie m : movies) {
            if (m.getGenre() != null && !m.getGenre().isEmpty()) {
                genres.add(m.getGenre());
            }
        }
        return new ArrayList<String>(genres);
    }

    public Map<String, Integer> getGenreStats() {
        Map<String, Integer> stats = new LinkedHashMap<String, Integer>();
        for (Movie m : movies) {
            String genre = m.getGenre();
            if (genre == null || genre.isEmpty()) continue;
            String[] parts = genre.split("[/、,，]");
            for (String part : parts) {
                String g = part.trim();
                if (!g.isEmpty()) {
                    Integer count = stats.get(g);
                    if (count == null) count = 0;
                    stats.put(g, count + 1);
                }
            }
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(stats.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return Integer.compare(b.getValue(), a.getValue());
            }
        });
        Map<String, Integer> sorted = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> e : list) {
            sorted.put(e.getKey(), e.getValue());
        }
        return sorted;
    }

    public Movie getMovieById(String id) {
        for (Movie m : movies) {
            if (m.getId().equals(id)) {
                return m;
            }
        }
        return null;
    }

    public Map<String, Object> getStatusStats() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        int total = movies.size();
        int watched = getCountByStatus("已看");
        int wantToWatch = getCountByStatus("想看");
        int shelved = getCountByStatus("搁置");

        result.put("total", total);
        result.put("watched", watched);
        result.put("wantToWatch", wantToWatch);
        result.put("shelved", shelved);

        if (total > 0) {
            result.put("watchedPercent", Math.round(watched * 100.0 / total));
            result.put("wantToWatchPercent", Math.round(wantToWatch * 100.0 / total));
            result.put("shelvedPercent", Math.round(shelved * 100.0 / total));
        } else {
            result.put("watchedPercent", 0);
            result.put("wantToWatchPercent", 0);
            result.put("shelvedPercent", 0);
        }

        int ratedCount = 0;
        double totalRating = 0;
        for (Movie m : movies) {
            if (m.getRating() > 0) {
                ratedCount++;
                totalRating += m.getRating();
            }
        }
        result.put("ratedCount", ratedCount);
        if (ratedCount > 0) {
            result.put("avgRating", Math.round(totalRating / ratedCount * 10.0) / 10.0);
        } else {
            result.put("avgRating", 0);
        }

        return result;
    }

    public Map<String, Integer> getYearStats() {
        Map<String, Integer> stats = new TreeMap<String, Integer>(Collections.reverseOrder());
        for (Movie m : movies) {
            int year = m.getYear();
            if (year > 0) {
                String key = String.valueOf(year);
                Integer count = stats.get(key);
                if (count == null) count = 0;
                stats.put(key, count + 1);
            }
        }
        return stats;
    }

    public Map<String, Integer> getRatingStats() {
        Map<String, Integer> stats = new LinkedHashMap<String, Integer>();
        for (int i = 5; i >= 1; i--) {
            stats.put(i + "星", 0);
        }
        stats.put("未评分", 0);
        for (Movie m : movies) {
            int rating = m.getRating();
            if (rating > 0 && rating <= 5) {
                String key = rating + "星";
                stats.put(key, stats.get(key) + 1);
            } else {
                stats.put("未评分", stats.get("未评分") + 1);
            }
        }
        return stats;
    }

    public String exportAll() {
        Map<String, Object> export = new LinkedHashMap<String, Object>();
        export.put("exportDate", new java.util.Date().toString());
        export.put("totalCount", movies.size());
        export.put("movies", movies);
        return store.toJson(export);
    }

    public int importMovies(List<Movie> newMovies, boolean overwrite) {
        if (overwrite) {
            movies.clear();
        }
        int count = 0;
        for (Movie m : newMovies) {
            if (m.getName() == null || m.getName().trim().isEmpty()) continue;
            m.setId(store.generateId());
            if (m.getCreatedAt() == 0) {
                m.setCreatedAt(System.currentTimeMillis());
            }
            if (m.getStatus() == null || m.getStatus().isEmpty()) {
                m.setStatus("想看");
            }
            movies.add(m);
            count++;
        }
        store.saveMovies(movies);
        return count;
    }

    public Movie getRandomWantToWatch() {
        List<Movie> wantList = new ArrayList<Movie>();
        for (Movie m : movies) {
            if ("想看".equalsIgnoreCase(m.getStatus())) {
                wantList.add(m);
            }
        }
        if (wantList.isEmpty()) {
            return null;
        }
        Random random = new Random();
        int index = random.nextInt(wantList.size());
        return wantList.get(index);
    }
}
