import React, { useEffect, useMemo, useRef, useState } from 'react';
import ArticleView from './features/reader/ArticleView';
import NotePanel from './features/notes/NotePanel';
import ModeratorBar from './features/moderator/ModeratorBar';
import RoomLobby from './features/room/RoomLobby';
import ReplayView from './features/replay/ReplayView';
import eventBus from './core/EventBus';
import socket from './core/socket';
import type {
  Article, Note, Reply, SocketStatus, NoteType, Paragraph, Presence,
  LikeUpdatePayload, ReplyLikeUpdatePayload, HighlightUpdatePayload,
  ParagraphSwitchPayload, OnlineCountPayload, ModeratorListPayload,
  DiscussionQueuePayload, TimelineEvent, RoomState, RoomSummary,
  ExportResultPayload
} from './core/types';

type NoteCounts = Record<string, number>;
type ViewMode = 'lobby' | 'reading' | 'replay';

const STORAGE_KEY = 'reading-board:user';
const ROOM_STORAGE_KEY = 'reading-board:last-room';

const App: React.FC = () => {
  const [viewMode, setViewMode] = useState<ViewMode>('lobby');
  const [roomId, setRoomId] = useState<string>(() => localStorage.getItem(ROOM_STORAGE_KEY) || '');
  const [roomName, setRoomName] = useState<string>('');
  const [ownerName, setOwnerName] = useState<string>('');
  const [article, setArticle] = useState<Article | null>(null);
  const [notes, setNotes] = useState<Note[]>([]);
  const [replies, setReplies] = useState<Reply[]>([]);
  const [discussionQueue, setDiscussionQueue] = useState<string[]>([]);
  const [noteCounts, setNoteCounts] = useState<NoteCounts>({});
  const [presences, setPresences] = useState<OnlineCountPayload['presences']>([]);
  const [socketStatus, setSocketStatus] = useState<SocketStatus>('connecting');
  const [onlineCount, setOnlineCount] = useState(0);
  const [onlineNames, setOnlineNames] = useState<string[]>([]);
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  const [userName, setUserNameState] = useState<string>(() => localStorage.getItem(STORAGE_KEY) || '');
  const [isOwner, setIsOwner] = useState(false);
  const [isModerator, setIsModerator] = useState(false);
  const [moderators, setModerators] = useState<string[]>([]);
  const [highlightParagraphId, setHighlightParagraphId] = useState<string | null>(null);
  const [selectedParagraphId, setSelectedParagraphId] = useState<string | null>(null);
  const [mobileNotesOpen, setMobileNotesOpen] = useState(false);
  const [errorToast, setErrorToast] = useState<string | null>(null);
  const [roomList, setRoomList] = useState<RoomSummary[]>([]);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [replayIndex, setReplayIndex] = useState(0);

  const flashTimerRef = useRef<number | null>(null);
  const errorTimerRef = useRef<number | null>(null);
  const typingTimerRef = useRef<number | null>(null);

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

  const applyRoomState = (state: RoomState) => {
    setRoomId(state.roomId);
    setRoomName(state.roomName);
    setOwnerName(state.ownerName);
    setArticle(state.article);
    setNotes(state.notes);
    setReplies(state.replies);
    setDiscussionQueue(state.discussionQueue);
    setNoteCounts(state.noteCounts);
    setPresences(state.presences);
    setIsOwner(state.isOwner);
    setIsModerator(state.isModerator);
    setTypingUsers(state.typingUsers || []);
    setOnlineCount(state.onlineCount);
    setOnlineNames(state.onlineNames);
    setModerators(state.moderators);
    setHighlightParagraphId(state.article.currentParagraphId);
    if (!selectedParagraphId) setSelectedParagraphId(state.article.currentParagraphId);
    localStorage.setItem(ROOM_STORAGE_KEY, state.roomId);
    socket.setRoomId(state.roomId);
    setViewMode('reading');
  };

  const downloadBlob = (content: string, filename: string, mimeType: string) => {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  useEffect(() => {
    if (userName) socket.setUserName(userName);
    if (roomId) socket.setRoomId(roomId);
    socket.connect();

    const unsubs: (() => void)[] = [];

    unsubs.push(eventBus.on('SOCKET_STATUS', s => setSocketStatus(s)));

    unsubs.push(eventBus.on('ROOM_LIST', list => {
      setRoomList(list || []);
    }));

    unsubs.push(eventBus.on('ROOM_CREATED', (state: RoomState) => {
      applyRoomState(state);
    }));

    unsubs.push(eventBus.on('ROOM_JOINED', (state: RoomState) => {
      applyRoomState(state);
    }));

    unsubs.push(eventBus.on('STATE_SYNC', (state: RoomState) => {
      if (state && state.roomId) applyRoomState(state);
    }));

    unsubs.push(eventBus.on('ROOM_STATE', state => {
      if (state.discussionQueue) setDiscussionQueue(state.discussionQueue);
      if (state.moderators) setModerators(state.moderators);
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

    unsubs.push(eventBus.on('REPLY_ADDED', (reply: Reply) => {
      setReplies(prev => {
        if (prev.find(r => r.id === reply.id)) return prev;
        return [...prev, reply];
      });
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

    unsubs.push(eventBus.on('REPLY_LIKE_UPDATED', (payload: ReplyLikeUpdatePayload) => {
      setReplies(prev => prev.map(r => {
        if (r.id === payload.replyId) {
          return {
            ...r,
            likes: payload.likes ? Array.from(payload.likes.users) : r.likes
          };
        }
        return r;
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

    unsubs.push(eventBus.on('PRESENCE_UPDATED', (payload: OnlineCountPayload) => {
      if (typeof payload.onlineCount === 'number') setOnlineCount(payload.onlineCount);
      if (Array.isArray(payload.names)) setOnlineNames(payload.names);
      if (Array.isArray(payload.presences)) {
        setPresences(payload.presences);
        const me = payload.presences.find((p: Presence) => p.userName === userName);
        if (me) {
          setIsModerator(me.isModerator || false);
          setIsOwner(me.isOwner || false);
        }
      }
      if (Array.isArray(payload.typingUsers)) setTypingUsers(payload.typingUsers);
      if (Array.isArray(payload.moderators)) setModerators(payload.moderators);
    }));

    unsubs.push(eventBus.on('MODERATOR_LIST', (payload: ModeratorListPayload) => {
      if (Array.isArray(payload.moderators)) setModerators(payload.moderators);
    }));

    unsubs.push(eventBus.on('DISCUSSION_QUEUE_UPDATED', (payload: DiscussionQueuePayload) => {
      setDiscussionQueue(payload.discussionQueue);
    }));

    unsubs.push(eventBus.on('ARTICLE_UPDATED', (a: Article) => {
      setArticle(a);
      setSelectedParagraphId(a.currentParagraphId);
      setHighlightParagraphId(a.currentParagraphId);
    }));

    unsubs.push(eventBus.on('EXPORT_RESULT', (payload: ExportResultPayload) => {
      if (payload.format === 'markdown') {
        downloadBlob(payload.content as string, payload.filename, 'text/markdown;charset=utf-8');
      } else if (payload.format === 'json') {
        downloadBlob(JSON.stringify(payload.content, null, 2), payload.filename, 'application/json;charset=utf-8');
      }
    }));

    unsubs.push(eventBus.on('TIMELINE_DATA', (data: TimelineEvent[]) => {
      setTimeline(data);
      setReplayIndex(0);
      setViewMode('replay');
    }));

    unsubs.push(eventBus.on('MODERATOR_GRANTED', () => {
      setIsModerator(true);
    }));

    unsubs.push(eventBus.on('MODERATOR_DENIED', (reason: string) => {
      setIsModerator(false);
      showError(`主持人申请被拒绝：${reason}`);
    }));

    unsubs.push(eventBus.on('ERROR', payload => {
      showError(`操作失败：${payload.reason || payload.action}`);
    }));

    unsubs.push(eventBus.on('REQUEST_ADD_NOTE', data => {
      const { paragraphId, content, type } = data as { paragraphId: string; content: string; type: NoteType };
      socket.addNote(paragraphId, content, type);
    }));

    unsubs.push(eventBus.on('REQUEST_ADD_REPLY', data => {
      const { noteId, parentReplyId, content } = data as { noteId: string; parentReplyId?: string; content: string };
      socket.addReply(noteId, content, parentReplyId);
    }));

    unsubs.push(eventBus.on('REQUEST_LIKE', (noteId: string) => {
      socket.toggleLike(noteId);
    }));

    unsubs.push(eventBus.on('REQUEST_LIKE_REPLY', (replyId: string) => {
      socket.toggleLikeReply(replyId);
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
      socket.setModerator(payload.moderator, payload.target);
    }));

    unsubs.push(eventBus.on('REQUEST_JOIN_ROOM', payload => {
      socket.joinRoom(payload.roomId, payload.passcode);
    }));

    unsubs.push(eventBus.on('REQUEST_LEAVE_ROOM', () => {
      handleLeaveRoom();
    }));

    unsubs.push(eventBus.on('REQUEST_CREATE_ROOM', payload => {
      socket.createRoom(payload.name, payload.passcode);
    }));

    unsubs.push(eventBus.on('REQUEST_LIST_ROOMS', () => {
      socket.listRooms();
    }));

    unsubs.push(eventBus.on('REQUEST_PRESENCE', payload => {
      socket.updatePresence(payload.paragraphId, payload.typing);
      if (payload.typing) {
        if (typingTimerRef.current) window.clearTimeout(typingTimerRef.current);
        typingTimerRef.current = window.setTimeout(() => {
          socket.updatePresence(undefined, false);
          typingTimerRef.current = null;
        }, 3000);
      }
    }));

    unsubs.push(eventBus.on('REQUEST_ADD_TO_QUEUE', (noteId: string) => {
      socket.addToQueue(noteId);
    }));

    unsubs.push(eventBus.on('REQUEST_REMOVE_FROM_QUEUE', (noteId: string) => {
      socket.removeFromQueue(noteId);
    }));

    unsubs.push(eventBus.on('REQUEST_REORDER_QUEUE', (order: string[]) => {
      socket.reorderQueue(order);
    }));

    unsubs.push(eventBus.on('REQUEST_CLEAR_NOTES_PARAGRAPH', payload => {
      socket.clearNotesByParagraph(payload.paragraphId);
    }));

    unsubs.push(eventBus.on('REQUEST_IMPORT_ARTICLE', payload => {
      socket.importArticle(payload.title, payload.author, payload.text);
    }));

    unsubs.push(eventBus.on('REQUEST_EXPORT_MD', () => {
      socket.exportMarkdown();
    }));

    unsubs.push(eventBus.on('REQUEST_EXPORT_JSON', () => {
      socket.exportJson();
    }));

    unsubs.push(eventBus.on('REQUEST_TIMELINE', () => {
      socket.getTimeline();
    }));

    unsubs.push(eventBus.on('SELECT_PARAGRAPH_FOR_NOTE', (pid: string) => {
      setSelectedParagraphId(pid);
      setMobileNotesOpen(true);
    }));

    unsubs.push(eventBus.on('OPEN_NOTES_PANEL', () => {
      setMobileNotesOpen(true);
    }));

    unsubs.push(eventBus.on('SHOW_REPLAY', () => {
      socket.getTimeline();
    }));

    return () => {
      unsubs.forEach(u => u());
      clearFlashTimer();
      if (errorTimerRef.current !== null) window.clearTimeout(errorTimerRef.current);
      if (typingTimerRef.current !== null) window.clearTimeout(typingTimerRef.current);
      socket.disconnect();
    };
  }, []);

  useEffect(() => {
    if (socketStatus === 'open' && viewMode === 'lobby') {
      socket.listRooms();
    }
  }, [socketStatus, viewMode]);

  useEffect(() => {
    if (socketStatus === 'open' && roomId && viewMode === 'reading') {
      socket.joinRoom(roomId);
    }
  }, [socketStatus, roomId]);

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

  const repliesByNote = useMemo(() => {
    const map: Record<string, Reply[]> = {};
    for (const r of replies) {
      if (!map[r.noteId]) map[r.noteId] = [];
      map[r.noteId].push(r);
    }
    for (const k of Object.keys(map)) {
      map[k].sort((a, b) => a.createdAt - b.createdAt);
    }
    return map;
  }, [replies]);

  const readersByParagraph = useMemo(() => {
    const map: Record<string, string[]> = {};
    for (const p of presences || []) {
      if (!map[p.paragraphId]) map[p.paragraphId] = [];
      if (p.userName !== userName) map[p.paragraphId].push(p.userName);
    }
    return map;
  }, [presences, userName]);

  const handleLeaveRoom = () => {
    socket.leaveRoom();
    setViewMode('lobby');
    setRoomId('');
    localStorage.removeItem(ROOM_STORAGE_KEY);
    setNotes([]);
    setReplies([]);
    setDiscussionQueue([]);
    setPresences([]);
    setTypingUsers([]);
    setTimeline([]);
    setIsOwner(false);
    setIsModerator(false);
    setModerators([]);
    setHighlightParagraphId(null);
    setSelectedParagraphId(null);
  };

  const handleStartReplay = () => {
    eventBus.emit('REQUEST_TIMELINE');
  };

  const handleExitReplay = () => {
    setViewMode('reading');
  };

  if (viewMode === 'lobby') {
    return (
      <div className="app-shell">
        <RoomLobby
          roomList={roomList}
          userName={userName}
          setUserName={setUserName}
          socketStatus={socketStatus}
        />
        {errorToast && (
          <div className="toast toast--error">⚠️ {errorToast}</div>
        )}
      </div>
    );
  }

  if (viewMode === 'replay') {
    return (
      <div className="app-shell">
        <ReplayView
          timeline={timeline}
          article={article}
          currentIndex={replayIndex}
          setCurrentIndex={setReplayIndex}
          onExit={handleExitReplay}
          roomName={roomName}
        />
      </div>
    );
  }

  return (
    <div className="app-shell">
      <ModeratorBar
        article={article}
        isModerator={isModerator}
        isOwner={isOwner}
        setIsModerator={() => {}}
        moderators={moderators}
        userName={userName}
        setUserName={setUserName}
        socketStatus={socketStatus}
        onlineCount={onlineCount}
        onlineNames={onlineNames}
        typingUsers={typingUsers}
        presences={presences}
        roomName={roomName}
        ownerName={ownerName}
        roomId={roomId}
        onOpenNotesMobile={() => setMobileNotesOpen(true)}
        onLeaveRoom={handleLeaveRoom}
        onStartReplay={handleStartReplay}
      />

      <main className="app-main">
        <div className="app-main__reader">
          <ArticleView
            article={article}
            notes={notes}
            repliesByNote={repliesByNote}
            noteCounts={noteCounts}
            currentParagraphId={currentParagraphId}
            highlightParagraphId={highlightParagraphId}
            selectedParagraphId={selectedParagraphId}
            setSelectedParagraphId={setSelectedParagraphId}
            isModerator={isModerator}
            userName={userName}
            readersByParagraph={readersByParagraph}
            discussionQueue={discussionQueue}
          />
        </div>

        <div className="app-main__divider" aria-hidden />

        <NotePanel
          article={article}
          currentParagraph={currentParagraph}
          selectedParagraph={selectedParagraph}
          setSelectedParagraphId={setSelectedParagraphId}
          notes={notes}
          repliesByNote={repliesByNote}
          discussionQueue={discussionQueue}
          userName={userName}
          isModerator={isModerator}
          isOwner={isOwner}
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
