import React, { useEffect, useMemo, useRef, useState } from 'react';
import ArticleView from './features/reader/ArticleView';
import NotePanel from './features/notes/NotePanel';
import ModeratorBar from './features/moderator/ModeratorBar';
import eventBus from './core/EventBus';
import socket from './core/socket';
import type {
  Article, Note, SocketStatus, NoteType, Paragraph,
  LikeUpdatePayload, HighlightUpdatePayload, ParagraphSwitchPayload,
  OnlineCountPayload, ModeratorListPayload
} from './core/types';

type NoteCounts = Record<string, number>;

const STORAGE_KEY = 'reading-board:user';
const MOD_STORAGE_KEY = 'reading-board:want-mod';

const App: React.FC = () => {
  const [article, setArticle] = useState<Article | null>(null);
  const [notes, setNotes] = useState<Note[]>([]);
  const [noteCounts, setNoteCounts] = useState<NoteCounts>({});
  const [socketStatus, setSocketStatus] = useState<SocketStatus>('connecting');
  const [onlineCount, setOnlineCount] = useState(0);
  const [onlineNames, setOnlineNames] = useState<string[]>([]);
  const [userName, setUserNameState] = useState<string>(() => localStorage.getItem(STORAGE_KEY) || '');
  const [isModerator, setIsModerator] = useState<boolean>(() => localStorage.getItem(MOD_STORAGE_KEY) === '1');
  const [moderators, setModerators] = useState<string[]>([]);
  const [highlightParagraphId, setHighlightParagraphId] = useState<string | null>(null);
  const [selectedParagraphId, setSelectedParagraphId] = useState<string | null>(null);
  const [mobileNotesOpen, setMobileNotesOpen] = useState(false);
  const [errorToast, setErrorToast] = useState<string | null>(null);

  const flashTimerRef = useRef<number | null>(null);
  const errorTimerRef = useRef<number | null>(null);

  const clearFlashTimer = () => {
    if (flashTimerRef.current !== null) {
      window.clearTimeout(flashTimerRef.current);
      flashTimerRef.current = null;
    }
  };

  const showError = (reason: string) => {
    setErrorToast(reason);
    if (errorTimerRef.current !== null) window.clearTimeout(errorTimerRef.current);
    errorTimerRef.current = window.setTimeout(() => setErrorToast(null), 3000);
  };

  const setUserName = (name: string) => {
    setUserNameState(name);
    localStorage.setItem(STORAGE_KEY, name);
    socket.setUserName(name);
  };

  const handleSetIsModerator = (want: boolean) => {
    setIsModerator(want);
    localStorage.setItem(MOD_STORAGE_KEY, want ? '1' : '0');
    socket.setModerator(want, want ? 'reading-moderator-2025' : undefined);
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
        if (!selectedParagraphId) setSelectedParagraphId(state.article.currentParagraphId);
      }
      if (state.notes) setNotes(state.notes);
      if (state.noteCounts) setNoteCounts(state.noteCounts);
      if (typeof state.onlineCount === 'number') setOnlineCount(state.onlineCount);
      if (Array.isArray(state.onlineNames)) setOnlineNames(state.onlineNames);
      if (Array.isArray(state.moderators)) setModerators(state.moderators);
      if (typeof state.isModerator === 'boolean') {
        setIsModerator(state.isModerator);
        localStorage.setItem(MOD_STORAGE_KEY, state.isModerator ? '1' : '0');
      }
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
      setSelectedParagraphId(payload.paragraphId);
      clearFlashTimer();
      flashTimerRef.current = window.setTimeout(() => {
        setHighlightParagraphId(null);
        flashTimerRef.current = null;
      }, 3000);
    }));

    unsubs.push(eventBus.on('ONLINE_COUNT', (payload: OnlineCountPayload) => {
      if (typeof payload.onlineCount === 'number') setOnlineCount(payload.onlineCount);
      if (Array.isArray(payload.names)) setOnlineNames(payload.names);
    }));

    unsubs.push(eventBus.on('MODERATOR_LIST', (payload: ModeratorListPayload) => {
      if (Array.isArray(payload.moderators)) setModerators(payload.moderators);
    }));

    unsubs.push(eventBus.on('MODERATOR_GRANTED', () => {
      setIsModerator(true);
      localStorage.setItem(MOD_STORAGE_KEY, '1');
    }));

    unsubs.push(eventBus.on('MODERATOR_DENIED', (reason: string) => {
      setIsModerator(false);
      localStorage.setItem(MOD_STORAGE_KEY, '0');
      showError(`主持人申请被拒绝：${reason}`);
    }));

    unsubs.push(eventBus.on('ERROR', payload => {
      showError(`操作失败：${payload.reason || payload.action}`);
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

    unsubs.push(eventBus.on('REQUEST_SET_MODERATOR', payload => {
      socket.setModerator(payload.moderator, payload.token);
    }));

    unsubs.push(eventBus.on('SELECT_PARAGRAPH_FOR_NOTE', (pid: string) => {
      setSelectedParagraphId(pid);
      setMobileNotesOpen(true);
    }));

    unsubs.push(eventBus.on('OPEN_NOTES_PANEL', () => {
      setMobileNotesOpen(true);
    }));

    return () => {
      unsubs.forEach(u => u());
      clearFlashTimer();
      if (errorTimerRef.current !== null) window.clearTimeout(errorTimerRef.current);
      socket.disconnect();
    };
  }, []);

  useEffect(() => {
    if (isModerator && socketStatus === 'open') {
      socket.setModerator(true, 'reading-moderator-2025');
    }
  }, [isModerator, socketStatus]);

  const currentParagraph = useMemo<Paragraph | null>(() => {
    if (!article) return null;
    return article.paragraphs.find(p => p.id === article.currentParagraphId) || null;
  }, [article]);

  const selectedParagraph = useMemo<Paragraph | null>(() => {
    if (!article) return null;
    const pid = selectedParagraphId || article.currentParagraphId;
    return article.paragraphs.find(p => p.id === pid) || currentParagraph;
  }, [article, selectedParagraphId, currentParagraph]);

  const currentParagraphId = article?.currentParagraphId || null;

  return (
    <div className="app-shell">
      <ModeratorBar
        article={article}
        isModerator={isModerator}
        setIsModerator={handleSetIsModerator}
        moderators={moderators}
        userName={userName}
        setUserName={setUserName}
        socketStatus={socketStatus}
        onlineCount={onlineCount}
        onlineNames={onlineNames}
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
            selectedParagraphId={selectedParagraphId}
            setSelectedParagraphId={setSelectedParagraphId}
            isModerator={isModerator}
            userName={userName}
          />
        </div>

        <div className="app-main__divider" aria-hidden />

        <NotePanel
          article={article}
          currentParagraph={currentParagraph}
          selectedParagraph={selectedParagraph}
          setSelectedParagraphId={setSelectedParagraphId}
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

      {errorToast && (
        <div className="toast toast--error">
          ⚠️ {errorToast}
        </div>
      )}
    </div>
  );
};

export default App;
