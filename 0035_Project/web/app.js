(function() {
    const API_BASE = '';

    const movieForm = document.getElementById('movieForm');
    const movieListEl = document.getElementById('movieList');
    const filterStatusEl = document.getElementById('filterStatus');
    const filterGenreEl = document.getElementById('filterGenre');
    const sortSelectEl = document.getElementById('sortSelect');
    const searchInputEl = document.getElementById('searchInput');
    const statTotalEl = document.getElementById('statTotal');
    const statWatchedEl = document.getElementById('statWatched');
    const statWantEl = document.getElementById('statWant');
    const statShelvedEl = document.getElementById('statShelved');
    const statResultEl = document.getElementById('statResult');
    const genreStatsEl = document.getElementById('genreStats');
    const editModalEl = document.getElementById('editModal');

    let currentFilter = { status: 'all', genre: 'all', search: '', sort: 'default' };
    let searchTimer = null;

    function apiRequest(url, options) {
        return fetch(API_BASE + url, options).then(function(res) {
            return res.json().catch(function() { return {}; }).then(function(data) {
                if (!res.ok) {
                    throw new Error(data.error || ('请求失败: ' + res.status));
                }
                return data;
            });
        });
    }

    function loadStats() {
        apiRequest('/api/stats').then(function(stats) {
            statTotalEl.textContent = stats.total || 0;
            statWatchedEl.textContent = stats.watched || 0;
            statWantEl.textContent = stats.wantToWatch || 0;
            statShelvedEl.textContent = stats.shelved || 0;
        }).catch(function(err) {
            console.error('加载统计信息失败:', err);
        });
    }

    function loadGenreStats() {
        apiRequest('/api/genre-stats').then(function(stats) {
            renderGenreStats(stats);
        }).catch(function(err) {
            console.error('加载类型统计失败:', err);
        });
    }

    function renderGenreStats(stats) {
        if (!stats || Object.keys(stats).length === 0) {
            genreStatsEl.innerHTML = '<span class="empty-hint-sm">暂无数据</span>';
            return;
        }
        genreStatsEl.innerHTML = '';
        Object.keys(stats).forEach(function(genre) {
            const tag = document.createElement('span');
            tag.className = 'genre-tag';
            if (currentFilter.genre === genre) {
                tag.classList.add('active');
            }
            tag.innerHTML = genre + ' <span class="count">' + stats[genre] + '</span>';
            tag.addEventListener('click', function() {
                if (currentFilter.genre === genre) {
                    currentFilter.genre = 'all';
                    filterGenreEl.value = 'all';
                } else {
                    currentFilter.genre = genre;
                    filterGenreEl.value = genre;
                }
                loadGenreStats();
                loadMovies();
            });
            genreStatsEl.appendChild(tag);
        });
    }

    function loadGenres() {
        apiRequest('/api/genres').then(function(genres) {
            const currentVal = filterGenreEl.value;
            filterGenreEl.innerHTML = '<option value="all">全部</option>';
            genres.forEach(function(g) {
                const opt = document.createElement('option');
                opt.value = g;
                opt.textContent = g;
                filterGenreEl.appendChild(opt);
            });
            filterGenreEl.value = currentVal || 'all';
        }).catch(function(err) {
            console.error('加载类型列表失败:', err);
        });
    }

    function loadMovies() {
        let url = '/api/movies';
        const params = [];
        if (currentFilter.status && currentFilter.status !== 'all') {
            params.push('status=' + encodeURIComponent(currentFilter.status));
        }
        if (currentFilter.genre && currentFilter.genre !== 'all') {
            params.push('genre=' + encodeURIComponent(currentFilter.genre));
        }
        if (currentFilter.search && currentFilter.search.trim()) {
            params.push('search=' + encodeURIComponent(currentFilter.search.trim()));
        }
        if (currentFilter.sort && currentFilter.sort !== 'default') {
            params.push('sort=' + encodeURIComponent(currentFilter.sort));
        }
        if (params.length > 0) {
            url += '?' + params.join('&');
        }

        apiRequest(url).then(function(movies) {
            renderMovies(movies);
        }).catch(function(err) {
            console.error('加载电影列表失败:', err);
            movieListEl.innerHTML = '<p class="empty-hint">加载失败，请刷新重试</p>';
        });
    }

    function renderStars(rating) {
        let html = '';
        const r = parseInt(rating) || 0;
        for (let i = 1; i <= 5; i++) {
            if (i <= r) {
                html += '<span class="star-full">★</span>';
            } else {
                html += '<span class="star-empty">★</span>';
            }
        }
        return html;
    }

    function renderMovies(movies) {
        statResultEl.textContent = movies.length;

        if (!movies || movies.length === 0) {
            movieListEl.innerHTML = '<p class="empty-hint">暂无电影，快来添加第一部吧！</p>';
            return;
        }

        movieListEl.innerHTML = '';
        movies.forEach(function(movie) {
            const card = document.createElement('div');
            card.className = 'movie-card';
            card.dataset.id = movie.id;

            const posterWrapper = document.createElement('div');
            posterWrapper.className = 'poster-wrapper';
            if (movie.posterUrl && movie.posterUrl.trim()) {
                const img = document.createElement('img');
                img.src = movie.posterUrl;
                img.alt = movie.name || '电影海报';
                img.onerror = function() {
                    this.style.display = 'none';
                    const ph = document.createElement('div');
                    ph.className = 'poster-placeholder';
                    ph.innerHTML = '🎬<div class="placeholder-text">NO POSTER</div>';
                    posterWrapper.appendChild(ph);
                };
                posterWrapper.appendChild(img);
            } else {
                const ph = document.createElement('div');
                ph.className = 'poster-placeholder';
                ph.innerHTML = '🎬<div class="placeholder-text">NO POSTER</div>';
                posterWrapper.appendChild(ph);
            }
            card.appendChild(posterWrapper);

            const cardContent = document.createElement('div');
            cardContent.className = 'card-content';

            const header = document.createElement('div');
            header.className = 'movie-header';

            const nameEl = document.createElement('div');
            nameEl.className = 'movie-name';
            nameEl.textContent = movie.name || '（未知电影）';
            header.appendChild(nameEl);

            const metaEl = document.createElement('div');
            metaEl.className = 'movie-meta';

            const statusBadge = document.createElement('span');
            statusBadge.className = 'movie-status status-' + (movie.status || '想看');
            statusBadge.textContent = movie.status || '想看';
            metaEl.appendChild(statusBadge);

            const ratingEl = document.createElement('span');
            ratingEl.className = 'movie-rating';
            ratingEl.innerHTML = renderStars(movie.rating);
            metaEl.appendChild(ratingEl);

            header.appendChild(metaEl);
            cardContent.appendChild(header);

            const info = document.createElement('div');
            info.className = 'movie-info';
            let infoHtml = '';
            if (movie.director) {
                infoHtml += '<div><span>导演：</span>' + escapeHtml(movie.director) + '</div>';
            }
            if (movie.year) {
                infoHtml += '<div><span>年份：</span>' + movie.year + '</div>';
            }
            if (movie.genre) {
                infoHtml += '<div><span>类型：</span>' + escapeHtml(movie.genre) + '</div>';
            }
            info.innerHTML = infoHtml || '<div style="color:#666;">暂无详细信息</div>';
            cardContent.appendChild(info);

            if (movie.comment) {
                const commentEl = document.createElement('div');
                commentEl.className = 'movie-comment';
                commentEl.textContent = movie.comment;
                cardContent.appendChild(commentEl);
            } else {
                const spacer = document.createElement('div');
                spacer.style.flex = '1';
                spacer.style.minHeight = '36px';
                cardContent.appendChild(spacer);
            }

            const actions = document.createElement('div');
            actions.className = 'movie-actions';

            const select = document.createElement('select');
            select.className = 'status-select';
            ['想看', '已看', '搁置'].forEach(function(s) {
                const opt = document.createElement('option');
                opt.value = s;
                opt.textContent = s;
                if (s === movie.status) opt.selected = true;
                select.appendChild(opt);
            });
            actions.appendChild(select);

            const editBtn = document.createElement('button');
            editBtn.className = 'btn-edit';
            editBtn.textContent = '编辑';
            editBtn.addEventListener('click', function() {
                openEditModal(movie);
            });
            actions.appendChild(editBtn);

            const updateBtn = document.createElement('button');
            updateBtn.className = 'btn-update';
            updateBtn.textContent = '改状态';
            updateBtn.addEventListener('click', function() {
                updateMovieStatus(movie.id, select.value);
            });
            actions.appendChild(updateBtn);

            const deleteBtn = document.createElement('button');
            deleteBtn.className = 'btn-delete';
            deleteBtn.textContent = '删除';
            deleteBtn.addEventListener('click', function() {
                if (confirm('确定要删除电影《' + movie.name + '》吗？')) {
                    deleteMovie(movie.id);
                }
            });
            actions.appendChild(deleteBtn);

            cardContent.appendChild(actions);
            card.appendChild(cardContent);
            movieListEl.appendChild(card);
        });
    }

    function escapeHtml(str) {
        if (!str) return '';
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function addMovie(data) {
        apiRequest('/api/movies', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }).then(function() {
            refreshAll();
        }).catch(function(err) {
            alert('添加失败: ' + err.message);
        });
    }

    function updateMovieStatus(id, status) {
        apiRequest('/api/movies/' + encodeURIComponent(id), {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: status })
        }).then(function() {
            refreshAll();
        }).catch(function(err) {
            alert('修改状态失败: ' + err.message);
        });
    }

    function updateMovieFull(id, data) {
        apiRequest('/api/movies/' + encodeURIComponent(id), {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }).then(function() {
            refreshAll();
        }).catch(function(err) {
            alert('修改失败: ' + err.message);
        });
    }

    function deleteMovie(id) {
        apiRequest('/api/movies/' + encodeURIComponent(id), {
            method: 'DELETE'
        }).then(function() {
            refreshAll();
        }).catch(function(err) {
            alert('删除失败: ' + err.message);
        });
    }

    function refreshAll() {
        loadStats();
        loadGenreStats();
        loadGenres();
        loadMovies();
    }

    function openEditModal(movie) {
        document.getElementById('editMovieId').value = movie.id;
        document.getElementById('editMovieName').value = movie.name || '';
        document.getElementById('editMovieDirector').value = movie.director || '';
        document.getElementById('editMovieYear').value = movie.year || '';
        document.getElementById('editMovieGenre').value = movie.genre || '';
        document.getElementById('editMovieStatus').value = movie.status || '想看';
        document.getElementById('editMovieRating').value = movie.rating || 0;
        document.getElementById('editMoviePoster').value = movie.posterUrl || '';
        document.getElementById('editMovieComment').value = movie.comment || '';
        editModalEl.style.display = 'flex';
    }

    window.closeEditModal = function() {
        editModalEl.style.display = 'none';
    };

    window.saveEdit = function() {
        const id = document.getElementById('editMovieId').value;
        const name = document.getElementById('editMovieName').value.trim();
        if (!name) {
            alert('电影名称不能为空');
            return;
        }
        const data = {
            name: name,
            director: document.getElementById('editMovieDirector').value.trim(),
            year: parseInt(document.getElementById('editMovieYear').value) || 0,
            genre: document.getElementById('editMovieGenre').value.trim(),
            status: document.getElementById('editMovieStatus').value,
            rating: parseInt(document.getElementById('editMovieRating').value) || 0,
            posterUrl: document.getElementById('editMoviePoster').value.trim(),
            comment: document.getElementById('editMovieComment').value.trim()
        };
        updateMovieFull(id, data);
        closeEditModal();
    };

    editModalEl.addEventListener('click', function(e) {
        if (e.target === editModalEl) {
            closeEditModal();
        }
    });

    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && editModalEl.style.display === 'flex') {
            closeEditModal();
        }
    });

    movieForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const name = document.getElementById('movieName').value.trim();
        if (!name) {
            alert('请输入电影名称');
            return;
        }

        const data = {
            name: name,
            director: document.getElementById('movieDirector').value.trim(),
            year: parseInt(document.getElementById('movieYear').value) || 0,
            genre: document.getElementById('movieGenre').value.trim(),
            status: document.getElementById('movieStatus').value,
            rating: parseInt(document.getElementById('movieRating').value) || 0,
            posterUrl: document.getElementById('moviePoster').value.trim(),
            comment: document.getElementById('movieComment').value.trim()
        };

        addMovie(data);
        movieForm.reset();
    });

    filterStatusEl.addEventListener('change', function() {
        currentFilter.status = filterStatusEl.value;
        loadMovies();
    });

    filterGenreEl.addEventListener('change', function() {
        currentFilter.genre = filterGenreEl.value;
        loadGenreStats();
        loadMovies();
    });

    sortSelectEl.addEventListener('change', function() {
        currentFilter.sort = sortSelectEl.value;
        loadMovies();
    });

    searchInputEl.addEventListener('input', function() {
        if (searchTimer) {
            clearTimeout(searchTimer);
        }
        searchTimer = setTimeout(function() {
            currentFilter.search = searchInputEl.value;
            loadMovies();
        }, 300);
    });

    refreshAll();
})();
