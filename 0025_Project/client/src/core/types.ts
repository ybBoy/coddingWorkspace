export type NoteType = 'THOUGHT' | 'QUESTION' | 'SUPPLEMENT';

export type TimelineEventType =
  | 'JOIN' | 'LEAVE' | 'NOTE_ADDED' | 'REPLY_ADDED' | 'LIKE'
  | 'HIGHLIGHT' | 'PARAGRAPH_SWITCH' | 'DISCUSSION_QUEUE_UPDATED'
  | 'TYPING_START' | 'TYPING_END' | 'ARTICLE_UPDATED' | 'USER_RENAMED';

export interface Paragraph {
  id: string;
  index: number;
  content: string;
}

export interface Article {
  id: string;
  title: string;
  author: string;
  paragraphs: Paragraph[];
  currentParagraphId: string;
}

export interface Note {
  id: string;
  paragraphId: string;
  author: string;
  content: string;
  type: NoteType;
  likes: string[];
  highlighted: boolean;
  createdAt: number;
}

export interface Reply {
  id: string;
  noteId: string;
  parentReplyId: string | null;
  author: string;
  content: string;
  likes: string[];
  createdAt: number;
}

export interface Presence {
  userName: string;
  roomId: string;
  paragraphId: string;
  typing: boolean;
  typingSince: number;
  joinedAt: number;
  lastActiveAt: number;
  isOwner: boolean;
  isModerator: boolean;
}

export interface TimelineEvent {
  id: string;
  type: TimelineEventType;
  userName: string;
  data?: Record<string, unknown>;
  timestamp: number;
}

export interface RoomSummary {
  id: string;
  name: string;
  hasPasscode: boolean;
  onlineCount: number;
  articleTitle: string;
  ownerName: string;
  createdAt: number;
}

export interface LikeUpdatePayload {
  noteId: string;
  likes?: {
    count: number;
    users: string[];
  };
  user: string;
}

export interface ReplyLikeUpdatePayload {
  replyId: string;
  likes?: {
    count: number;
    users: string[];
  };
  user: string;
}

export interface HighlightUpdatePayload {
  noteId: string;
  highlighted: boolean;
}

export interface ParagraphSwitchPayload {
  paragraphId: string;
  index: number;
}

export interface OnlineCountPayload {
  onlineCount: number;
  names?: string[];
  presences?: Presence[];
  typingUsers?: string[];
  moderators?: string[];
}

export interface ModeratorListPayload {
  moderators: string[];
}

export interface DiscussionQueuePayload {
  discussionQueue: string[];
}

export interface ExportResultPayload {
  format: 'markdown' | 'json';
  content: string | Record<string, unknown>;
  filename: string;
}

export type SocketStatus = 'connecting' | 'open' | 'closed' | 'error';

export interface WsMessage<T = unknown> {
  type: string;
  payload: T;
  sender?: string;
}

export interface RoomState {
  roomId: string;
  roomName: string;
  ownerName: string;
  article: Article;
  notes: Note[];
  replies: Reply[];
  discussionQueue: string[];
  noteCounts: Record<string, number>;
  presences: Presence[];
  isOwner: boolean;
  isModerator: boolean;
  typingUsers: string[];
  onlineCount: number;
  onlineNames: string[];
  moderators: string[];
}

export interface EventMap {
  ROOM_LIST: RoomSummary[];
  ROOM_CREATED: RoomState;
  ROOM_JOINED: RoomState;
  NOTE_ADDED: Note;
  REPLY_ADDED: Reply;
  LIKE_UPDATED: LikeUpdatePayload;
  REPLY_LIKE_UPDATED: ReplyLikeUpdatePayload;
  HIGHLIGHT_UPDATED: HighlightUpdatePayload;
  PARAGRAPH_SWITCHED: ParagraphSwitchPayload;
  PRESENCE_UPDATED: OnlineCountPayload;
  MODERATOR_LIST: ModeratorListPayload;
  DISCUSSION_QUEUE_UPDATED: DiscussionQueuePayload;
  ARTICLE_UPDATED: Article;
  EXPORT_RESULT: ExportResultPayload;
  TIMELINE_DATA: TimelineEvent[];
  MODERATOR_GRANTED: boolean;
  MODERATOR_DENIED: string;
  ERROR: { action: string; reason: string };
  STATE_SYNC: RoomState;
  ROOM_STATE: { discussionQueue: string[]; moderators: string[] };
  SOCKET_STATUS: SocketStatus;
  USER_NAME_CHANGED: string;
  REQUEST_ADD_NOTE: { paragraphId: string; content: string; type: NoteType };
  REQUEST_ADD_REPLY: { noteId: string; parentReplyId?: string; content: string };
  REQUEST_LIKE: string;
  REQUEST_LIKE_REPLY: string;
  REQUEST_HIGHLIGHT: string;
  REQUEST_SWITCH_PARAGRAPH: string;
  REQUEST_MOVE_NEXT: void;
  REQUEST_MOVE_PREV: void;
  REQUEST_SET_MODERATOR: { moderator: boolean; target?: string };
  REQUEST_JOIN_ROOM: { roomId: string; passcode?: string };
  REQUEST_LEAVE_ROOM: void;
  REQUEST_CREATE_ROOM: { name: string; passcode?: string };
  REQUEST_LIST_ROOMS: void;
  REQUEST_PRESENCE: { paragraphId?: string; typing?: boolean };
  REQUEST_ADD_TO_QUEUE: string;
  REQUEST_REMOVE_FROM_QUEUE: string;
  REQUEST_REORDER_QUEUE: string[];
  REQUEST_IMPORT_ARTICLE: { title?: string; author?: string; text: string };
  REQUEST_EXPORT_MD: void;
  REQUEST_EXPORT_JSON: void;
  REQUEST_TIMELINE: void;
  SELECT_PARAGRAPH_FOR_NOTE: string;
  OPEN_NOTES_PANEL: void;
  SHOW_REPLAY: void;
}
