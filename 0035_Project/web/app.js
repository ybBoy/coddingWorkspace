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
    const randomModalEl = document.getElementById('randomModal');
    const randomMovieContentEl = document.getElementById('randomMovieContent');
    const importOptionsEl = document.getElementById('importOptions');
    const ratingStatsEl = document.getElementById('ratingStats');
    const yearStatsEl = document.getElementById('yearStats');
    const batchToolbarEl = document.getElementById('batchToolbar');
    const selectedCountEl = document.getElementById('selectedCount');
    const movieTableContainer = document.getElementById('movieTable');
    const movieTableBody = document.getElementById('movieTableBody');
    const selectAllCheckbox = document.getElementById('selectAllCheckbox');
    const tableSelectAll = document.getElementById('tableSelectAll');
    const batchStatusSelect = document.getElementById('batchStatusSelect');

    let currentFilter = { status: 'all', genre: 'all', search: '', sort: 'default' };
    let searchTimer = null;
    let lastImportFile = null;
    let selectedIds = new Set();
    let currentView = 'card';
    let currentMovies = [];

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

    function loadRatingStats() {
        apiRequest('/api/rating-stats').then(function(stats) {
            renderRatingStats(stats);
        }).catch(function(err) {
            console.error('加载评分统计失败:', err);
        });
    }

    function renderRatingStats(stats) {
        if (!stats || Object.keys(stats).length === 0) {
            ratingStatsEl.innerHTML = '<span class="empty-hint-sm">暂无数据</span>';
            return;
        }
        let total = 0;
        Object.keys(stats).forEach(function(k) { total += stats[k]; });
        ratingStatsEl.innerHTML = '';
        Object.keys(stats).forEach(function(label) {
            const count = stats[label];
            const percent = total > 0 ? Math.round(count * 100 / total) : 0;
            const item = document.createElement('div');
            item.className = 'rating-item';
            item.innerHTML =
                '<span class="rating-label">' + label + '</span>' +
                '<div class="rating-bar"><div class="rating-bar-fill" style="width:' + percent + '%"></div></div>' +
                '<span class="rating-count">' + count + '</span>';
            ratingStatsEl.appendChild(item);
        });
    }

    function loadYearStats() {
        apiRequest('/api/year-stats').then(function(stats) {
            renderYearStats(stats);
        }).catch(function(err) {
            console.error('加载年度统计失败:', err);
        });
    }

    function renderYearStats(stats) {
        if (!stats || Object.keys(stats).length === 0) {
            yearStatsEl.innerHTML = '<span class="empty-hint-sm">暂无数据</span>';
            return;
        }
        let max = 0;
        Object.keys(stats).forEach(function(k) {
            if (stats[k] > max) max = stats[k];
        });
        yearStatsEl.innerHTML = '';
        const years = Object.keys(stats).slice(0, 8);
        years.forEach(function(year) {
            const count = stats[year];
            const percent = max > 0 ? Math.round(count * 100 / max) : 0;
            const item = document.createElement('div');
            item.className = 'year-item';
            item.innerHTML =
                '<span class="year-label">' + year + '</span>' +
                '<div class="year-bar"><div class="year-bar-fill" style="width:' + percent + '%"></div></div>' +
                '<span class="year-count">' + count + '</span>';
            yearStatsEl.appendChild(item);
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
        currentMovies = movies || [];
        statResultEl.textContent = currentMovies.length;

        if (currentView === 'card') {
            renderCardView(currentMovies);
        } else {
            renderTableView(currentMovies);
        }
        updateBatchToolbar();
    }

    function renderCardView(movies) {
        if (!movies || movies.length === 0) {
            movieListEl.innerHTML = '<p class="empty-hint">暂无电影，快来添加第一部吧！</p>';
            return;
        }

        movieListEl.innerHTML = '';
        movies.forEach(function(movie) {
            const card = document.createElement('div');
            card.className = 'movie-card';
            if (selectedIds.has(movie.id)) card.classList.add('selected');
            card.dataset.id = movie.id;

            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'card-checkbox';
            checkbox.checked = selectedIds.has(movie.id);
            checkbox.addEventListener('change', function(e) {
                e.stopPropagation();
                toggleSelect(movie.id, this.checked);
            });
            card.appendChild(checkbox);

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

            if (movie.tags && movie.tags.trim()) {
                const tagsEl = document.createElement('div');
                tagsEl.className = 'tags-container';
                const tagArr = movie.tags.split(/[,，、\/]/);
                tagArr.forEach(function(t) {
                    t = t.trim();
                    if (t) {
                        const tag = document.createElement('span');
                        tag.className = 'tag';
                        tag.textContent = t;
                        tagsEl.appendChild(tag);
                    }
                });
                cardContent.appendChild(tagsEl);
            }

            const watchInfo = document.createElement('div');
            watchInfo.className = 'watch-info';
            let watchInfoHtml = '';
            if (movie.priority > 0) {
                watchInfoHtml += '优先级: ' + '★'.repeat(movie.priority) + '　';
            }
            if (movie.watchDate) {
                watchInfoHtml += '观看: ' + movie.watchDate + '　';
            }
            if (movie.rewatchCount > 0) {
                watchInfoHtml += '重刷: ' + movie.rewatchCount + '次';
            }
            if (watchInfoHtml) {
                watchInfo.innerHTML = watchInfoHtml;
                cardContent.appendChild(watchInfo);
            }

            if (movie.comment) {
                const commentEl = document.createElement('div');
                commentEl.className = 'movie-comment';
                commentEl.textContent = movie.comment;
                cardContent.appendChild(commentEl);
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

    function toggleSelect(id, checked) {
        if (checked) {
            selectedIds.add(id);
        } else {
            selectedIds.delete(id);
        }
        updateBatchToolbar();
        updateCardSelection();
        updateTableSelection();
    }

    function toggleSelectAll(checkboxEl) {
        if (checkboxEl.checked) {
            currentMovies.forEach(function(m) { selectedIds.add(m.id); });
        } else {
            selectedIds.clear();
        }
        if (selectAllCheckbox) selectAllCheckbox.checked = checkboxEl.checked;
        if (tableSelectAll) tableSelectAll.checked = checkboxEl.checked;
        updateBatchToolbar();
        updateCardSelection();
        updateTableSelection();
    }

    function clearSelection() {
        selectedIds.clear();
        if (selectAllCheckbox) selectAllCheckbox.checked = false;
        if (tableSelectAll) tableSelectAll.checked = false;
        updateBatchToolbar();
        updateCardSelection();
        updateTableSelection();
    }

    function updateBatchToolbar() {
        if (!batchToolbarEl) return;
        const count = selectedIds.size;
        if (selectedCountEl) selectedCountEl.textContent = count;
        if (count > 0) {
            batchToolbarEl.style.display = 'flex';
        } else {
            batchToolbarEl.style.display = 'none';
        }
    }

    function updateCardSelection() {
        if (currentView !== 'card') return;
        const cards = document.querySelectorAll('.movie-card');
        cards.forEach(function(card) {
            const id = card.dataset.id;
            const checkbox = card.querySelector('.card-checkbox');
            if (selectedIds.has(id)) {
                card.classList.add('selected');
                if (checkbox) checkbox.checked = true;
            } else {
                card.classList.remove('selected');
                if (checkbox) checkbox.checked = false;
            }
        });
    }

    function updateTableSelection() {
        if (currentView !== 'table') return;
        const rows = movieTableBody.querySelectorAll('tr');
        rows.forEach(function(row) {
            const id = row.dataset.id;
            const checkbox = row.querySelector('.row-checkbox');
            if (selectedIds.has(id)) {
                row.style.backgroundColor = 'rgba(212, 175, 55, 0.1)';
                if (checkbox) checkbox.checked = true;
            } else {
                row.style.backgroundColor = '';
                if (checkbox) checkbox.checked = false;
            }
        });
    }

    function batchDelete() {
        if (selectedIds.size === 0) return;
        if (!confirm('确定要删除选中的 ' + selectedIds.size + ' 部电影吗？')) return;
        const ids = Array.from(selectedIds);
        apiRequest('/api/batch/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ids: ids })
        }).then(function(res) {
            alert('已删除 ' + (res.deleted || 0) + ' 部电影');
            selectedIds.clear();
            refreshAll();
        }).catch(function(err) {
            alert('批量删除失败: ' + err.message);
        });
    }

    function batchUpdateStatus() {
        if (selectedIds.size === 0) return;
        const status = batchStatusSelect.value;
        if (!status) {
            alert('请选择要修改的状态');
            return;
        }
        const ids = Array.from(selectedIds);
        apiRequest('/api/batch/status', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ ids: ids, status: status })
        }).then(function(res) {
            alert('已修改 ' + (res.updated || 0) + ' 部电影的状态');
            selectedIds.clear();
            batchStatusSelect.value = '';
            refreshAll();
        }).catch(function(err) {
            alert('批量修改失败: ' + err.message);
        });
    }

    function switchView(view) {
        currentView = view;
        const btnCard = document.getElementById('btnCardView');
        const btnTable = document.getElementById('btnTableView');
        if (btnCard) btnCard.classList.toggle('active', view === 'card');
        if (btnTable) btnTable.classList.toggle('active', view === 'table');

        if (view === 'card') {
            movieListEl.style.display = '';
            movieTableContainer.style.display = 'none';
            renderCardView(currentMovies);
        } else {
            movieListEl.style.display = 'none';
            movieTableContainer.style.display = 'block';
            renderTableView(currentMovies);
        }
    }

    function renderTableView(movies) {
        if (!movies || movies.length === 0) {
            movieTableBody.innerHTML = '<tr><td colspan="10" style="text-align:center;padding:30px;color:#5a4a3a;">暂无电影</td></tr>';
            return;
        }

        movieTableBody.innerHTML = '';
        movies.forEach(function(movie) {
            const tr = document.createElement('tr');
            tr.dataset.id = movie.id;
            if (selectedIds.has(movie.id)) {
                tr.style.backgroundColor = 'rgba(212, 175, 55, 0.1)';
            }

            const tdCheck = document.createElement('td');
            const checkbox = document.createElement('input');
            checkbox.type = 'checkbox';
            checkbox.className = 'row-checkbox';
            checkbox.checked = selectedIds.has(movie.id);
            checkbox.addEventListener('change', function() {
                toggleSelect(movie.id, this.checked);
            });
            tdCheck.appendChild(checkbox);
            tr.appendChild(tdCheck);

            const tdName = document.createElement('td');
            tdName.textContent = movie.name || '';
            tdName.style.fontWeight = '600';
            tdName.style.color = '#f0d78c';
            tr.appendChild(tdName);

            const tdDir = document.createElement('td');
            tdDir.textContent = movie.director || '-';
            tr.appendChild(tdDir);

            const tdYear = document.createElement('td');
            tdYear.textContent = movie.year || '-';
            tr.appendChild(tdYear);

            const tdGenre = document.createElement('td');
            tdGenre.textContent = movie.genre || '-';
            tr.appendChild(tdGenre);

            const tdStatus = document.createElement('td');
            const statusBadge = document.createElement('span');
            statusBadge.className = 'status-badge status-' + (movie.status || '想看');
            statusBadge.textContent = movie.status || '想看';
            tdStatus.appendChild(statusBadge);
            tr.appendChild(tdStatus);

            const tdRating = document.createElement('td');
            tdRating.innerHTML = renderStars(movie.rating);
            tr.appendChild(tdRating);

            const tdTags = document.createElement('td');
            tdTags.textContent = movie.tags || '-';
            tdTags.style.maxWidth = '120px';
            tdTags.style.overflow = 'hidden';
            tdTags.style.textOverflow = 'ellipsis';
            tdTags.style.whiteSpace = 'nowrap';
            tr.appendChild(tdTags);

            const tdPriority = document.createElement('td');
            tdPriority.textContent = movie.priority > 0 ? '★'.repeat(movie.priority) : '-';
            tdPriority.style.color = '#d4af37';
            tr.appendChild(tdPriority);

            const tdActions = document.createElement('td');
            const actionsDiv = document.createElement('div');
            actionsDiv.className = 'table-actions';

            const editBtn = document.createElement('button');
            editBtn.className = 'table-btn';
            editBtn.textContent = '编辑';
            editBtn.addEventListener('click', function() { openEditModal(movie); });
            actionsDiv.appendChild(editBtn);

            const delBtn = document.createElement('button');
            delBtn.className = 'table-btn delete';
            delBtn.textContent = '删除';
            delBtn.addEventListener('click', function() {
                if (confirm('确定要删除《' + movie.name + '》吗？')) {
                    deleteMovie(movie.id);
                }
            });
            actionsDiv.appendChild(delBtn);

            tdActions.appendChild(actionsDiv);
            tr.appendChild(tdActions);

            movieTableBody.appendChild(tr);
        });
    }

    function loadStatusStats() {
        apiRequest('/api/status-stats').then(function(data) {
            const total = data.total || 1;
            const watched = data.watched || 0;
            const want = data.wantToWatch || 0;
            const shelved = data.shelved || 0;

            document.getElementById('barWatchedNum').textContent = watched;
            document.getElementById('barWantNum').textContent = want;
            document.getElementById('barShelvedNum').textContent = shelved;

            setTimeout(function() {
                document.getElementById('barWatched').style.width = ((watched / total) * 100).toFixed(1) + '%';
                document.getElementById('barWant').style.width = ((want / total) * 100).toFixed(1) + '%';
                document.getElementById('barShelved').style.width = ((shelved / total) * 100).toFixed(1) + '%';
            }, 50);

            const avgEl = document.getElementById('avgRating');
            if (data.avgRating > 0) {
                avgEl.innerHTML = '<span class="star">★</span>' + Number(data.avgRating).toFixed(1);
            } else {
                avgEl.textContent = '-';
            }
        }).catch(function(err) {
            console.error('加载状态统计失败:', err);
        });
    }

    function exportMovies() {
        window.open(API_BASE + '/api/export', '_blank');
    }

    function importMovies(event) {
        const file = event.target.files[0];
        if (!file) return;
        lastImportFile = file;
        importOptionsEl.style.display = 'flex';
    }

    function executeImport() {
        if (!lastImportFile) return;
        const mode = document.querySelector('input[name="importMode"]:checked').value;
        const overwrite = mode === 'overwrite';

        const reader = new FileReader();
        reader.onload = function(e) {
            try {
                const content = e.target.result;
                const url = API_BASE + '/api/import?overwrite=' + overwrite;
                fetch(url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: content
                }).then(function(res) {
                    return res.json();
                }).then(function(data) {
                    if (data.success) {
                        alert('导入成功！共导入 ' + data.imported + ' 部电影');
                        refreshAll();
                        lastImportFile = null;
                        importOptionsEl.style.display = 'none';
                        document.getElementById('importFile').value = '';
                    } else {
                        throw new Error(data.error || '导入失败');
                    }
                }).catch(function(err) {
                    alert('导入失败: ' + err.message);
                });
            } catch (err) {
                alert('文件读取失败: ' + err.message);
            }
        };
        reader.readAsText(lastImportFile, 'UTF-8');
    }

    function getRandomMovie() {
        apiRequest('/api/random').then(function(movie) {
            randomModalEl.style.display = 'flex';
            renderRandomMovie(movie);
        }).catch(function(err) {
            randomModalEl.style.display = 'flex';
            randomMovieContentEl.innerHTML = '<div class="empty-random">🎬 ' +
                (err.message || '没有想看的电影，先添加一些吧！') + '</div>';
        });
    }

    function renderRandomMovie(movie) {
        let posterHtml = '';
        if (movie.posterUrl) {
            posterHtml = '<img src="' + movie.posterUrl + '" alt="' + escapeHtml(movie.name) +
                '" onerror="this.parentElement.innerHTML=\'<div class=\\\'random-poster-placeholder\\\'>🎬</div>\'">';
        } else {
            posterHtml = '<div class="random-poster-placeholder">🎬</div>';
        }

        let ratingHtml = '';
        if (movie.rating > 0) {
            for (let i = 1; i <= 5; i++) {
                ratingHtml += (i <= movie.rating) ?
                    '<span style="color:#f1c40f;">★</span>' :
                    '<span style="color:#4a4a4a;">★</span>';
            }
        }

        let commentHtml = '';
        if (movie.comment) {
            commentHtml = '<div class="random-comment">' + escapeHtml(movie.comment) + '</div>';
        }

        let metaHtml = (movie.director ? escapeHtml(movie.director) + ' · ' : '') +
            (movie.year || '') + (movie.genre ? ' · ' + escapeHtml(movie.genre) : '');

        randomMovieContentEl.innerHTML = '<div class="random-movie-display">' +
            '<div class="random-poster">' + posterHtml + '</div>' +
            '<div class="random-name">' + escapeHtml(movie.name) + '</div>' +
            '<div class="random-meta">' + metaHtml + '</div>' +
            '<div class="random-rating">' + ratingHtml + '</div>' +
            commentHtml +
            '</div>';
    }

    function closeRandomModal() {
        randomModalEl.style.display = 'none';
        randomMovieContentEl.innerHTML = '';
    }

    function cancelImport() {
        lastImportFile = null;
        importOptionsEl.style.display = 'none';
        document.getElementById('importFile').value = '';
    }

    function refreshAll() {
        loadStats();
        loadStatusStats();
        loadGenreStats();
        loadRatingStats();
        loadYearStats();
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
        document.getElementById('editMovieTags').value = movie.tags || '';
        document.getElementById('editMoviePriority').value = movie.priority || 0;
        document.getElementById('editMovieWatchDate').value = movie.watchDate || '';
        document.getElementById('editMovieRewatchCount').value = movie.rewatchCount || 0;
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
            tags: document.getElementById('editMovieTags').value.trim(),
            priority: parseInt(document.getElementById('editMoviePriority').value) || 0,
            watchDate: document.getElementById('editMovieWatchDate').value || '',
            rewatchCount: parseInt(document.getElementById('editMovieRewatchCount').value) || 0,
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
            tags: document.getElementById('movieTags').value.trim(),
            priority: parseInt(document.getElementById('moviePriority').value) || 0,
            watchDate: document.getElementById('movieWatchDate').value || '',
            rewatchCount: parseInt(document.getElementById('movieRewatchCount').value) || 0,
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

    window.getRandomMovie = getRandomMovie;
    window.closeRandomModal = closeRandomModal;
    window.exportMovies = exportMovies;
    window.importMovies = importMovies;
    window.executeImport = executeImport;
    window.cancelImport = cancelImport;
    window.switchView = switchView;
    window.toggleSelectAll = toggleSelectAll;
    window.batchDelete = batchDelete;
    window.batchUpdateStatus = batchUpdateStatus;
    window.clearSelection = clearSelection;

    refreshAll();
})();
