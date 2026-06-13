package service;

import domain.*;
import store.JsonFileStore;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ReadingService {
    private Article article;
    private final List<Note> notes;
    private final JsonFileStore store;

    public ReadingService(JsonFileStore store) {
        this.store = store;
        this.notes = new CopyOnWriteArrayList<>();

        Optional<JsonFileStore.StoreData> loaded = store.load();
        if (loaded.isPresent()) {
            JsonFileStore.StoreData data = loaded.get();
            this.article = data.getArticle();
            if (data.getNotes() != null) {
                this.notes.addAll(data.getNotes());
            }
        } else {
            this.article = createDefaultArticle();
        }
    }

    private Article createDefaultArticle() {
        List<Paragraph> paragraphs = new ArrayList<>();
        String[] contents = {
            "读书之法，在循序而渐进，熟读而精思。先须熟读，使其言皆若出于吾之口。继以精思，使其意皆若出于吾之心。然后可以有得尔。",
            "凡读书，须要读得字字响亮，不可误一字，不可少一字，不可多一字，不可倒一字，不可牵强暗记，只是要多诵数遍，自然上口，久远不忘。",
            "古人云，读书百遍，其义自见。谓读得熟，则不待解说，自晓其义也。余尝谓，读书有三到，谓心到，眼到，口到。",
            "心不在此，则眼不看仔细，心眼既不专一，却只漫浪诵读，决不能记，记亦不能久也。三到之中，心到最急。心既到矣，眼口岂不到乎？",
            "读书无疑者，须教有疑，有疑者，却要无疑，到这里方是长进。读书有始有终，不可半途而废，须是今日格一件，明日又格一件，积习既久，自然脱然有贯通处。"
        };
        for (int i = 0; i < contents.length; i++) {
            paragraphs.add(new Paragraph("p_" + i, i, contents[i]));
        }
        return new Article("art_001", "读书要略", "朱熹（节选）", paragraphs);
    }

    public Article getArticle() { return article; }

    public List<Note> getAllNotes() {
        return new ArrayList<>(notes);
    }

    public List<Note> getNotesByParagraph(String paragraphId) {
        return notes.stream()
                .filter(n -> paragraphId.equals(n.getParagraphId()))
                .sorted(Comparator.comparingLong(Note::getCreatedAt))
                .collect(Collectors.toList());
    }

    public Note addNote(String paragraphId, String author, String content, Note.NoteType type) {
        if (author == null || author.trim().isEmpty()) return null;
        if (content == null || content.trim().isEmpty()) return null;
        if (paragraphId == null) return null;

        String id = "note_" + UUID.randomUUID().toString().substring(0, 8);
        Note note = new Note(id, paragraphId, author.trim(), content.trim(), type);
        notes.add(note);
        persist();
        return note;
    }

    public synchronized boolean toggleLike(String noteId, String user) {
        if (user == null || user.trim().isEmpty()) return false;
        for (Note note : notes) {
            if (noteId.equals(note.getId())) {
                Set<String> likes = note.getLikes();
                if (likes.contains(user)) {
                    note.removeLike(user);
                } else {
                    note.addLike(user);
                }
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean toggleHighlight(String noteId) {
        for (Note note : notes) {
            if (noteId.equals(note.getId())) {
                note.setHighlighted(!note.isHighlighted());
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean switchParagraph(String paragraphId) {
        if (paragraphId == null) return false;
        for (Paragraph p : article.getParagraphs()) {
            if (paragraphId.equals(p.getId())) {
                article.setCurrentParagraphId(paragraphId);
                persist();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean moveNext() {
        if (article.canMoveNext()) {
            article.moveToNextParagraph();
            persist();
            return true;
        }
        return false;
    }

    public synchronized boolean movePrev() {
        if (article.canMovePrev()) {
            article.moveToPrevParagraph();
            persist();
            return true;
        }
        return false;
    }

    public Map<String, Integer> getNoteCountByParagraph() {
        Map<String, Integer> map = new HashMap<>();
        for (Note n : notes) {
            map.merge(n.getParagraphId(), 1, Integer::sum);
        }
        return map;
    }

    private void persist() {
        try {
            store.save(article, new ArrayList<>(notes));
        } catch (Exception e) {
            System.err.println("[Service] Persist error: " + e.getMessage());
        }
    }
}
