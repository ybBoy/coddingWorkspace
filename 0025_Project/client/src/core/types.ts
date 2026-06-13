export type NoteType = 'THOUGHT' | 'QUESTION' | 'SUPPLEMENT';

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

export interface LikeUpdatePayload {
  noteId: string;
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

export type SocketStatus = 'connecting' | 'open' | 'closed' | 'error';

export interface WsMessage<T = unknown> {
  type: string;
  payload: T;
  sender?: string;
}

export interface EventMap {
  NOTE_ADDED: Note;
  LIKE_UPDATED: LikeUpdatePayload;
  HIGHLIGHT_UPDATED: HighlightUpdatePayload;
  PARAGRAPH_SWITCHED: ParagraphSwitchPayload;
  STATE_SYNC: {
    article: Article;
    notes: Note[];
    noteCounts: Record<string, number>;
    onlineCount: number;
  };
  SOCKET_STATUS: SocketStatus;
  USER_NAME_CHANGED: string;
  REQUEST_ADD_NOTE: {
    paragraphId: string;
    content: string;
    type: NoteType;
  };
  REQUEST_LIKE: string;
  REQUEST_HIGHLIGHT: string;
  REQUEST_SWITCH_PARAGRAPH: string;
  REQUEST_MOVE_NEXT: void;
  REQUEST_MOVE_PREV: void;
}
