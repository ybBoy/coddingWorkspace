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
                matchSearch = name.contains(searchLower) || director.contains(searchLower);
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
                store.saveMovies(movies);
                return true;
            }
        }
        return false;
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
}
