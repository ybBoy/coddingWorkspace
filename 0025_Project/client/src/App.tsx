import React, { useEffect, useMemo, useState } from 'react';
import ArticleView from './features/reader/ArticleView';
import NotePanel from './features/notes/NotePanel';
import ModeratorBar from './features/moderator/ModeratorBar';
import eventBus from './core/EventBus';
import socket from './core/socket';
import type {
  Article, Note, SocketStatus, NoteType,
  LikeUpdatePayload, HighlightUpdatePayload, ParagraphSwitchPayload
} from './core/types';

type NoteCounts = Record<string, number>;

const STORAGE_KEY = 'reading-board:user';

const App: React.FC = () => {
  const [article, setArticle] = useState<Article | null>(null);
  const [notes, setNotes] = useState<Note[]>([]);
  const [noteCounts, setNoteCounts] = useState<NoteCounts>({});
  const [socketStatus, setSocketStatus] = useState<SocketStatus>('connecting');
  const [onlineCount, setOnlineCount] = useState(0);
  const [userName, setUserNameState] = useState<string>(() => localStorage.getItem(STORAGE_KEY) || '');
  const [isModerator, setIsModerator] = useState<boolean>(false);
  const [highlightParagraphId, setHighlightParagraphId] = useState<string | null>(null);
  const [flashTimer, setFlashTimer] = useState<number | null>(null);
  const [mobileNotesOpen, setMobileNotesOpen] = useState(false);

  const setUserName = (name: string) => {
    setUserNameState(name);
    localStorage.setItem(STORAGE_KEY, name);
    socket.setUserName(name);
  };

  useEffect(() => {
    if (userName) socket.setUserName(userName);
    socket.connect();

    const unsubs: (() => void)[] = [];

    unsubs.push(eventBus.on('SOCKET_STATUS', s => setSocketStatus(s)));

    unsubs.push(eventBus.on('STATE_SYNC', state => {
      if (state.article) {
        setArticle(state.article);
        setHighlightParagraphId(state.article.currentParagraphId);
      }
      if (state.notes) setNotes(state.notes);
      if (state.noteCounts) setNoteCounts(state.noteCounts);
      if (typeof state.onlineCount === 'number') setOnlineCount(state.onlineCount);
    }));

    unsubs.push(eventBus.on('NOTE_ADDED', (note: Note) => {
      setNotes(prev => {
        if (prev.find(n => n.id === note.id)) return prev;
        return [...prev, note];
      });
      setNoteCounts(prev => ({
        ...prev,
        [note.paragraphId]: (prev[note.paragraphId] || 0) + 1
      }));
    }));

    unsubs.push(eventBus.on('LIKE_UPDATED', (payload: LikeUpdatePayload) => {
      setNotes(prev => prev.map(n => {
        if (n.id === payload.noteId) {
          return {
            ...n,
            likes: payload.likes ? Array.from(payload.likes.users) : n.likes
          };
        }
        return n;
      }));
    }));

    unsubs.push(eventBus.on('HIGHLIGHT_UPDATED', (payload: HighlightUpdatePayload) => {
      setNotes(prev => prev.map(n => {
        if (n.id === payload.noteId) {
          return { ...n, highlighted: payload.highlighted };
        }
        return n;
      }));
    }));

    unsubs.push(eventBus.on('PARAGRAPH_SWITCHED', (payload: ParagraphSwitchPayload) => {
      setArticle(prev => prev ? { ...prev, currentParagraphId: payload.paragraphId } : prev);
      setHighlightParagraphId(payload.paragraphId);
      if (flashTimer) window.clearTimeout(flashTimer);
      const t = window.setTimeout(() => setHighlightParagraphId(null), 3000);
      setFlashTimer(t);
    }));

    unsubs.push(eventBus.on('REQUEST_ADD_NOTE', data => {
      const { paragraphId, content, type } = data as { paragraphId: string; content: string; type: NoteType };
      socket.addNote(paragraphId, content, type);
    }));

    unsubs.push(eventBus.on('REQUEST_LIKE', (noteId: string) => {
      socket.toggleLike(noteId);
    }));

    unsubs.push(eventBus.on('REQUEST_HIGHLIGHT', (noteId: string) => {
      socket.toggleHighlight(noteId);
    }));

    unsubs.push(eventBus.on('REQUEST_SWITCH_PARAGRAPH', (pid: string) => {
      socket.switchParagraph(pid);
    }));

    unsubs.push(eventBus.on('REQUEST_MOVE_NEXT', () => {
      socket.moveNext();
    }));

    unsubs.push(eventBus.on('REQUEST_MOVE_PREV', () => {
      socket.movePrev();
    }));

    return () => {
      unsubs.forEach(u => u());
      socket.disconnect();
      if (flashTimer) window.clearTimeout(flashTimer);
    };
  }, []);

  const currentParagraph = useMemo(() => {
    if (!article) return null;
    return article.paragraphs.find(p => p.id === article.currentParagraphId) || null;
  }, [article]);

  const currentParagraphId = article?.currentParagraphId || null;

  return (
    <div className="app-shell">
      <ModeratorBar
        article={article}
        isModerator={isModerator}
        setIsModerator={setIsModerator}
        userName={userName}
        setUserName={setUserName}
        socketStatus={socketStatus}
        onlineCount={onlineCount}
        onOpenNotesMobile={() => setMobileNotesOpen(true)}
      />

      <main className="app-main">
        <div className="app-main__reader">
          <ArticleView
            article={article}
            notes={notes}
            noteCounts={noteCounts}
            currentParagraphId={currentParagraphId}
            highlightParagraphId={highlightParagraphId}
            isModerator={isModerator}
          />
        </div>

        <div className="app-main__divider" aria-hidden />

        <NotePanel
          currentParagraph={currentParagraph}
          notes={notes}
          userName={userName}
          isModerator={isModerator}
          mobileOpen={mobileNotesOpen}
          onMobileClose={() => setMobileNotesOpen(false)}
        />
      </main>

      {mobileNotesOpen && (
        <div
          className="backdrop"
          onClick={() => setMobileNotesOpen(false)}
          aria-hidden
        />
      )}

      {!userName && (
        <div className="toast toast--hint">
          👋 请在右上角输入昵称，开始参与共读讨论
        </div>
      )}
    </div>
  );
};

export default App;
